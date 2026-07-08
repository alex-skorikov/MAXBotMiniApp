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
public class MiniAppInitResponse {

    private String title;
    private String description;

    private List<MiniAppAction> actions;

    private Map<String, Object> form;         // Поля ввода
}

