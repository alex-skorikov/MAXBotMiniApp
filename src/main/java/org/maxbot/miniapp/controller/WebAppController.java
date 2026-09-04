package org.maxbot.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.webapp.SessionInitRequest;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class WebAppController {

    private final PatentService patentService;
    private final ContextRepository contextRepository;
    private static final Logger log = LoggerFactory.getLogger(WebAppController.class);

    public WebAppController(PatentService patentService,
                            ContextRepository contextRepository) {
        this.patentService = patentService;
        this.contextRepository = contextRepository;
    }

    @GetMapping("/session/{id}")
    public Mono<UserContext> getUserContext(@PathVariable("id") int userId) {
        log.info("🌐 [WEB APP] Запрос контекста сессии для ID: {}", userId);

        return Mono.fromCallable(() -> contextRepository.load(String.valueOf(userId)))
                .subscribeOn(Schedulers.boundedElastic())
                // Если контекст в базе не найден, возвращаем пустой объект, чтобы фронт не падал
                .defaultIfEmpty(UserContext.builder()
                        .chatId(String.valueOf(userId))
                        .limit(5)
                        .offset(0)
                        .build());
    }

    @PostMapping("/session/init")
    public Mono<Map<String, String>> sessionInit(@RequestBody SessionInitRequest request) {
        log.info("🌐 [WEB APP] Инициализация сессии. Привязка userId: {} к чату/сессии: {}",
                request.getUserId(), request.getChatId());

        String requestUserId = request.getUserId();
        int userId;
        try {
            userId = Integer.parseInt(requestUserId.trim());
        } catch (NumberFormatException e) {
            log.error("❌ [WEB APP] Ошибка парсинга userId '{}': {}", requestUserId, e.getMessage());
            return Mono.error(new IllegalArgumentException("Invalid format for userId"));
        }

        return Mono.fromRunnable(() -> {
                    UserContext userContext = contextRepository.load(request.getUserId());
                    if (userContext == null) {
                        userContext = new UserContext();
                        userContext.setUserId(userId);
                    }
                    userContext.setChatId(request.getChatId());
                    contextRepository.save(userContext);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .thenReturn(Map.of("status", "SUCCESS", "message", "Сессия успешно инициализирована"));
    }

    @PostMapping("/search")
    public Mono<PatentSearchPagedResponse> search(
            @RequestBody PatentSearchRequest req,
            @RequestParam("userId") int requestUserId) {

        return patentService.searchPatents(req)
                .doOnNext(resp -> {
                    // Асинхронно загружаем, синхронизируем и сохраняем контекст в Redis
                    Mono.fromRunnable(() -> {
                                UserContext ctx = contextRepository.load(String.valueOf(requestUserId));
                                if (ctx == null) {
                                    ctx = new UserContext();
                                    ctx.setUserId(requestUserId);
                                }

                                // --- Синхронизируем контекст пользователя ---
                                contextRepository.syncUserContext(ctx, req, resp);

                                // Сохраняем обновленный контекст обратно в Redis
                                contextRepository.save(ctx);
                            })
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe();
                })
                .map(resp -> PatentService.getPatentSearchPagedResponse(req, resp));
    }

    @GetMapping("/docs/{id}")
    public Mono<ResponseEntity<PatentHit>> getDocumentFromContext(
            @PathVariable("id") String docId,
            @RequestParam("userId") int userId) {

        log.info("🌐 [WEB APP] Запрос карточки документа ID: {} из контекста пользователя: {}", docId, userId);

        return Mono.fromCallable(() -> contextRepository.load(String.valueOf(userId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(ctx -> {
                    if (ctx == null || ctx.getHits() == null || ctx.getHits().isEmpty()) {
                        log.warn("⚠️ [WEB APP] Кэш результатов поиска пуст для пользователя {}", userId);
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<PatentHit>build());
                    }

                    return Flux.fromIterable(ctx.getHits())
                            .filter(hit -> docId.equals(hit.getId()))
                            .next()
                            .map(ResponseEntity::ok)
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).<PatentHit>build());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).<PatentHit>build());
    }

    @GetMapping("/docs/export")
    public Mono<ResponseEntity<Resource>> exportDocument(
            @RequestParam("docId") String docId,
            @RequestParam("userId") int userId) {

        log.info("🌐 [WEB APP] Запрос на экспорт документа ID: {} для пользователя: {}", docId, userId);
        ObjectMapper objectMapper = new ObjectMapper();

        return Mono.fromCallable(() -> contextRepository.load(String.valueOf(userId)))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(ctx -> {
                    if (ctx == null || ctx.getHits() == null || ctx.getHits().isEmpty()) {
                        log.warn("⚠️ [WEB APP] Кэш пуст при попытке экспорта для пользователя {}", userId);
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>build());
                    }

                    // Ищем документ в кэше хитов пользователя
                    return Flux.fromIterable(ctx.getHits())
                            .filter(hit -> docId.equals(hit.getId()))
                            .next()
                            .flatMap(hit -> {
                                try {
                                    // Превращаем объект патента в красивую JSON строку с отступами
                                    String jsonContent = objectMapper.writerWithDefaultPrettyPrinter()
                                            .writeValueAsString(hit);

                                    byte[] fileBytes = jsonContent.getBytes(StandardCharsets.UTF_8);
                                    ByteArrayResource resource = new ByteArrayResource(fileBytes);

                                    // Формируем имя файла (например: patent_UA0000028083C2_20001016.json)
                                    String fileName = "patent_" + docId + ".json";

                                    // Возвращаем файл с заголовками для скачивания в браузере (Шаг 41-42)
                                    return Mono.just(ResponseEntity.ok()
                                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                                            .contentLength(fileBytes.length)
                                            .body((Resource) resource));

                                } catch (Exception e) {
                                    log.error("❌ Ошибка при генерации файла экспорта для документа {}: {}", docId, e.getMessage());
                                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                            .<Resource>build());
                                }
                            })
                            .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).<Resource>build());
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @ExceptionHandler({IllegalArgumentException.class, ServerWebInputException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, String>> handleInvalidInput(Exception ex) {
        log.warn("⚠️ [API ВАЛИДАЦИЯ] Перехвачен некорректный ввод: {}", ex.getMessage());
        return Mono.just(Map.of("status", "ERROR", "message", "Invalid request parameter or format"));
    }

}
