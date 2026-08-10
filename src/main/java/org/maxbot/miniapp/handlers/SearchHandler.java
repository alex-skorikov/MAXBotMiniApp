package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.service.PatentSearchService; // Предположим, сервис лежит тут
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SearchHandler implements StepHandler {

    private final PatentSearchService patentSearchService;

    public SearchHandler(PatentSearchService patentSearchService) {
        this.patentSearchService = patentSearchService;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        String base = ctx.getSelectedBase();      // "Патенты", "Промобразцы" и др.
        String date = ctx.getDate();              // "2021-02-03"
        String array = ctx.getSearchArrays();     // "Россия и страны СНГ"
        String classifier = ctx.getClassifiers(); // "HYU"

        String query = event.getText();

        String searchResult;
        try {
            searchResult = String.valueOf(patentSearchService.searchReactive("q", query, 5, 1));
        } catch (Exception e) {
            searchResult = "❌ Произошла ошибка при поиске патентов. Попробуйте позже.";
        }

        // 4. Формируем клавиатуру инлайн-кнопок
        List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();

        // Кнопка возврата к вводу текста
        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("🔙 Назад к вводу запроса")
                .payload("BACK")
                .build()));

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("🔄 Сбросить фильтры и начать заново")
                .payload("BACK_TO_START") // Не забудьте прописать этот payload в MaxMapper -> BotEvents.BACK
                .build()));

        return BotResponse.builder()
                .text("🔍 **Результаты поиска по запросу \"" + query + "\":**\n\n" + searchResult)
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                .buttons(buttons)
                                .build())
                        .build()
                ))
                .build();
    }
}


// ===========================
    // PATENT SEARCH
    // ===========================

//    private Mono<Void> handlePatentSearch(String queryMode, String query, int userId, int chatId) {
//
//        return patentSearchService.searchReactive(queryMode, query, 5, 0)
//                .flatMap(raw -> {
//
//                    if (raw.getHits().isEmpty()) {
//                        BotAnswerMessage message = BotAnswerMessage.builder()
//                                .text("Ничего не найдено.")
//                                .build();
//                        return maxApiClient.sendMessage(chatId, message);
//                    }
//
//                    List<Mono<Void>> messages = raw.getHits().stream()
//                            .map(hit -> {
//                                String patentUrl = "https://searchplatform.rospatent.gov.ru/doc/" + hit.getId();
//                                BotAnswerMessage response = BotAnswerMessage.builder()
//                                        .text(PatentCardService.formatPatentCard(hit))
//                                        .attachments(List.of(
//                                                BotAnswerMessage.Attachment.builder()
//                                                        .type("inline_keyboard")
//                                                        .payload(BotAnswerMessage.InlineKeyboardPayload.builder()
//                                                                .buttons(List.of(List.of(
//                                                                        BotAnswerMessage.Button.builder()
//                                                                                .type("link")
//                                                                                .text("Ссылка")
//                                                                                .url(patentUrl)
//                                                                                .build()
//                                                                )))
//                                                                .build())
//                                                        .build()
//                                        ))
//                                        .build();
//
//                                return maxApiClient.sendMessage(chatId, response);
//                            })
//                            .toList();
//                    return Mono.when(messages);
//                });
//    }
//}
