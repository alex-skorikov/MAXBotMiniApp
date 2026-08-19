package org.maxbot.miniapp.handlers; // Скорректируйте пакет при необходимости

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArrayFilterHandlerTest {

    private final ArrayFilterHandler handler = new ArrayFilterHandler();

    @Test
    void shouldReturnArraySelectionMenuWithBackButton() {
        // Given
        UserContext ctx = new UserContext();
        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        // 1. Проверяем базовые поля ответа
        assertNotNull(response);
        assertFalse(response.isNotify());
        assertEquals("Выберите поисковый массив:", response.getText());

        // 2. Проверяем структуру вложений инлайн-клавиатуры
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());

        BotResponse.Attachment attachment = response.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());

        // 3. Проверяем наличие сгенерированных кнопок
        assertNotNull(attachment.getPayload());
        List<List<BotResponse.Button>> buttons = attachment.getPayload().getButtons();
        assertNotNull(buttons);
        assertFalse(buttons.isEmpty());

        // Проверяем, что все функциональные кнопки присутствуют в разметке
        boolean hasCis = false;
        boolean hasRst = false;
        boolean hasIndustrial = false;
        boolean hasSmallPf = false;
        boolean hasBack = false;

        for (List<BotResponse.Button> row : buttons) {
            for (BotResponse.Button button : row) {
                if ("Россия и страны СНГ".equals(button.getText())) hasCis = true;
                if ("Минимум РСТ".equals(button.getText())) hasRst = true;
                if ("Промышленные образцы".equals(button.getText())) hasIndustrial = true;
                if ("Страны с малым ПФ".equals(button.getText())) hasSmallPf = true;
                if ("🔙 Назад к выбору фильтров".equals(button.getText()) && "BACK".equals(button.getPayload())) {
                    hasBack = true;
                }
            }
        }

        assertTrue(hasCis, "Должна быть кнопка 'Россия и страны СНГ'");
        assertTrue(hasRst, "Должна быть кнопка 'Минимум РСТ'");
        assertTrue(hasIndustrial, "Должна быть кнопка 'Промышленные образцы'");
        assertTrue(hasSmallPf, "Должна быть кнопка 'Страны с малым ПФ'");
        assertTrue(hasBack, "Должна быть корректно настроенная кнопка 'Назад'");
    }
}
