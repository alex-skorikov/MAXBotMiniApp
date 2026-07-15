package org.maxbot.miniapp.client;


import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.maxbot.miniapp.service.PatentCardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(MaxApiClient.class);

    public MaxApiClient(@Value("${max.api.token}") String token) {

        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public Mono<Void> sendMessage(int chatId, Map<String, Object> bodyValue) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("chat_id", chatId)
                        .build())
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> sendMessage(int chatId, BotAnswerMessage bodyValue) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("chat_id", chatId)
                        .build())
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public Mono<Void> sendAnswer(String callbackId, Map<String, Object> bodyValue) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/answers")
                        .queryParam("callback_id", callbackId)
                        .build())
                .bodyValue(bodyValue)
                .retrieve()
                .bodyToMono(Void.class);
    }

    public void sendMenu(int chatId) {
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

        sendMessage(chatId, response).subscribe();
    }
}