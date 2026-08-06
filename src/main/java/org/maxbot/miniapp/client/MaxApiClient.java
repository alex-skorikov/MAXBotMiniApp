package org.maxbot.miniapp.client;


import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class MaxApiClient {

    private final String token;
    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(MaxApiClient.class);

    public MaxApiClient(@Value("${max.token}") String token, WebClient webClient) {
        this.token = token;
        this.webClient = webClient.mutate()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .build();
    }

    public Mono<Void> sendMessage(int chatId, BotAnswerMessage bodyValue) {
        log.info(">>> Send Message: {}", bodyValue);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/messages")
                        .queryParam("chat_id", chatId).build())
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("MAX API sendMessage error", e));
    }

    public Mono<Void> sendMessage2(int chatId, BotResponse resp) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/messages")
                        .queryParam("chat_id", chatId).build())
                .bodyValue(resp).retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("MAX API sendMessage error", e));
    }

    public Mono<Void> sendAnswer(String callbackId, BotResponse resp) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/answers")
                        .queryParam("callback_id", callbackId).build())
                .bodyValue(Map.of("message", resp))
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("MAX API sendAnswer error", e));
    }
}