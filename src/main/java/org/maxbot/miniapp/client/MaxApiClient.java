package org.maxbot.miniapp.client;


import org.maxbot.miniapp.dto.bot.BotResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MaxApiClient {

    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(MaxApiClient.class);

    public MaxApiClient(@Value("${max.token}") String token,
                        @Value("${max.api.url}") String maxUrl,
                        WebClient webClient) {
        this.webClient = webClient.mutate()
                .baseUrl(maxUrl)
                .defaultHeader("Authorization", token)
                .build();
    }

    public Mono<Void> sendMessage(int chatId, BotResponse resp) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/messages")
                        .queryParam("chat_id", chatId).build())
                .bodyValue(resp).retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("❌ MAX API sendMessage error: {} ", e.getMessage()));
    }

    public Mono<Void> sendAnswer(String callbackId, BotResponse resp) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/answers")
                        .queryParam("callback_id", callbackId).build())
                .bodyValue(Map.of("message", resp))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("❌ MAX API sendAnswer error: {} ", e.getMessage()));
    }
}