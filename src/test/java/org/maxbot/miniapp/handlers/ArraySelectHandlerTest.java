package org.maxbot.miniapp.handlers; // Укажите ваш точный пакет для хендлеров

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
    void shouldSaveSearchArraysAndReturnSuccessResponse() {
        // Given
        int chatId = 12345;
        UserContext ctx = new UserContext();
        ctx.setUserId(chatId);
        ctx.setChatId(String.valueOf(chatId));

        BotEvent event = new BotEvent();
        // Передаем список массивов (например, "cis", "ru_pat")
        String expectedArrays = "[cis, ru_pat]";
        event.setPayloadDescription(expectedArrays);

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        // 1. Проверяем возвращаемый ответ бота
        assertNotNull(response);
        assertEquals("Поисковый массив выбран", response.getText());
        assertFalse(response.isNotify());

        // 2. Проверяем, что состояние контекста изменилось прямо в памяти
        assertEquals(expectedArrays, ctx.getSearchArrays());

        // 3. Проверяем, что контекст успешно сохранился в репозиторий (Redis / HashMap)
        UserContext savedCtx = contextRepository.load(String.valueOf(chatId));
        assertNotNull(savedCtx);
        assertEquals(expectedArrays, savedCtx.getSearchArrays());
    }
}
