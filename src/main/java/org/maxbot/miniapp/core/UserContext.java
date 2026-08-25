package org.maxbot.miniapp.core;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Builder
public class UserContext {
    private int userId;
    private String chatId;
    private String selectedBase;
    private String date;
    private String datasetName;
    private List<String> datasetArrays;
    private String classifiers;
    private BotStates state;
    private BotEvents botEvent;
    private String searchQuery;
    @ToString.Exclude
    private List<PatentHit> hits;
    private int offset = 0;
    private int limit = 5;
}



