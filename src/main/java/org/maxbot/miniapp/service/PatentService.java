package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.client.RospatentClient;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

@Service
public class PatentService {

    private static final Logger log = LoggerFactory.getLogger(PatentService.class);

    private final MaxApiClient maxApiClient;
    private final RospatentClient client;

    public PatentService(MaxApiClient maxApiClient,
                         RospatentClient client) {
        this.maxApiClient = maxApiClient;
        this.client = client;
    }

    public Mono<PatentSearchResponse> searchReactive(String queryMode,
                                                     String query,
                                                     Integer limit,
                                                     Integer offset) {
        return client.searchReactive(queryMode, query, limit, offset);
    }

    // Метод асинхронного извлечения данных конкретного патента без изменения стейта
    public void sendSinglePatentCardAsync(int chatId, String docId) {
        searchReactive("id", docId, 1, 0)
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
}
