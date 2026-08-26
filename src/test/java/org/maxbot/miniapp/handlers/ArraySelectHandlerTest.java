package org.maxbot.miniapp.handlers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.repository.HashMapContextRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ArraySelectHandlerTest {

    private HashMapContextRepository contextRepository;
    private ArraySelectHandler handler;

    @BeforeEach
    void setUp() {
        contextRepository = new HashMapContextRepository();
        contextRepository.clear();
        handler = new ArraySelectHandler(contextRepository);
    }

    @Test
    void shouldSaveContextAndReturnSuccessResponse() {
        // Given
        UserContext ctx = new UserContext();
        ctx.setUserId(123456);
        ctx.setChatId("12345");

        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        assertEquals("Поисковый массив выбран", response.getText());
        assertFalse(response.isNotify());

        UserContext savedCtx = contextRepository.load("123456");
        assertNotNull(savedCtx, "Контекст должен быть успешно сохранен в репозиторий");
        assertEquals("12345", savedCtx.getChatId());
    }
}
