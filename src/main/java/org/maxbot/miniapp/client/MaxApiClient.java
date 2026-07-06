package org.maxbot.miniapp.client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class MaxApiClient {

    private final WebClient webClient;
    private final String botToken;

    public MaxApiClient(@Value("${max.api.token}") String token) {
        this.botToken = token;

        this.webClient = WebClient.builder()
                .baseUrl("https://api.max.ru")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void sendMessage(String chatId, String text) {

        Map<String, Object> payload = Map.of(
                "chat_id", chatId,
                "text", text
        );

        webClient.post()
                .uri("/bot/" + botToken + "/sendMessage")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}