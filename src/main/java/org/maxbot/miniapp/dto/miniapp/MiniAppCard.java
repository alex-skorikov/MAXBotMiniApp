package org.maxbot.miniapp.dto.miniapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class MiniAppCard {

    private String id;        // RU123456
    private String title;     // Способ обработки изображений
    private String subtitle;  // Владелец: ООО "Техно"
    private String description; // Дата: 2023
}

