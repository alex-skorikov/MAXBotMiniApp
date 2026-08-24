package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.client.RosPatentClient;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PatentService {

    private final MaxApiClient maxApiClient;
    private final RosPatentClient client;
    private static final Map<String, List<String>> SEARCH_ARRAYS = Map.ofEntries(
            Map.entry("Россия и страны СНГ", List.of("ru_till_1994", "ru_since_1994", "cis", "dsgn_ru")),
            Map.entry("Минимум РСТ", List.of("ap", "cn", "ch", "au", "gb", "ki", "ca", "at", "jp", "ep", "de", "fr", "ap", "us")),
            Map.entry("Промышленные образцы", List.of("dsgn_kr", "dsgn_cn", "dsgn_jp")),
            Map.entry("Страны с малым ПФ", List.of("others"))
    );

    public PatentService(MaxApiClient maxApiClient,
                         RosPatentClient client) {
        this.maxApiClient = maxApiClient;
        this.client = client;
    }

    public static PatentSearchRequest createRequest(
            String queryMode,
            String query,
            int limit,
            int offset,
            String date,
            List<String> searchArrays,
            String classifiers) {

        // 1. Инициализируем базовый билдер запроса
        var requestBuilder = PatentSearchRequest.builder()
                .queryMode(queryMode)
                .query(query)
                .limit(limit)
                .offset(offset);

        // 2. Добавляем классификаторы (datasets), только если они заданы
        if (searchArrays != null && !searchArrays.isEmpty()) {
            requestBuilder.datasets(searchArrays);
        }

        // 3. Динамически собираем фильтры
        var filterBuilder = PatentSearchRequest.Filter.builder();
        boolean hasFilters = false;

        // Проверяем поисковые массивы (classification)
        if (classifiers != null && !classifiers.isEmpty()) {
            filterBuilder.classification(PatentSearchRequest.Classification.builder()
                    .values(List.of(classifiers))
                    .build());
            hasFilters = true;
        }

        // Проверяем дату публикации
        if (date != null && !date.isBlank()) {
            filterBuilder.datePublished(PatentSearchRequest.DatePublished.builder()
                    .range(PatentSearchRequest.Range.builder()
                            .gt(date)
                            .build())
                    .build());
            hasFilters = true;
        }

        // Добавляем блок фильтров в запрос, только если заполнился хотя бы один критерий
        if (hasFilters) {
            requestBuilder.filter(filterBuilder.build());
        }

        return requestBuilder.build();
    }

    public Mono<PatentSearchResponse> searchPatents(PatentSearchRequest request) {
        return client.searchReactive(request);
    }

    // Метод извлечения данных конкретного патента без изменения стейта
    public Mono<Void> sendSinglePatentCardAsync(int chatId, String docId, UserContext userContext) {

        List<PatentHit> hits = userContext.getHits();
        Optional<PatentHit> hit = hits.stream().filter(h -> h.getId().equals(docId)).findFirst();
        if (hit.isPresent()) {
            // Экранируем ID для сборки полностью валидного адреса
            String encodedId = java.net.URLEncoder.encode(hit.get().getId(), java.nio.charset.StandardCharsets.UTF_8);
            String patentUrl = "https://searchplatform.rospatent.gov.ru/doc/" + encodedId;

            List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();

            buttons.add(List.of(BotResponse.Button.builder()
                    .type("link")
                    .text("🔗 Ссылка на оригинал патента")
                    .url(patentUrl)
                    .build()));

            BotResponse cardResponse = BotResponse.builder()
                    .notify(false)
                    .text(PatentCardService.formatPatentCard(hit.get()))
                    .attachments(List.of(BotResponse.Attachment.builder()
                            .type("inline_keyboard")
                            .payload(BotResponse.InlineKeyboardPayload.builder()
                                    .buttons(buttons)
                                    .build())
                            .build()
                    ))
                    .build(); // Отправляем без блока инлайн-кнопок

            return maxApiClient.sendMessage(chatId, cardResponse);
        } else {
            BotResponse errorResponse = BotResponse.builder()
                    .notify(false)
                    .text("❌ Не удалось загрузить информацию по документу " + docId)
                    .build();

            return maxApiClient.sendMessage(chatId, errorResponse);
        }
    }

    public List<String> getSearchArrayByDescription(String searchArrayName) {
        return SEARCH_ARRAYS.get(searchArrayName);
    }

    public static PatentSearchPagedResponse getPatentSearchPagedResponse(PatentSearchRequest request,
                                                                         PatentSearchResponse raw) {
        PatentSearchPagedResponse response = new PatentSearchPagedResponse();
        response.setItems(raw.getHits());

        PatentSearchPagedResponse.Pagination pagination =
                new PatentSearchPagedResponse.Pagination();

        int pageSize = request.getLimit();
        int page = (request.getOffset() / pageSize) + 1;

        pagination.setPage(page);
        pagination.setPageSize(pageSize);
        pagination.setTotal(raw.getTotal());
        pagination.setHasNext(request.getOffset() + pageSize < raw.getTotal());

        response.setPagination(pagination);
        return response;
    }
}
