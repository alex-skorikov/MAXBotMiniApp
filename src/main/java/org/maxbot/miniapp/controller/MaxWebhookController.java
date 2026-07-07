package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@RestController
public class MaxWebhookController {

    private final WebClient webClient;

    public MaxWebhookController(@Value("${max.api.token}") String token) {
        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @PostMapping("/webhook")
    public Mono<Void> handleUpdate(@RequestBody UpdateDto update) {

        System.out.println(">>> Incoming update: " + update);

        MessageDto msg = update.getMessage();
        if (msg == null) {
            return Mono.empty();
        }

        SenderDto sender = msg.getSender();
        RecipientDto recipient = msg.getRecipient();
        BodyDto body = msg.getBody();

        int userId = sender.getUser_id();
//        int chatId = recipient.getChat_id();
        String text = body.getText();

        String reply = "Информация о вас:\n" +
                "ID: " + userId + "\n" +
                "Имя: " + sender.getFirst_name() + " " + sender.getLast_name() + "\n" +
                "Текст: " + text;

        Map<String, Object> sendBody = Map.of(
                "text", reply,
                "attachments", List.of()
        );

        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("user_id", userId)
                        .build())
                .bodyValue(sendBody)
                .retrieve()
                .bodyToMono(Void.class);
    }
}

