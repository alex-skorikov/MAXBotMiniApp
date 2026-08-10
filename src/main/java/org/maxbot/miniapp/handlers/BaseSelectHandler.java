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
                .text("🔙 Назад к выбору базы")
                .payload("BACK")
                .build()));

        // --- Формируем ответ безопасно
        String selectBase = ctx.getSelectedBase();
        String selectDate = ctx.getDate(); // 🟢 ФИКС: Считываем именно дату (.getDate()), а не базу заново
        String classifiers = ctx.getClassifiers();
        String selectArrays = ctx.getSearchArrays();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Выбрана база: ").append(selectBase != null ? selectBase : "Не выбрана").append("\n");

        if (selectDate != null && !selectDate.isBlank()) {
            stringBuilder.append("Фильтр по дате установлен: ").append(selectDate).append("\n");
        }

        if (selectArrays != null && !selectArrays.isBlank()) {
            stringBuilder.append("Выбран массив: ").append(selectArrays).append("\n");
        }

        if (classifiers != null && !classifiers.isBlank()) {
            stringBuilder.append("Классификатор ").append(classifiers).append(" установлен");
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
