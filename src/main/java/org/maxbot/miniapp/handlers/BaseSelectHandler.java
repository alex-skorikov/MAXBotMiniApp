package org.maxbot.miniapp.handlers;

import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.util.BotAnswerUtil;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
public class BaseSelectHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = BotAnswerUtil.getButtons(Map.of(
                "Дата", "DATE_INPUT",
                "Поисковые массивы", "SEARCH_ARRAYS",
                "Классификаторы", "CLASSIFIERS"));

        log.info(">>> BaseSelectHandler обрабатываем... Event: {}, UserContext: {}", event, ctx);

        // --- Формируем ответ
        String selectBase = ctx.getSelectedBase();
        String selectDate = ctx.getDate();
        String classifiers = ctx.getClassifiers();
        String selectArrays = ctx.getSearchArrayName();

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Выбрана база: ").append(selectBase != null ? selectBase : "Не выбрана").append("\n");

        if (selectDate != null && !selectDate.isBlank()) {
            stringBuilder.append("Фильтр по дате установлен: ").append(selectDate).append("\n");
        }

        if (selectArrays != null && !selectArrays.isBlank()) {
            stringBuilder.append("Выбран массив: ").append(selectArrays).append("\n");
        }

        if (classifiers != null && !classifiers.isBlank()) {
            stringBuilder.append("Классификатор: ").append(classifiers).append(" установлен");
        }

        // Кнопки переходов
        List<BotResponse.Button> navigationRow = new ArrayList<>();
        navigationRow.add(BotResponse.Button.builder()
                .type("callback")
                .text("⬅️ Назад")
                .payload("BACK")
                .build());
        navigationRow.add(BotResponse.Button.builder()
                .type("callback")
                .text("🔍 Поиск")
                .payload("START_SEARCH") // payload для перехода к вводу строки
                .build());
        navigationRow.add(BotResponse.Button.builder()
                .type("callback")
                .text("🔄 Сбросить")
                .payload("BACK_TO_START")
                .build());

        buttons.add(navigationRow);

        return BotResponse.builder()
                .notify(false)
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
