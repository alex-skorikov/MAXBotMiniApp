package org.maxbot.miniapp.client;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Component
public class MaxApiClient {


    private final WebClient webClient;

    public MaxApiClient(@Value("${max.api.token}") String token) {
        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public void sendUserMessage(long userId, String text, String userToken) {

        Map<String, Object> payload = Map.of(
                "text", text,
                "attachments", List.of()
        );

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("user_id", userId)
                        .build()
                )
                .header("Authorization", "Bearer " + userToken)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}