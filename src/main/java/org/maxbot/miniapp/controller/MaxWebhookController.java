package org.maxbot.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentCardService;
import org.maxbot.miniapp.service.PatentSearchService;
import org.maxbot.miniapp.service.UserService;
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
public class MaxWebhookController {

    private final Map<Integer, String> userState = new ConcurrentHashMap<>();
    private final PatentSearchService patentSearchService;
    private static final Logger log = LoggerFactory.getLogger(MaxWebhookController.class);
    private final MaxApiClient maxApiClient;

    public MaxWebhookController(@Value("${max.api.token}") String token,
                                PatentSearchService patentSearchService,
                                MaxApiClient maxApiClient) {
        this.patentSearchService = patentSearchService;
        this.maxApiClient = maxApiClient;
    }

    @PostMapping("/webhook")
    public Mono<Void> handleUpdate(@RequestBody String updates) {
        try {
            log.info(">>> RAW UPDATE: {}", updates);

            ObjectMapper mapper = new ObjectMapper();
            UpdateDto update = mapper.readValue(updates, UpdateDto.class);


            if ("bot_started".equals(update.getUpdateType())) {
                return maxApiClient.sendMenu(update.getUserId())
                        .onErrorResume(e -> Mono.empty());
            }

            if ("message_created".equals(update.getUpdateType())) {
                MessageDto msg = update.getMessage();
                int chatId = msg.getRecipient().getChatId();
                int userId = msg.getSender().getUserId();
                String text = msg.getBody().getText();

                if ("PATENT_SEARCH".equals(userState.get(userId))) {
                    return handlePatentSearch("qn", text, userId, chatId)
                            .onErrorResume(e -> Mono.empty());
                }

                return maxApiClient.sendMenu(userId)
                        .onErrorResume(e -> Mono.empty());
            }

            if ("message_callback".equals(update.getUpdateType())) {
                CallbackDto cb = update.getCallback();
                int userId = cb.getUser().getUserId();
                int chatId = update.getMessage().getRecipient().getChatId();
                String payload = cb.getPayload();

                switch (payload) {
                    case "INFO":
                        String info = UserService.getUserInfo(cb, update);
                        BotAnswerMessage responseInfo = BotAnswerMessage.builder()
                                .text(info)
                                .build();
                        return maxApiClient.sendMessage(userId, responseInfo)
                                .onErrorResume(e -> Mono.empty());

                    case "PATENT_SEARCH":
                        userState.put(userId, "PATENT_SEARCH");
                        BotAnswerMessage searchRq = BotAnswerMessage.builder()
                                .text("Введите поисковый запрос:")
                                .build();
                        return maxApiClient.sendMessage(userId, searchRq)
                                .onErrorResume(e -> Mono.empty());
                }
            }

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
