package org.maxbot.miniapp.dto.miniapp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MiniAppActionResponse {

    private String title;                 // Заголовок блока
    private String description;           // Описание
    private boolean success;              // Флаг успешности
    private List<MiniAppCard> cards;      // Карточки результата
    private Map<String, Object> meta;     // Доп. данные (опционально)

    /**
     * Успешный ответ с карточками
     */
    public static MiniAppActionResponse ofCards(List<MiniAppCard> cards) {
        return MiniAppActionResponse.builder()
                .title("Результаты поиска")
                .description("Найдено патентов: " + cards.size())
                .success(true)
                .cards(cards)
                .build();
    }

    /**
     * Ошибка
     */
    public static MiniAppActionResponse error(String message) {
        return MiniAppActionResponse.builder()
                .title("Ошибка")
                .description(message)
                .success(false)
                .cards(List.of())
                .build();
    }
}



