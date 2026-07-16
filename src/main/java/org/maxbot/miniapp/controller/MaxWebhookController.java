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
    public void handleUpdate(@RequestBody String updates) {
        try {
            log.info(">>> RAW UPDATE: {}", updates);

            ObjectMapper mapper = new ObjectMapper();
            UpdateDto update = mapper.readValue(updates, UpdateDto.class);

            // --- Старт бота ---
            if ("bot_started".equals(update.getUpdateType())) {
                maxApiClient.sendMenu(update.getChatId());
                return;
            }

            // --- На любое сообщение отправляем пока меню ---
            if ("message_created".equals(update.getUpdateType())) {
                MessageDto msg = update.getMessage();
                int chatId = msg.getRecipient().getChatId();
                int userId = msg.getSender().getUserId();
                String text = msg.getBody().getText();

                // Если пользователь в режиме поиска патентов
                if ("PATENT_SEARCH".equals(userState.get(userId))) {
                    handlePatentSearch(userId, chatId, text);
                    return;
                }

                // Иначе показываем меню
                maxApiClient.sendMenu(chatId);
                return;
            }

            // --- CALLBACK ---
            if ("message_callback".equals(update.getUpdateType())) {
                CallbackDto cb = update.getCallback();
                int userId = cb.getUser().getUserId();
                String callbackId = cb.getCallbackId();

                if (cb.getCallbackId() == null) {
                    log.warn("Callback received WITHOUT callback_id → ignoring");
                    return;
                }

                String payload = cb.getPayload();

                int chatId = update.getMessage().getRecipient().getChatId();
                switch (payload) {
                    case "INFO":
                        String info = UserService.getUserInfo(cb, update);
                        maxApiClient.sendMessage(chatId, Map.of("text", info));
                        break;
                    case "PATENT_SEARCH":
                        userState.put(userId, "PATENT_SEARCH");
                        maxApiClient.sendMessage(chatId, Map.of("text", "Введите поисковый запрос:"));
                        break;
                }
            }

        } catch (Exception e) {
            log.error("Error handling update", e);
        }
    }

    // ===========================
    // PATENT SEARCH
    // ===========================

    private void handlePatentSearch(int userId, int chatId, String query) {

        PatentSearchResponse raw = patentSearchService.search(query,
                "q", 5, 0);

        if (raw.getHits().isEmpty()) {
            userState.remove(userId);
            maxApiClient.sendMessage(chatId, Map.of("text", "Ничего не найдено."));
        } else {
            raw.getHits().forEach(hit -> {
                String patentUrl = "https://searchplatform.rospatent.gov.ru/doc/" + hit.getId();
                BotAnswerMessage response = BotAnswerMessage.builder()
                        .text(PatentCardService.formatPatentCard(hit))
                        .attachments(List.of(BotAnswerMessage.Attachment.builder()
                                .type("inline_keyboard")
                                .payload(BotAnswerMessage.InlineKeyboardPayload.builder()
                                        .buttons(List.of(List.of(BotAnswerMessage.Button.builder()
                                                .type("link")
                                                .text("Ссылка")
                                                .url(patentUrl)
                                                .build())))
                                        .build())
                                .build()
                        ))
                        .build();

                maxApiClient.sendMessage(chatId, response);
            });
            userState.remove(userId);
        }
    }
}
