package org.maxbot.miniapp.core;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.maxbot.miniapp.statemachine.BotEvents;

@Getter
@Setter
@ToString
public class BotEvent {
    private String userId;
    private String chatId;
    private BotEvents type;
    private String text;
    private String callbackId;
    private String payload;
    private String payloadDescription;

}
