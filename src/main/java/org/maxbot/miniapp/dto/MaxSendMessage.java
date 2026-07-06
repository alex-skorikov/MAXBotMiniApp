package org.maxbot.miniapp.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
public class MaxSendMessage {
    private String chatId;
    private String text;

    public MaxSendMessage(String chatId, String text) {
        this.chatId = chatId;
        this.text = text;
    }
}
