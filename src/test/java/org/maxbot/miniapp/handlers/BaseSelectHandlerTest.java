package org.maxbot.miniapp.handlers;

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaseSelectHandlerTest {

    private final BaseSelectHandler handler = new BaseSelectHandler();

    @Test
    void shouldReturnResponseWithDefaultTextWhenNoFiltersAreSet() {
        // Given
        UserContext ctx = new UserContext(); // Все поля null
        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        assertFalse(response.isNotify());
        assertNotNull(response.getText());

        String actualText = response.getText();
        assertTrue(actualText.contains("Выбрана база: Не выбрана"),
                "Текст должен содержать дефолтное состояние базы. Было: " + actualText);

        assertFalse(actualText.contains("Фильтр по дате установлен:"));
        assertFalse(actualText.contains("Выбран массив:"));
        assertFalse(actualText.contains("Классификатор:"));

        validateNavigationButtons(response);
    }

    @Test
    void shouldReturnResponseWithFullTextWhenAllFiltersAreSet() {
        // Given
        UserContext ctx = new UserContext();
        ctx.setSelectedBase("Патенты");
        ctx.setDate("2026-08-17");
        ctx.setDatasetName("Россия и страны СНГ");
        ctx.setClassifiers("F02K9/00");

        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        assertNotNull(response.getText());

        String actualText = response.getText();

        assertTrue(actualText.contains("Выбрана база: Патенты"));
        assertTrue(actualText.contains("Фильтр по дате установлен: 2026-08-17"));
        assertTrue(actualText.contains("Выбран массив: Россия и страны СНГ"));
        assertTrue(actualText.contains("Классификатор: F02K9/00 установлен"));

        validateNavigationButtons(response);
    }

    private void validateNavigationButtons(BotResponse response) {
        assertNotNull(response.getAttachments(), "Attachments не должны быть null");
        assertFalse(response.getAttachments().isEmpty(), "Attachments не должны быть пустыми");

        BotResponse.Attachment attachment = response.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());

        List<List<BotResponse.Button>> buttons = attachment.getPayload().getButtons();
        assertNotNull(buttons);

        // Последний ряд должен быть навигационным и содержать ровно 3 кнопки
        List<BotResponse.Button> navigationRow = buttons.get(buttons.size() - 1);
        assertEquals(3, navigationRow.size());

        assertEquals("⬅️ Назад", navigationRow.get(0).getText());
        assertEquals("BACK", navigationRow.get(0).getPayload());

        assertEquals("🔍 Поиск", navigationRow.get(1).getText());
        assertEquals("START_SEARCH", navigationRow.get(1).getPayload());

        assertEquals("🔄 Сбросить", navigationRow.get(2).getText());
        assertEquals("BACK_TO_START", navigationRow.get(2).getPayload());
    }
}
