package org.maxbot.miniapp.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.service.PatentSearchService;
import org.maxbot.miniapp.service.PatentCardService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHandler implements StepHandler {

    private final PatentSearchService patentSearchService;
    private final MaxApiClient maxApiClient;

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        String query = ctx.getSearchQuery();
        int chatId = Integer.parseInt(ctx.getChatId());

        if (query == null || query.isBlank()) {
            sendTextMessageAsync(chatId, "❌ Поисковый запрос пуст. Вернитесь назад.");
            return null;
        }

        // Запускаем реактивную цепочку асинхронно, изолируя её в пуле boundedElastic
        patentSearchService.searchReactive("q", query, 5, 0)
                .subscribeOn(Schedulers.boundedElastic()) // ИСПРАВЛЕНО: Уводим выполнение из параллельного потока
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().isEmpty()) {
                        return maxApiClient.sendMessage(chatId, BotResponse.builder()
                                .text("🔍 **Результаты поиска по запросу \"" + query + "\":**\n\nНичего не найдено.")
                                .build());
                    }

                    // Трансформируем список хитов в поток отправки сообщений
                    return Flux.fromIterable(searchResponse.getHits())
                            .flatMap(hit -> {
                                String patentUrl = "https://rospatent.gov.ru" + hit.getId();
                                String formattedCard = PatentCardService.formatPatentCard(hit);

                                BotResponse cardResponse = BotResponse.builder()
                                        .text(formattedCard)
                                        .attachments(List.of(
                                                BotResponse.Attachment.builder()
                                                        .type("inline_keyboard")
                                                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                                                .buttons(List.of(List.of(
                                                                        BotResponse.Button.builder()
                                                                                .type("link")
                                                                                .text("🔗 Открыть патент")
                                                                                .url(patentUrl)
                                                                                .build()
                                                                )))
                                                                .build())
                                                        .build()
                                        ))
                                        .build();

                                return maxApiClient.sendMessage(chatId, cardResponse);
                            })
                            // После отправки всех карточек, отправляем финальное меню управления
                            .then(Mono.defer(() -> {
                                BotResponse finalMenu = BotResponse.builder()
                                        .text("Выше показаны результаты поиска. Что делаем дальше?")
                                        .attachments(List.of(
                                                BotResponse.Attachment.builder()
                                                        .type("inline_keyboard")
                                                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                                                .buttons(createControlButtons())
                                                                .build())
                                                        .build()
                                        ))
                                        .build();
                                return maxApiClient.sendMessage(chatId, finalMenu);
                            }));
                })
                .doOnError(e -> {
                    log.error("Ошибка асинхронного поиска патентов для чата {}", chatId, e);
                    sendTextMessageAsync(chatId, "❌ Произошла ошибка при поиске патентов. Попробуйте позже.");
                })
                .subscribe(); // ИСПРАВЛЕНО: Вместо .block() подписываемся на неблокирующий поток

        // Возвращаем null, так как отправка происходит асинхронно через реактивную подписку выше
        return null;
    }

    private void sendTextMessageAsync(int chatId, String text) {
        maxApiClient.sendMessage(chatId, BotResponse.builder().text(text).build())
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe();
    }

    private List<List<BotResponse.Button>> createControlButtons() {
        List<List<BotResponse.Button>> buttons = new ArrayList<>();
        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("🔙 Назад к вводу запроса")
                .payload("BACK")
                .build()));
        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("🔄 Начать заново")
                .payload("BACK_TO_START")
                .build()));
        return buttons;
    }
}
