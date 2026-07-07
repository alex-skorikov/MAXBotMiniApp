package org.maxbot.miniapp.client;


import io.netty.handler.logging.LogLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

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

    public void sendMessage(long chatId, String text) {

        Map<String, Object> payload = Map.of(
                "text", text,
                "attachments", List.of()
        );

        System.out.println(">>> Send message: " + payload);

        webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("user_id", chatId)
                        .build()
                )
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}