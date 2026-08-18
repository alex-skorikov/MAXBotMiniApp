package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.client.RospatentClient;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Service
public class PatentService {

    private static final Logger log = LoggerFactory.getLogger(PatentService.class);

    private final MaxApiClient maxApiClient;
    private final RospatentClient client;
    private static final Map<String, List<String>> SEARCH_ARRAYS = Map.ofEntries(
            Map.entry("Россия и страны СНГ", List.of("ru_till_1994", "ru_since_1994", "cis", "dsgn_ru")),
            Map.entry("Минимум РСТ", List.of("ap", "cn", "ch", "au", "gb", "ki", "ca", "at", "jp", "ep", "de", "fr", "ap", "us")),
            Map.entry("Промышленные образцы", List.of("dsgn_kr", "dsgn_cn", "dsgn_jp")),
            Map.entry("Страны с малым ПФ", List.of("others"))
    );


    public PatentService(MaxApiClient maxApiClient,
                         RospatentClient client) {
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
        if (classifiers != null && !classifiers.isBlank()) {
            requestBuilder.datasets(List.of(classifiers));
        }

        // 3. Динамически собираем фильтры
        var filterBuilder = PatentSearchRequest.Filter.builder();
        boolean hasFilters = false;

        // Проверяем поисковые массивы (classification)
        if (searchArrays != null && !searchArrays.isEmpty()) {
            filterBuilder.classification(PatentSearchRequest.Classification.builder()
                    .values(searchArrays)
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

    // Метод асинхронного извлечения данных конкретного патента без изменения стейта
    public void sendSinglePatentCardAsync(int chatId, String docId) {

        PatentSearchRequest request = PatentSearchRequest.builder()
                .queryMode("id")
                .query(docId)
                .limit(1)
                .offset(0)
                .build();

        searchPatents(request)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits()
                            .isEmpty()) {
                        return maxApiClient.sendMessage(chatId, BotResponse.builder()
                                .notify(false)
                                .text("❌ Не удалось загрузить информацию по документу " + docId)
                                .build());
                    }

                    var hit = searchResponse.getHits().get(0);

                    // Экранируем ID для сборки полностью валидного адреса
                    String encodedId = java.net.URLEncoder.encode(hit.getId(), java.nio.charset.StandardCharsets.UTF_8);
                    String patentUrl = "https://searchplatform.rospatent.gov.ru/doc/" + encodedId;

                    List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();

                    buttons.add(List.of(BotResponse.Button.builder()
                            .type("link")
                            .text("🔗 Ссылка на оригинал патента")
                            .url(patentUrl)
                            .build()));

                    BotResponse cardResponse = BotResponse.builder()
                            .notify(false)
                            .text(PatentCardService.formatPatentCard(hit))
                            .attachments(List.of(BotResponse.Attachment.builder()
                                    .type("inline_keyboard")
                                    .payload(BotResponse.InlineKeyboardPayload.builder()
                                            .buttons(buttons)
                                            .build())
                                    .build()
                            ))
                            .build(); // Отправляем без блока инлайн-кнопок

                    return maxApiClient.sendMessage(chatId, cardResponse);
                })
                .doOnError(err -> log.error("Критическая ошибка при загрузке документа {}", docId, err))
                .subscribe();
    }

    public List<String> getSearchArrayByDescription(String searchArrayName) {
        return SEARCH_ARRAYS.get(searchArrayName);
    }
}
