package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.util.BotAnswerUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class ArrayFilterHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = BotAnswerUtil.getButtons(Map.of(
                "Россия и страны СНГ", "COUNTRY_INPUT",
                "Минимум РСТ", "RST_INPUT",
                "Промышленные образцы", "INDUSTRIAL_INPUT",
                "Страны с малым ПФ", "SMALL_PF_INPUT"));

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад к выбору фильтров")
                .payload("BACK")
                .build()));

        return BotResponse.builder()
                .notify(false)
                .text("Выберите поисковый массив:")
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
