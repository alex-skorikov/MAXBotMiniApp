package org.maxbot.miniapp.util;

import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class BotAnswerUtil {
    public static List<List<BotResponse.Button>> getButtons(Map<String, String> objectMap) {
        List<List<BotResponse.Button>>  result = new ArrayList<>();
        objectMap.forEach((k, v) -> {
            List<BotResponse.Button> list = new ArrayList<>();
            BotResponse.Button callback = BotResponse.Button.builder()
                    .type("callback")
                    .text(k)
                    .payload(v)
                    .build();
            list.add(callback);
            result.add(list);
        });
        return result;
    }
}
