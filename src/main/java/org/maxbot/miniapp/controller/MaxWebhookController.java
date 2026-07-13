package org.maxbot.miniapp.controller;

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
    public Mono<Void> handleUpdate(@RequestBody UpdateDto update) {

        log.info(">>> RAW UPDATE: {}", update);

        // Обрабатываем только message_callback
        if ("message_callback".equals(update.getUpdate_type()) && update.getCallback() != null) {
            return handleCallback(update.getCallback());
        }

        // Обрабатываем только message_created
        if ("message_created".equals(update.getUpdate_type()) && update.getMessage() != null) {
            return handleMessage(update.getMessage());
        }

        // Игнорируем bot_started, bot_stopped
        return Mono.empty();
    }


    // ===========================
    // CALLBACK HANDLER
    // ===========================

    private Mono<Void> handleCallback(CallbackDto cb) {

        if (cb.getCallbackId() == null || cb.getCallbackId().isBlank()) {
            log.warn("Callback received WITHOUT callback_id → ignoring");
            return Mono.empty();
        }

        String callbackId = cb.getCallbackId();
        String payload = cb.getPayload();
        int userId = cb.getUserId();

        switch (payload) {
            case "INFO":
                return answer(callbackId, Map.of(
                        "message", Map.of("text", "Информация о вас:\nUser ID: " + userId)
                ));

            case "PATENT_SEARCH":
                userState.put(userId, "PATENT_SEARCH");
                return answer(callbackId, Map.of(
                        "message", Map.of("text", "Введите поисковый запрос:")
                ));
        }

        return Mono.empty();
    }


    // ===========================
    // MESSAGE HANDLER
    // ===========================

    private Mono<Void> handleMessage(MessageDto msg) {

        int chatId = msg.getRecipient().getChat_id();   // ВАЖНО: используем chat_id
        String text = msg.getBody().getText();

        // Если пользователь вводит текст в режиме поиска
        if ("PATENT_SEARCH".equals(userState.get(chatId))) {
            return handlePatentSearch(chatId, text);
        }

        // На любое другое сообщение → показываем кнопки
        return sendButtons(chatId);
    }

    // ===========================
    // ANSWERS API
    // ===========================

    private Mono<Void> answer(String callbackId, Map<String, Object> body) {
        return webClient.post()
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

    private Mono<Void> sendButtons(int chatId) {

        Map<String, Object> body = Map.of(
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
        );

        return sendMessage(chatId, body);
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
