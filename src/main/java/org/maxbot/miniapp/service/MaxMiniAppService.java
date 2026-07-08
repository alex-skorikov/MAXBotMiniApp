package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.RospatentClient;
import org.maxbot.miniapp.dto.miniapp.*;
import org.maxbot.miniapp.dto.patent.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class MaxMiniAppService {

    private final RospatentClient rospatentClient;

    public MaxMiniAppService(RospatentClient rospatentClient) {
        this.rospatentClient = rospatentClient;
    }

    /**
     * Обработка /miniapp/init
     */
    public MiniAppInitResponse init(MiniAppInitRequest req) {

        return MiniAppInitResponse.builder()
                .title("Поиск патентов")
                .description("Введите ключевые слова для поиска патентов в базе Роспатента")
                .actions(List.of(
                        new MiniAppAction("search", "Поиск патента")
                ))
                .form(Map.of(
                        "query", Map.of(
                                "type", "text",
                                "label", "Ключевые слова",
                                "placeholder", "например: нейросеть обработка изображений"
                        )
                ))
                .build();
    }

    /**
     * Обработка /miniapp/action
     */
    public MiniAppActionResponse handleAction(MiniAppActionRequest req) {

        String action = req.getAction();

        if ("search".equals(action)) {

            String query = req.getInput().get("query");

            if (query == null || query.isBlank()) {
                return MiniAppActionResponse.error("Введите поисковый запрос");
            }

            PatentSearchResponse result = rospatentClient.search(query);

            List<MiniAppCard> cards = result.toCards();

            return MiniAppActionResponse.ofCards(cards);
        }

        return MiniAppActionResponse.error("Неизвестное действие: " + action);
    }
}


