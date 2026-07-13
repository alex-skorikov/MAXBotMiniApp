package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.bot.BodyDto;
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

        BodyDto body = msg.getBody();
        String text = body.getText();
        String payload = body.getPayload();
        int userId = msg.getSender().getUser_id();

        // 1) Если нажата кнопка
        if (payload != null) {
            switch (payload) {

                case "INFO":
                    return sendMessage(userId, Map.of(
                            "text", "Информация о вас:\n" +
                                    "ID: " + userId + "\n" +
                                    "Имя: " + msg.getSender().getFirst_name() + " " + msg.getSender().getLast_name()
                    )).then(sendMessage(userId, mainMenu()));

                case "PATENT_SEARCH":
                    userState.put(userId, "PATENT_SEARCH");
                    return sendMessage(userId, Map.of(
                            "text", "Введите поисковый запрос:"
                    ));
            }
        }

        // 2) Если пользователь вводит текст в режиме поиска
        if ("PATENT_SEARCH".equals(userState.get(userId))) {
            return handlePatentSearch(userId, text)
                    .then(sendMessage(userId, mainMenu()));
        }

        // 3) На любое другое сообщение → показываем две кнопки
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


//    private Map<String, Object> mainMenu() {
//        return Map.of(
//                "text", "Добро пожаловать! Выберите действие:",
//                "quick_replies", List.of(
//                        Map.of("title", "ℹ️ Информация", "payload", "INFO"),
//                        Map.of("title", "🔍 Поиск патентов", "payload", "PATENT_SEARCH")
//                )
//        );
//    }

    private Map<String, Object> mainMenu() {
        return Map.of(
                "text", "Выберите действие:",
                "attachments", List.of(
                        Map.of(
                                "type", "inline_keyboard",
                                "payload", Map.of(
                                        "buttons", List.of(
                                                List.of(
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "ℹ️ Информация",
                                                                "payload", "INFO"
                                                        ),
                                                        Map.of(
                                                                "type", "callback",
                                                                "text", "🔍 Поиск патентов",
                                                                "payload", "PATENT_SEARCH"
                                                        )
                                                )
                                        )
                                )
                        )
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

