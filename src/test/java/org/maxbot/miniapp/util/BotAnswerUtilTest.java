package org.maxbot.miniapp.util;

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.bot.BotResponse;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotAnswerUtilTest {

    @Test
    void getButtonsTransformsMapToButtonRowsCorrectly() {
        Map<String, String> objectMap = new LinkedHashMap<>();
        objectMap.put("Кнопка 1", "PAYLOAD_1");
        objectMap.put("Кнопка 2", "PAYLOAD_2");

        List<List<BotResponse.Button>> result = BotAnswerUtil.getButtons(objectMap);

        assertNotNull(result);
        assertEquals(2, result.size()); // Ожидаем 2 ряда кнопок

        List<BotResponse.Button> row1 = result.get(0);
        assertEquals(1, row1.size());
        BotResponse.Button button1 = row1.get(0);
        assertEquals("callback", button1.getType());
        assertEquals("Кнопка 1", button1.getText());
        assertEquals("PAYLOAD_1", button1.getPayload());

        List<BotResponse.Button> row2 = result.get(1);
        assertEquals(1, row2.size());
        BotResponse.Button button2 = row2.get(0);
        assertEquals("callback", button2.getType());
        assertEquals("Кнопка 2", button2.getText());
        assertEquals("PAYLOAD_2", button2.getPayload());

    }

    @Test
    void getButtonsReturnsEmptyListWhenMapIsEmpty() {
        Map<String, String> emptyMap = Collections.emptyMap();

        List<List<BotResponse.Button>> result = BotAnswerUtil.getButtons(emptyMap);

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }
}