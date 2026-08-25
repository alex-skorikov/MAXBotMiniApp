package org.maxbot.miniapp.dto.webapp;

import lombok.Data;

@Data
public class SessionInitRequest {
    private String userId;
    private String chatId;
}