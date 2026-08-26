package org.maxbot.miniapp.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateFilterHandlerTest {

    private DateFilterHandler handler;

    @BeforeEach
    void setUp() {
        this.handler = new DateFilterHandler();
    }

    @Test
    void shouldReturnDefaultMessageWhenEventTextIsNull() {
        // Given
        UserContext ctx = new UserContext();
        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        assertFalse(response.isNotify());

        String actualText = response.getText();
        assertTrue(actualText.contains("Введите дату в формате 2020-01-01:"));
        assertFalse(actualText.contains("Неверный формат даты"), "Префикса ошибки быть не должно");
        validateBackButton(response);
    }

    @Test
    void shouldReturnErrorMessagePrefixWhenDateDescriptionIsInvalid() {
        // Given
        UserContext ctx = new UserContext();
        BotEvent event = new BotEvent();
        event.setText("25-12-2025");

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        String actualText = response.getText();

        assertTrue(actualText.contains("Неверный формат даты! Пожалуйста, используйте YYYY-MM-DD."));
        assertTrue(actualText.contains("Введите дату в формате 2020-01-01:"));

        validateBackButton(response);
    }

    @Test
    void shouldNotReturnErrorMessageWhenDateDescriptionIsValid() {
        // Given
        UserContext ctx = new UserContext();
        BotEvent event = new BotEvent();
        event.setText("2026-08-17");

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        String actualText = response.getText();

        assertFalse(actualText.contains("Неверный формат даты!"), "Для верного формата префикс ошибки не нужен");
        assertTrue(actualText.contains("Введите дату в формате 2020-01-01:"));
        validateBackButton(response);
    }

    // Вспомогательный метод для проверки кнопки "Назад"
    private void validateBackButton(BotResponse response) {
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());

        BotResponse.Attachment attachment = response.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());
        assertNotNull(attachment.getPayload());

        List<List<BotResponse.Button>> buttonRows = attachment.getPayload().getButtons();
        assertNotNull(buttonRows);
        assertEquals(1, buttonRows.size());

        BotResponse.Button backButton = buttonRows.get(0).get(0);
        assertEquals("BACK", backButton.getPayload());
        assertTrue(backButton.getText().contains("Назад к выбору фильтров"));
    }
}
