package org.maxbot.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.service.PatentCardService;
import org.maxbot.miniapp.service.PatentSearchService;
import org.maxbot.miniapp.stepalgo.AbstractAlgo;
import org.maxbot.miniapp.stepalgo.AlgoStatus;
import org.maxbot.miniapp.stepalgo.BotStepsAlgo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MaxWebhookControllerV2 {

    private final Map<Integer, String> userState = new ConcurrentHashMap<>();
    private final PatentSearchService patentSearchService;
    private static final Logger log = LoggerFactory.getLogger(MaxWebhookControllerV2.class);
    private final MaxApiClient maxApiClient;
    private final AbstractAlgo algo;

    public MaxWebhookControllerV2(@Value("${max.token}") String token,
                                  PatentSearchService patentSearchService,
                                  MaxApiClient maxApiClient) {
        this.patentSearchService = patentSearchService;
        this.maxApiClient = maxApiClient;
        this.algo = new BotStepsAlgo(maxApiClient,"BotSearchAlgorithm");
    }

    @PostMapping("/webhook")
    public Mono<Void> webhook(@RequestBody String update) {
        try {
            log.info(">>> RAW UPDATE: {}", update);

            ObjectMapper mapper = new ObjectMapper();
            UpdateDto upd = mapper.readValue(update, UpdateDto.class);

            algo.run(AlgoStatus.STEP_0, upd);


//            if ("bot_started".equals(upd.getUpdateType()) ||
//                    "message_created".equals(upd.getUpdateType())) {
//                return maxApiClient.sendStartMenu(upd.getChatId())
//                        .onErrorResume(e -> Mono.empty());
//            }

//            CallbackDto cb = upd.getCallback();
//            int userId = cb.getUser().getUserId();
//            int chatId = upd.getMessage().getRecipient().getChatId();
//            String payload = cb.getPayload();

//            if ("message_callback".equals(upd.getUpdateType())) {
//                return algo.run(AlgoStatus.STEP_0, upd);
//            }


//            if ("message_created".equals(upd.getUpdateType())) {
//                MessageDto msg = upd.getMessage();
//                String text = msg.getBody().getText();
//
//                if ("PATENT_SEARCH".equals(userState.get(userId))) {
//                    return handlePatentSearch("qn", text, userId, chatId)
//                            .onErrorResume(e -> Mono.empty());
//                }
//
//                return maxApiClient.sendStartMenu(chatId)
//                        .onErrorResume(e -> Mono.empty());
//            }

//            if ("message_callback".equals(upd.getUpdateType())) {
//                switch (payload) {
//                    case "PATENTS":
//                        String info = UserService.getUserInfo(cb, upd);
//                        BotAnswerMessage responseInfo = BotAnswerMessage.builder()
//                                .text(info)
//                                .build();
//                        return maxApiClient.sendMessage(chatId, responseInfo)
//                                .onErrorResume(e -> Mono.empty());
//
//                    case "PATENT_SEARCH":
//                        userState.put(userId, "PATENT_SEARCH");
//                        BotAnswerMessage searchRq = BotAnswerMessage.builder()
//                                .text("Введите поисковый запрос:")
//                                .build();
//                        return maxApiClient.sendMessage(chatId, searchRq)
//                                .onErrorResume(e -> Mono.empty());
//                }
//            }

            return Mono.empty();
        } catch (Exception e) {
            log.error("Error handling update", e);
            return Mono.empty();
        }
    }

    // ===========================
    // PATENT SEARCH
    // ===========================

    private Mono<Void> handlePatentSearch(String queryMode, String query, int userId, int chatId) {

        return patentSearchService.searchReactive(queryMode, query, 5, 0)
                .flatMap(raw -> {

                    if (raw.getHits().isEmpty()) {
                        userState.remove(userId);
                        BotAnswerMessage message = BotAnswerMessage.builder()
                                .text("Ничего не найдено.")
                                .build();
                        return maxApiClient.sendMessage(chatId, message);
                    }

                    List<Mono<Void>> messages = raw.getHits().stream()
                            .map(hit -> {
                                String patentUrl = "https://searchplatform.rospatent.gov.ru/doc/" + hit.getId();
                                BotAnswerMessage response = BotAnswerMessage.builder()
                                        .text(PatentCardService.formatPatentCard(hit))
                                        .attachments(List.of(
                                                BotAnswerMessage.Attachment.builder()
                                                        .type("inline_keyboard")
                                                        .payload(BotAnswerMessage.InlineKeyboardPayload.builder()
                                                                .buttons(List.of(List.of(
                                                                        BotAnswerMessage.Button.builder()
                                                                                .type("link")
                                                                                .text("Ссылка")
                                                                                .url(patentUrl)
                                                                                .build()
                                                                )))
                                                                .build())
                                                        .build()
                                        ))
                                        .build();

                                return maxApiClient.sendMessage(chatId, response);
                            })
                            .toList();

                    userState.remove(userId);

                    return Mono.when(messages);
                });
    }
}
