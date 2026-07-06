package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.MaxApiClient;
import org.springframework.stereotype.Service;

@Service
public class PatentSearchService {

    private final MaxApiClient maxApiClient;

    public PatentSearchService(MaxApiClient maxApiClient) {
        this.maxApiClient = maxApiClient;
    }

    public Object processIncomingMessage(String message) {

        // Заглушка поиска патентов
        String result = "Патент по запросу '%s' не найден (заглушка)".formatted(message);

        // Отправка ответа в MAX
//        maxApiClient.sendUserMessage(329529068L, "Привет!", result);

        return new Response(result);
    }

    public record Response(String result) {}
}
