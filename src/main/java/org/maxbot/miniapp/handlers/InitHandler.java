package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.*;
import org.maxbot.miniapp.statemachine.BotEvents; // Импортируем ваши ивенты
import org.maxbot.miniapp.statemachine.BotStates;
import org.maxbot.miniapp.util.BotAnswerUtil;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action; // Импортируем Action
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class InitHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = BotAnswerUtil.getButtons(Map.of(
                "Патенты", "PATENTS",
                "Промобразцы", "PROM_SAMPLE",
                "Полезные модели", "MODELS"));

        return BotResponse.builder()
                .text("Добро пожаловать! Выберите базу:")
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
