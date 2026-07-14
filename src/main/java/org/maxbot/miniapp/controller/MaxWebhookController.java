package org.maxbot.miniapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MaxWebhookController {

    private final Map<Integer, String> userState = new ConcurrentHashMap<>();
    private final WebClient webClient;
    private final PatentSearchService patentSearchService;
    private static final Logger log = LoggerFactory.getLogger(MaxWebhookController.class);

    public MaxWebhookController(@Value("${max.api.token}") String token,
                                PatentSearchService patentSearchService) {

        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();

        this.patentSearchService = patentSearchService;
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
                sendMenu(chatId);
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
                        answer(callbackId, Map.of(
                                "message", Map.of("text", "Информация о вас:\nUser ID: " + userId)
                        ));
                        break;
                    case "PATENT_SEARCH":
                        userState.put(userId, "PATENT_SEARCH");
                        answer(callbackId, Map.of(
                                "message", Map.of("text", "Введите поисковый запрос:")
                        ));
                        break;
                }
            }

        } catch (Exception e) {
            log.error("Error handling update", e);
        }
    }

    // ===========================
    // ANSWERS API
    // ===========================

    private void answer(String callbackId, Map<String, Object> body) {
        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/answers")
                        .queryParam("callback_id", callbackId)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // ===========================
    // MESSAGES API
    // ===========================

    private Mono<Void> sendMessage(int chatId, Map<String, Object> body) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("chat_id", chatId)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    // ===========================
    // BUTTONS
    // ===========================

    private void sendMenu(int chatId) {

        Map<String, Object> body = Map.of(
                "message", Map.of(
                        "text", "Выберите действие:",
                        "attachments", List.of(
                                Map.of(
                                        "type", "inline_keyboard",
                                        "payload", Map.of(
                                                "buttons", List.of(
                                                        List.of(
                                                                Map.of(
                                                                        "type", "callback",
                                                                        "text", "ℹ️ Информация",
                                                                        "payload", "INFO"
                                                                ),
                                                                Map.of(
                                                                        "type", "callback",
                                                                        "text", "🔍 Поиск патентов",
                                                                        "payload", "PATENT_SEARCH"
                                                                )
                                                        )
                                                )
                                        )
                                )
                        )
                )
        );
        sendMessage(chatId, body).subscribe();
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
            return sendMessage(chatId, Map.of("text", "Ничего не найдено."));
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

        return sendMessage(chatId, Map.of("text", sb.toString()));
    }
}
