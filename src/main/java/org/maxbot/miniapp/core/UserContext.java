package org.maxbot.miniapp.core;

import lombok.*;
import org.maxbot.miniapp.statemachine.BotStates;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserContext {
    private int userId;
    private String chatId;
    private String selectedBase;
    private Map<String, Object> filters = new HashMap<>();
    private BotStates state;
}



