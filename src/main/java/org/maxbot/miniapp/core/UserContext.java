package org.maxbot.miniapp.core;

import lombok.*;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;

import java.time.LocalDate;
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
    private String date;
    private Map<String, Object> filters = new HashMap<>();
    private BotStates state;
    private BotEvents botEvent;
}



