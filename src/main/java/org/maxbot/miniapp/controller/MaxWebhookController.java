package org.maxbot.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.SenderDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
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
    public void handleUpdate(@RequestBody String updates) {
        try {
            log.info(">>> RAW UPDATE: {}", updates);

            ObjectMapper mapper = new ObjectMapper();
            UpdateDto update = mapper.readValue(updates, UpdateDto.class);

            // --- MESSAGE CREATED ---
            if ("message_created".equals(update.getUpdateType())) {
                MessageDto msg = update.getMessage();
                int chatId = msg.getRecipient().getChatId();
                int userId = msg.getSender().getUserId();
                String text = msg.getBody().getText();

                // Если пользователь в режиме поиска патентов
                if ("PATENT_SEARCH".equals(userState.get(userId))) {
                    handlePatentSearch(chatId, text).subscribe();
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
                long chatId = update.getMessage().getRecipient().getChatId();

                switch (payload) {
                    case "INFO":
                        String info = UserService.getUserInfo(cb, update);
                        maxApiClient.answer(callbackId, Map.of(
                                "message", Map.of("text", info)
                        )).subscribe();
                        break;
                    case "PATENT_SEARCH":
                        userState.put(userId, "PATENT_SEARCH");
                        maxApiClient.answer(callbackId, Map.of(
                                "message", Map.of("text", "Введите поисковый запрос:")
                        )).subscribe();
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

    private Mono<Void> handlePatentSearch(int chatId, String query) {

        PatentSearchResponse raw = patentSearchService.search(
                query,
                "q",
                10,
                0
        );

        if (raw.getHits() == null || raw.getHits().isEmpty()) {
            userState.remove(chatId);
            return maxApiClient.sendMessage(chatId, Map.of("text", "Ничего не найдено."));
        }

        StringBuilder sb = new StringBuilder("Результаты поиска:\n\n");

        raw.getHits().forEach(item -> {
            sb.append("📄 ").append(item.getTitle()).append("\n");
            if (item.getInventor() != null)
                sb.append("👤 ").append(item.getInventor()).append("\n");
            if (item.getIpc() != null)
                sb.append("🔧 IPC: ").append(item.getIpc()).append("\n");
            sb.append("\n");
        });

        userState.remove(chatId);

        return maxApiClient.sendMessage(chatId, Map.of("text", sb.toString()));
    }
}
