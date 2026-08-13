package org.maxbot.miniapp.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.mockito.Mockito;

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
    void handleReturnsCorrectBotResponseStructure() {
        // Given
        UserContext mockCtx = Mockito.mock(UserContext.class);
        BotEvent mockEvent = Mockito.mock(BotEvent.class);

        // When
        BotResponse response = handler.handle(mockCtx, mockEvent);

        // Then
        assertNotNull(response);
        assertFalse(response.isNotify());
        assertTrue(response.getText().contains("Введите дату в формате 2020-01-01:"));

        // Проверяем структуру кнопок инлайн-клавиатуры
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());

        BotResponse.Attachment attachment = response.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());
        assertNotNull(attachment.getPayload());

        List<List<BotResponse.Button>> buttonRows = attachment.getPayload().getButtons();
        assertNotNull(buttonRows);
        assertEquals(1, buttonRows.size());

        BotResponse.Button backButton = buttonRows.get(0).get(0);
        assertEquals("callback", backButton.getType());
        assertEquals("BACK", backButton.getPayload());
        assertTrue(backButton.getText().contains("Назад к выбору фильтров"));

    }
}