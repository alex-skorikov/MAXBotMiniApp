package org.maxbot.miniapp.handlers;

import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class SearchHandler implements StepHandler {

    private final String botName;
    private final PatentService patentService;
    private final MaxApiClient maxApiClient;
    private final ContextRepository contextRepository;

    public SearchHandler(@Value("${max.bot.name}") String botName,
                         PatentService patentService,
                         MaxApiClient maxApiClient,
                         ContextRepository contextRepository) {
        this.botName = botName;
        this.patentService = patentService;
        this.maxApiClient = maxApiClient;
        this.contextRepository = contextRepository;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        String query = ctx.getSearchQuery();
        int chatId = Integer.parseInt(ctx.getChatId());

        if (query == null || query.isBlank()) {
            maxApiClient.sendMessage(chatId, BotResponse.builder().notify(false).text("❌ Поисковый запрос пуст.")
                            .build())
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();
            return null;
        }

        int limit = (ctx.getLimit() > 0) ? ctx.getLimit() : 5;
        int offset = ctx.getOffset();

        String date = ctx.getDate();
        List<String> searchArrays = ctx.getDatasetArrays();
        String classifiers = ctx.getClassifiers();

        PatentSearchRequest searchRequest = PatentService.createRequest(
                "qn", query, limit, offset, date, searchArrays, classifiers
        );

        patentService.searchPatents(searchRequest)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(searchResponse -> {
                    if (isResponseEmpty(searchResponse)) {
                        return sendEmptyResultMessage(chatId, query);
                    }
                    // Сохраняем результат поиска для отображения "Подробнее"
                    List<PatentHit> hits = searchResponse.getHits();
                    ctx.setHits(hits);
                    contextRepository.save(ctx);

                    long totalFound = calculateTotalFound(searchResponse);

                    return sendSearchHeader(chatId, query, totalFound)
                            .then(Mono.defer(() -> sendPatentCards(chatId, searchResponse.getHits(), offset)))
                            .then(Mono.defer(() -> sendNavigationMenu(chatId, offset, limit, totalFound)));
                })
                .onErrorResume(e -> {
                    log.error("❌ [SEARCH HANDLER] Ошибка при поиске патентов", e);

                    // Извлекаем чистое сообщение об ошибке
                    String reason = e.getMessage() != null ? e.getMessage() : "Неизвестная ошибка платформы";

                    // Формируем красивую карточку ошибки для пользователя
                    BotResponse errorUi = BotResponse.builder()
                            .notify(false)
                            .text("⚠️ *Ошибка обращения к Роспатенту!*\n\n" +
                                    "Платформа поиска отклонила запрос по причине:\n" +
                                    "`" + reason + "`\n\n")
                            .attachments(List.of(BotResponse.Attachment.builder()
                                    .type("inline_keyboard")
                                    .payload(BotResponse.InlineKeyboardPayload.builder()
                                            .buttons(List.of(List.of(
                                                    BotResponse.Button.builder()
                                                            .type("callback")
                                                            .text("◀️ Вернуться к фильтрам")
                                                            .payload("BACK_TO_START")
                                                            .build()
                                            )))
                                            .build())
                                    .build()))
                            .build();
                    return maxApiClient.sendMessage(chatId, errorUi).then();
                })
                .subscribe();

        return null;
    }

    // --- Проверки и расчеты ---

    private boolean isResponseEmpty(PatentSearchResponse response) {
        return response == null || response.getHits() == null || response.getHits().isEmpty();
    }

    private long calculateTotalFound(PatentSearchResponse response) {
        return response.getTotal() != 0 ? response.getTotal() : response.getHits().size();
    }

    // --- Отправка сообщений и сборка UI ---

    private Mono<Void> sendEmptyResultMessage(int chatId, String query) {
        // Кнопки переходов
        List<BotResponse.Button> navigationRow = new ArrayList<>();
        navigationRow.add(BotResponse.Button.builder()
                .type("callback")
                .text("🔄 Сбросить")
                .payload("BACK_TO_START")
                .build());
        List<List<BotResponse.Button>> buttons = new ArrayList<>();

        buttons.add(navigationRow);

        BotResponse response = BotResponse.builder()
                .notify(false)
                .text("🔍 По запросу \"" + query + "\" ничего не найдено.")
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                .buttons(buttons)
                                .build())
                        .build()
                )).build();
        return maxApiClient.sendMessage(chatId, response).then();
    }

    private Mono<Void> sendSearchHeader(int chatId, String query, long totalFound) {
        String headerText = String.format("🔍 Результаты поиска по запросу: \"%s\"\n📊 Найдено документов: %d",
                query, totalFound);

        BotResponse headerMessage = BotResponse.builder()
                .notify(false)
                .text(headerText)
                .build();
        return maxApiClient.sendMessage(chatId, headerMessage);
    }

    private Mono<Void> sendPatentCards(int chatId, List<PatentHit> hits, int offset) {
        int[] counter = {offset + 1};

        return Flux.fromIterable(hits)
                .concatMap(hit -> {
                    BotResponse singlePatentMessage = buildPatentCard(hit, counter[0]++);
                    return maxApiClient.sendMessage(chatId, singlePatentMessage);
                })
                .then();
    }

    private BotResponse buildPatentCard(PatentHit hit, int currentNumber) {
        String publicationDate = hit.getCommon() != null ? hit.getCommon().getPublicationDate() : "Не указана";
        String docId = hit.getId();
        String title = extractTitle(hit);

        String cardText = String.format("%d.%s\n%s\nДата публикации: %s",
                currentNumber, title, docId, publicationDate);

        List<List<BotResponse.Button>> cardButtons = List.of(List.of(
                BotResponse.Button.builder()
                        .type("callback")
                        .text("Подробнее")
                        .payload("DOC_VIEW_" + docId)
                        .build()
        ));

        return BotResponse.builder()
                .notify(false)
                .text(cardText)
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                .buttons(cardButtons)
                                .build())
                        .build()))
                .build();
    }

    private String extractTitle(PatentHit hit) {
        if (hit.getBiblio() != null && hit.getBiblio().getRu() != null && hit.getBiblio().getRu().getTitle() != null) {
            return hit.getBiblio().getRu().getTitle();
        }
        return "Без названия";
    }

    private Mono<Void> sendNavigationMenu(int chatId, int offset, int limit, long totalFound) {
        List<List<BotResponse.Button>> navButtons = new ArrayList<>();
        List<BotResponse.Button> navigationRow = new ArrayList<>();

        if (offset > 0) {
            navigationRow.add(BotResponse.Button.builder()
                    .type("callback").text("⬅️ Назад").payload("SEARCH_PREV_PAGE").build());
        }
        if (offset + limit < totalFound) {
            navigationRow.add(BotResponse.Button.builder()
                    .type("callback").text("Вперёд ➡️").payload("SEARCH_NEXT_PAGE").build());
        }
        if (!navigationRow.isEmpty()) {
            navButtons.add(navigationRow);
        }

        String dip = "https://max.ru/" + botName + "?startapp";
        navButtons.add(List.of(
                BotResponse.Button.builder()
                        .type("link").text("⚙️ Расширенный поиск").url(dip).build(),
                BotResponse.Button.builder()
                        .type("callback").text("🔄 Сбросить").payload("BACK_TO_START").build()
        ));

        BotResponse navigationMenu = BotResponse.builder()
                .notify(false)
                .text("🎛️ Управление поиском:                             ")
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                .buttons(navButtons)
                                .build())
                        .build()))
                .build();

        return maxApiClient.sendMessage(chatId, navigationMenu);
    }
}
