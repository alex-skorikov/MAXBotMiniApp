package org.maxbot.miniapp.handlers; // Скорректируйте пакет при необходимости

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SearchQueryWaitingHandlerTest {

    private final SearchQueryWaitingHandler handler = new SearchQueryWaitingHandler();

    @Test
    void shouldReturnSearchInputPromptWithBackButton() {
        // Given
        UserContext ctx = new UserContext();
        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        // 1. Проверяем базовые параметры ответа
        assertNotNull(response);
        assertFalse(response.isNotify(), "Флаг notify должен быть false");
        assertEquals("📝 Ввод поискового запроса", response.getText());

        // 2. Проверяем наличие инлайн-клавиатуры в аттачментах
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());

        BotResponse.Attachment attachment = response.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());

        // 3. Валидируем кнопку "Назад к фильтрам"
        assertNotNull(attachment.getPayload());
        List<List<BotResponse.Button>> buttons = attachment.getPayload().getButtons();
        assertNotNull(buttons);
        assertEquals(1, buttons.size(), "Должен быть один ряд кнопок");
        assertEquals(1, buttons.get(0).size(), "В ряду должна быть ровно одна кнопка");

        BotResponse.Button backButton = buttons.get(0).get(0);
        assertEquals("callback", backButton.getType());
        assertEquals("🔙 Назад к фильтрам", backButton.getText());
        assertEquals("BACK", backButton.getPayload());
    }
}
