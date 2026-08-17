package org.maxbot.miniapp.handlers; // Скорректируйте пакет под вашу структуру

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class BaseSelectHandlerTest {

    private final BaseSelectHandler handler = new BaseSelectHandler();

    @Test
    void shouldReturnResponseWithDefaultTextWhenNoFiltersAreSet() {
        // Given
        UserContext ctx = new UserContext(); // Все поля null по умолчанию
        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);
        assertFalse(response.isNotify());

        // Проверяем текст (должна сработать ветка "Не выбрана" и не должно быть строк фильтров)
        String expectedText = "Выбрана база: Не выбрана\n";
        assertEquals(expectedText, response.getText());

        // Проверяем наличие кнопок навигации
        validateNavigationButtons(response);
    }

    @Test
    void shouldReturnResponseWithFullTextWhenAllFiltersAreSet() {
        // Given
        UserContext ctx = new UserContext();
        ctx.setSelectedBase("Патенты");
        ctx.setDate("2026-08-17");
        ctx.setSearchArrays("[cis, ru_pat]");
        ctx.setClassifiers("F02K9/00");

        BotEvent event = new BotEvent();

        // When
        BotResponse response = handler.handle(ctx, event);

        // Then
        assertNotNull(response);

        // Проверяем, что StringBuilder собрал все строчки фильтров
        String expectedText = "Выбрана база: Патенты\n" +
                "Фильтр по дате установлен: 2026-08-17\n" +
                "Выбран массив: [cis, ru_pat]\n" +
                "Классификатор: F02K9/00 установлен";
        assertEquals(expectedText, response.getText());

        // Проверяем наличие кнопок навигации
        validateNavigationButtons(response);
    }

    // Хелпер для проверки общей структуры клавиатуры и кнопок перехода
    private void validateNavigationButtons(BotResponse response) {
        assertNotNull(response.getAttachments());
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
