package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.util.BotAnswerUtil;
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
