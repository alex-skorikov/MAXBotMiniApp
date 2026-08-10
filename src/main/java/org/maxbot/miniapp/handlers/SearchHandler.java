package org.maxbot.miniapp.handlers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient; // Укажите ваш правильный импорт для клиента
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.service.PatentSearchService;
import org.maxbot.miniapp.service.PatentCardService;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SearchHandler implements StepHandler {

    private final PatentSearchService patentSearchService;
    private final MaxApiClient maxApiClient; // Внедряем клиент для прямой отправки сообщений

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        String query = ctx.getSearchQuery();
        int chatId = Integer.parseInt(ctx.getChatId());

        if (query == null || query.isBlank()) {
            sendTextMessage(chatId, "❌ Поисковый запрос пуст. Вернитесь назад.");
            return null;
        }

        try {
            // Синхронно дожидаемся ответа от Роспатента
            var searchResponse = patentSearchService.searchReactive("q", query, 5, 0).block();

            if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().isEmpty()) {
                sendTextMessage(chatId, "🔍 **Результаты поиска по запросу \"" + query + "\":**\n\nНичего не найдено.");
            } else {
                // Формируем клавиатуру для карточек (если нужна)
                List<List<BotResponse.Button>> controlButtons = createControlButtons();

                // Итерируемся по найденным патентам и отправляем их пользователю
                searchResponse.getHits().forEach(hit -> {
                    String patentUrl = "https://rospatent.gov.ru" + hit.getId();
                    String formattedCard = PatentCardService.formatPatentCard(hit);

                    BotResponse response = BotResponse.builder()
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

                    // Отправляем каждую карточку патента в чат
                    maxApiClient.sendMessage(chatId, response).block();
                });

                // В самом конце отправляем меню управления (Назад / Сбросить)
                BotResponse finalMenu = BotResponse.builder()
                        .text("Выше показаны первые 5 результатов. Что делаем дальше?")
                        .attachments(List.of(
                                BotResponse.Attachment.builder()
                                        .type("inline_keyboard")
                                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                                .buttons(controlButtons)
                                                .build())
                                        .build()
                        ))
                        .build();

                maxApiClient.sendMessage(chatId, finalMenu).block();
            }

        } catch (Exception e) {
            log.error("Ошибка выполнения поиска патентов для чата {}", chatId, e);
            sendTextMessage(chatId, "❌ Произошла ошибка при поиске патентов. Попробуйте позже.");
        }

        // Возвращаем null, так как мы уже сами всё отправили через maxApiClient
        return null;
    }

    private void sendTextMessage(int chatId, String text) {
        BotResponse response = BotResponse.builder().text(text).build();
        maxApiClient.sendMessage(chatId, response).block();
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
