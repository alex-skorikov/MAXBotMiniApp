package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.maxbot.miniapp.service.PatentCardService;
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
@Component
public class SearchHandler implements StepHandler {

    private final PatentSearchService patentSearchService;

    public SearchHandler(PatentSearchService patentSearchService) {
        this.patentSearchService = patentSearchService;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад к вводу запроса поиска")
                .payload("BACK")
                .build()));

        return BotResponse.builder()
                .text("Здесь будут результаты поиска:")
                .build();
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
}
