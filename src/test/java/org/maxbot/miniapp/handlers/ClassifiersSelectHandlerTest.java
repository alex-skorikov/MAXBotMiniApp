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

class ClassifiersSelectHandlerTest {

    private HashMapContextRepository contextRepository;
    private ClassifiersSelectHandler handler;

    @BeforeEach
    void setUp() {
        contextRepository = new HashMapContextRepository();
        contextRepository.clear();
        handler = new ClassifiersSelectHandler(contextRepository);
    }

    @Test
    void shouldSaveClassifierPayloadToDateAndReturnSuccessResponse() {
        // Given
        int chatId = 54321;
        UserContext ctx = new UserContext();
        ctx.setUserId(chatId);
        ctx.setChatId(String.valueOf(chatId));

        BotEvent event = new BotEvent();
        // Передаем строку-описание (например, код классификатора или выбранное значение)
        String expectedPayload = "F02K9/00";
        event.setPayloadDescription(expectedPayload);

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        // 1. Проверяем возвращаемый ответ бота
        assertNotNull(response);
        assertEquals("Классификатор выбран", response.getText());
        assertFalse(response.isNotify());

        // 2. Проверяем, что значение установилось в поле Date контекста (как прописано в коде хендлера)
        assertEquals(expectedPayload, ctx.getDate());

        // 3. Проверяем, что контекст успешно обновился в репозитории
        UserContext savedCtx = contextRepository.load(String.valueOf(chatId));
        assertNotNull(savedCtx);
        assertEquals(expectedPayload, savedCtx.getDate());
    }
}
