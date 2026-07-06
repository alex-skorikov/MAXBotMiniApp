package org.maxbot.miniapp.controller;


import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class MaxBotController {


    private final MaxApiClient maxApiClient;
    private final PatentSearchService patentSearchService;

    public MaxBotController(MaxApiClient maxApiClient,
                            PatentSearchService patentSearchService) {
        this.maxApiClient = maxApiClient;
        this.patentSearchService = patentSearchService;
        System.out.println(">>> MaxBotController LOADED");

    }

/*    @PostMapping("/webhook")
    public Map<String, Object> handleUpdate(@RequestBody Map<String, Object> update) {
        return Map.of("ok", true);
    }*/

    @PostMapping("/webhook")
    public Map<String, Object> handleUpdate(@RequestBody Map<String, Object> update) {

        System.out.println(">>> Incoming update: " + update);

        Map<String, Object> message = (Map<String, Object>) update.get("message");
        if (message == null) {
            return Map.of("ok", true);
        }

        String chatId = String.valueOf(message.get("chat_id"));
        String text = String.valueOf(message.getOrDefault("text", ""));

        // Обработка команды /start
        if (text.equals("/start")) {

            String reply = """
                    Привет! Я ваш MAXBotMiniApp.
                    Готов помочь вам с поиском патентов и другой информацией.
                    """;

            maxApiClient.sendMessage(chatId, reply);
            return Map.of("ok", true);
        }

        // Обработка обычных сообщений
        Map<String, Object> from = (Map<String, Object>) message.get("from");

        String firstName = from != null ? String.valueOf(from.getOrDefault("first_name", "не задано")) : "не задано";
        String lastName = from != null ? String.valueOf(from.getOrDefault("last_name", "")) : "";
        String username = from != null ? String.valueOf(from.getOrDefault("username", "не задан")) : "не задан";
        String role = from != null ? String.valueOf(from.getOrDefault("role", "неизвестно")) : "неизвестно";

        String reply = String.format(
                "Информация о вас:\n" +
                        "ID: %s\n" +
                        "Имя: %s %s\n" +
                        "Username: %s\n" +
                        "Роль: %s",
                chatId, firstName, lastName, username, role
        );

        maxApiClient.sendMessage(chatId, reply);

        return Map.of("ok", true);
    }
}
