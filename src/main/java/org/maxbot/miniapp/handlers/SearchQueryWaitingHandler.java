package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SearchQueryWaitingHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = new ArrayList<>();

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("🔙 Назад к фильтрам")
                .payload("BACK")
                .build()));

        return BotResponse.builder()
                .text("📝 **Шаг 2: Ввод поискового запроса**\n\nВсе фильтры успешно настроены! Теперь отправьте в чат текстовое сообщение с ключевыми словами для поиска патентов (например: *\"беспилотный летательный аппарат\"*):")
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
