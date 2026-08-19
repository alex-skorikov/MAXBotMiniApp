package org.maxbot.miniapp.handlers; // Укажите ваш точный пакет для хендлеров

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InitHandlerTest {

    private final InitHandler initHandler = new InitHandler();

    @Test
    void shouldReturnWelcomeResponseWithBaseSelectionButtons() {
        // Given
        UserContext ctx = new UserContext();
        BotEvent event = new BotEvent();

        // When
        BotResponse response = initHandler.handle(ctx, event);

        // Then
        assertNotNull(response);
        assertFalse(response.isNotify(), "Флаг notify должен быть равен false");

        // Проверяем текст приветствия
        String expectedText = """
                Добро пожаловать!
                Это помошник по поиску патентов
                и других объектов интеллектуальной собственности.
                Выберите базу для поиска:
                """;
        assertEquals(expectedText, response.getText());

        // Проверяем наличие инлайн-клавиатуры в аттачментах
        assertNotNull(response.getAttachments());
        assertEquals(1, response.getAttachments().size());

        BotResponse.Attachment attachment = response.getAttachments().get(0);
        assertEquals("inline_keyboard", attachment.getType());

        // Проверяем кнопки
        assertNotNull(attachment.getPayload());
        List<List<BotResponse.Button>> buttons = attachment.getPayload().getButtons();
        assertNotNull(buttons);
        assertFalse(buttons.isEmpty());

        // Проверяем, что утилита сгенерировала нужные кнопки (Патенты, Промобразцы, Полезные модели)
        // В зависимости от реализации BotAnswerUtil.getButtons кнопки могут быть
        // в одном ряду или в разных. Проверим, что тексты/payload присутствуют:
        boolean hasPatents = false;
        boolean hasPromSample = false;
        boolean hasModels = false;

        for (List<BotResponse.Button> row : buttons) {
            for (BotResponse.Button button : row) {
                // Если у вас в Button используются другие геттеры (например, getText(), getPayload() или getCallbackData()),
                // скорректируйте их под вашу DTO-модель кнопки.
                if ("Патенты".equals(button.getText())) hasPatents = true;
                if ("Промобразцы".equals(button.getText())) hasPromSample = true;
                if ("Полезные модели".equals(button.getText())) hasModels = true;
            }
        }

        assertTrue(hasPatents, "Должна быть кнопка 'Патенты'");
        assertTrue(hasPromSample, "Должна быть кнопка 'Промобразцы'");
        assertTrue(hasModels, "Должна быть кнопка 'Полезные модели'");
    }
}
