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

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

        patentSearchService.searchReactive("q", query, 5, 0)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().isEmpty()) {
                        return maxApiClient.sendMessage(chatId, BotResponse.builder()
                                .text("🔍 **Результаты поиска по запросу \"" + query + "\":**\n\nНичего не найдено.")
                                .build());
                    }

                    // Асинхронно отправляем каждую карточку патента
                    return Flux.fromIterable(searchResponse.getHits())
                            .flatMap(hit -> {
                                // 1. Безопасно экранируем ID документа для формирования валидного URL
                                String encodedId = URLEncoder.encode(hit.getId(), StandardCharsets.UTF_8);
                                String patentUrl = "https://rospatent.gov.ru" + encodedId;

                                // 2. Вшиваем ссылку прямо в текст карточки с Markdown-разметкой
                                String formattedCard = PatentCardService.formatPatentCard(hit) +
                                        "\n\n🔗 **[Открыть полную карточку патента](" + patentUrl + ")**\n" +
                                        "— — — — — — — — — — — — — — —";

                                BotResponse cardResponse = BotResponse.builder()
                                        .text(formattedCard)
                                        .format("markdown") // Указываем платформе, что используем Markdown-разметку
                                        .build();

                                return maxApiClient.sendMessage(chatId, cardResponse);
                            })
                            // 3. Строго ПОСЛЕ отправки всех карточек выводим навигационное меню
                            .then(Mono.defer(() -> {
                                BotResponse finalMenu = BotResponse.builder()
                                        .text("Выше показаны результаты поиска (Топ-5). Что делаем дальше?")
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
                    log.error("Критическая ошибка асинхронного поиска патентов для чата {}", chatId, e);
                    sendTextMessageAsync(chatId, "❌ Произошла ошибка при поиске патентов. Попробуйте позже.");
                })
                .subscribe();

        return null; // Уведомляем диспетчер стейт-машины, что синхронный ответ слать не нужно
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
