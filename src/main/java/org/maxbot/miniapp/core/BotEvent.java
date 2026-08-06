package org.maxbot.miniapp.core;

import lombok.Getter;
import lombok.Setter;
import org.maxbot.miniapp.statemachine.BotEvents;

@Getter
@Setter
public class BotEvent {
    private String userId;
    private String chatId;
    private BotEvents type;
    private String text;
    private String callbackData;
    private String payload;
    private String payloadDescription;

}
