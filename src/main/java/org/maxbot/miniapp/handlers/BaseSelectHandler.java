package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.util.BotAnswerUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BaseSelectHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {

        if (event != null && event.getPayload() != null && !"BACK".equals(event.getPayload())) {
            String base = event.getPayloadDescription();
            ctx.setSelectedBase(base);
        }

        List<List<BotResponse.Button>> buttons = BotAnswerUtil.getButtons(Map.of(
                "Дата", "DATE_INPUT",
                "Поисковые массивы", "SEARCH_ARRAYS",
                "Классификаторы", "CLASSIFIERS"));

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад к выбору базы")
                .payload("BACK")
                .build()));

        // --- Формируем ответ
        String selectBase = ctx.getSelectedBase();
        String selectDate = ctx.getSelectedBase() !=null? ctx.getDate() : "";
        String selectFilter = ctx.getFilters().toString();
        String selectArrays = ctx.getSearchArrays().toString();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Выбрана база: ").append(selectBase).append("\n");
        if (selectBase != null){
            stringBuilder.append("Установлена дата: ").append(selectDate).append("\n");
        }
        if (selectFilter != null){
            stringBuilder.append("Установлены фильтры: ").append(selectFilter).append("\n");
        }

        return BotResponse.builder()
                .text(stringBuilder.toString())
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
