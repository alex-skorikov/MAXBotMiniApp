package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.maxbot.miniapp.util.BotAnswerUtil;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BaseSelectHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {

        if (event != null && event.getPayload() != null && !"BACK".equals(event.getPayload())) {
            String base = event.getPayload();
            ctx.setSelectedBase(base);
        }

        List<List<BotResponse.Button>> buttons = BotAnswerUtil.getButtons(Map.of(
                "Дата", "DATE",
                "Поисковые массивы", "SEARCH_ARRAYS",
                "Классификаторы", "CLASSIFIERS"));

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад к выбору базы")
                .payload("BACK")
                .build()));

        return BotResponse.builder()
                .text("Выбрана база: " + ctx.getSelectedBase() + "\nВыберите фильтры")
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder()
                                .buttons(buttons)
                                .build())
                        .build()
                ))
                .build();
    }
}
