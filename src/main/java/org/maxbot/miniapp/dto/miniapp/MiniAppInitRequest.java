package org.maxbot.miniapp.dto.miniapp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MiniAppInitRequest {

    private String userId;          // ID пользователя в MAX
    private String sessionId;       // ID сессии MiniApp
    private String locale;          // язык интерфейса (ru, en)
    private Map<String, Object> context; // контекст MiniApp (если есть)
}

