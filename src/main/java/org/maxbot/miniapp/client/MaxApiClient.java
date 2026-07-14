package org.maxbot.miniapp.client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class MaxApiClient {

    private final WebClient webClient;

    public MaxApiClient(@Value("${max.api.token}") String token) {

        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<Void> sendMessage(int chatId, Map<String, Object> body) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("chat_id", chatId)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> sendAnswer(String callbackId, Map<String, Object> body) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/answers")
                        .queryParam("callback_id", callbackId)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public void sendMenu(int chatId) {

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
        sendMessage(chatId, body).subscribe();
    }
}