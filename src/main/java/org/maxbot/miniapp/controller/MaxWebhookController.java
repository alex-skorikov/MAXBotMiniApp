package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MaxWebhookController {
    private final Map<Integer, String> userState = new ConcurrentHashMap<>();
    private final WebClient webClient;
    private final PatentSearchService patentSearchService;

    public MaxWebhookController(@Value("${max.api.token}") String token, PatentSearchService patentSearchService) {
        this.webClient = WebClient.builder()
                .baseUrl("https://platform-api2.max.ru")
                .defaultHeader("Authorization", token)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.patentSearchService = patentSearchService;
    }

//    @PostMapping("/webhook")
//    public Mono<Void> handleUpdate(@RequestBody UpdateDto update) {
//
//        System.out.println(">>> Incoming update: " + update);
//
//        MessageDto msg = update.getMessage();
//        if (msg == null) {
//            return Mono.empty();
//        }
//
//        SenderDto sender = msg.getSender();
//        RecipientDto recipient = msg.getRecipient();
//        BodyDto body = msg.getBody();
//
//        int senderUserId = sender.getUser_id();

    /// /        int chatId = recipient.getChat_id();
    /// /        int recipientUserId = recipient.getUser_id();
//        String text = body.getText();
//
//        String reply = "Информация о вас:\n" +
//                "ID: " + senderUserId + "\n" +
//                "Имя: " + sender.getFirst_name() + " " + sender.getLast_name() + "\n" +
//                "Текст: " + text;
//
//        Map<String, Object> sendBody = Map.of(
//                "text", reply,
//                "attachments", List.of()
//        );
//
//        return webClient.post()
//                .uri(uriBuilder -> uriBuilder
//                        .path("/messages")
//                        .queryParam("user_id", senderUserId)
//                        .build())
//                .bodyValue(sendBody)
//                .retrieve()
//                .bodyToMono(Void.class);
//    }
    @PostMapping("/webhook")
    public Mono<Void> handleUpdate(@RequestBody UpdateDto update) {

        MessageDto msg = update.getMessage();
        if (msg == null) return Mono.empty();

        String text = msg.getBody().getText();
        int userId = msg.getSender().getUser_id();

        // если нажата кнопка
        if (msg.getBody().getPayload() != null) {
            String payload = msg.getBody().getPayload();

            switch (payload) {
                case "PATENT_SEARCH":
                    userState.put(userId, "PATENT_SEARCH");
                    return sendMessage(userId, Map.of(
                            "text", "Введите поисковый запрос (например: ракета):"
                    ));
            }
        }

        // если пользователь вводит текст
        String state = userState.get(userId);
        if ("PATENT_SEARCH".equals(state)) {
            return handlePatentSearch(userId, text);
        }

        // иначе показываем меню
        return sendMessage(userId, mainMenu());
    }

    private Mono<Void> sendMessage(int userId, Map<String, Object> body) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/messages")
                        .queryParam("user_id", userId)
                        .build())
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class);
    }


    private Map<String, Object> mainMenu() {
        return Map.of(
                "text", "Добро пожаловать! Выберите действие:",
                "quick_replies", List.of(
                        Map.of("title", "ℹ️ Информация", "payload", "INFO"),
                        Map.of("title", "🔍 Поиск патентов", "payload", "PATENT_SEARCH")
                )
        );
    }
    private Mono<Void> handlePatentSearch(int userId, String query) {

        PatentSearchResponse raw = patentSearchService.search(
                query,
                "q",      // режим текстового поиска
                10,       // limit
                0         // offset
        );

        if (raw.getHits() == null || raw.getHits().isEmpty()) {
            return sendMessage(userId, Map.of("text", "Ничего не найдено."));
        }

        StringBuilder sb = new StringBuilder("Результаты поиска:\n\n");

        raw.getHits().forEach(item -> {
            sb.append("📄 ").append(item.getTitle()).append("\n");
            if (item.getInventor() != null)
                sb.append("👤 ").append(item.getInventor()).append("\n");
            if (item.getIpc() != null)
                sb.append("🔧 IPC: ").append(item.getIpc()).append("\n");
            sb.append("\n");
        });

        userState.remove(userId);

        return sendMessage(userId, Map.of("text", sb.toString()));
    }



}

