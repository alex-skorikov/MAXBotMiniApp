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
        String selectDate = ctx.getSelectedBase();
        String classifiers = ctx.getClassifiers();
        String selectArrays = ctx.getSearchArrays();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Выбрана база: ").append(selectBase).append("\n");
        if (selectDate != null){
            stringBuilder.append("Фильтр по дате установлен: ").append(selectDate).append("\n");
        }
        if (!selectArrays.isEmpty()){
            stringBuilder.append("Выбран массив: ").append(selectArrays).append("\n");
        }
        if (!classifiers.isEmpty()){
            stringBuilder.append("Клсссифкатор ").append(classifiers).append(" установлен");
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
