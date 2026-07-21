package org.maxbot.miniapp.client;


import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Component
public class MaxApiClient {

    private final String token;
    private final WebClient webClient;
    private static final Logger log = LoggerFactory.getLogger(MaxApiClient.class);

    public MaxApiClient(@Value("${max.token}") String token, WebClient webClient) {
        this.token = token;
        log.info("MAX_TOKEN = '{}'", token);
        this.webClient = webClient.mutate()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
//                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
//                        "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .build();
    }

    public Mono<Void> sendMessage(int chatId, BotAnswerMessage bodyValue) {
        log.info(">>> Send Message: {}", bodyValue);
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("chat_id", chatId)
                        .build())
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("MAX API sendMessage error", e));
    }

    public Mono<Void> sendAnswer(String callbackId, Map<String, Object> bodyValue) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/answers")
                        .queryParam("callback_id", callbackId)
                        .build())
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Void.class)
                .doOnError(e -> log.error("MAX API sendAnswer error", e));
    }

    public Mono<Void> sendMenu(int chatId) {
        BotAnswerMessage response = BotAnswerMessage.builder()
                .text("Выберите действие:")
                .attachments(List.of(BotAnswerMessage.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotAnswerMessage.InlineKeyboardPayload.builder()
                                .buttons(List.of(List.of(
                                                BotAnswerMessage.Button.builder()
                                                        .type("callback")
                                                        .text("ℹ️ Информация")
                                                        .payload("INFO")
                                                        .build(),
                                                BotAnswerMessage.Button.builder()
                                                        .type("callback")
                                                        .text("🔍 Поиск патентов")
                                                        .payload("PATENT_SEARCH")
                                                        .build()
                                        )
                                ))
                                .build())
                        .build()
                ))
                .build();

        return sendMessage(chatId, response);
    }
}