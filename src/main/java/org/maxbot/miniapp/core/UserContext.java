package org.maxbot.miniapp.core;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;


@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class UserContext {
    private int userId;
    private String chatId;
    private String selectedBase;
    private String date;
    private String searchArrays;
    private String classifiers;
    private BotStates state;
    private BotEvents botEvent;
    private String searchQuery;
    private int searchOffset = 0;
    private int searchLimit = 5;
}



