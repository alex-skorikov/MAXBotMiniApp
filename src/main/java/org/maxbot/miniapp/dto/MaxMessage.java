package org.maxbot.miniapp.dto;

import lombok.*;

@Data
@Getter
@Setter
public class MaxMessage {
    private String chatId;
    private String text;

    public MaxMessage(String channel, String text) {
        this.chatId = channel;
        this.text = text;
    }
}
