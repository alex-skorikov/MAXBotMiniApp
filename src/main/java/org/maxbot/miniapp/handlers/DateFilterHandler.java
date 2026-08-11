package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DateFilterHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад к выбору фильтров")
                .payload("BACK")
                .build()));

        return BotResponse.builder()
                .text("\uD83D\uDCC5 Введите дату в формате 2020-01-01:")
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder().buttons(buttons).build())
                        .build()
                )).build();
    }
}


