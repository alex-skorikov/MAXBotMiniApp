package org.maxbot.miniapp.dto.miniapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAppAction {
    private String action;    // "search"
    private String label;     // "Поиск патента"
}

