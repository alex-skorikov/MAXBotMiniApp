package org.maxbot.miniapp.core;

import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserContextTest {

    @Test
    void testUserContextPojoMethods() {
        // 1. Создаем первый объект и наполняем его данными через сеттеры/билдер
        UserContext ctx1 = new UserContext();
        ctx1.setUserId(123);
        ctx1.setChatId("456");
        ctx1.setSelectedBase("База");
        ctx1.setDate("2026-08-26");
        ctx1.setDatasetName("Массив");
        ctx1.setDatasetArrays(List.of("RU"));
        ctx1.setClassifiers("A61K");
        ctx1.setState(BotStates.INIT);
        ctx1.setBotEvent(BotEvents.USER_OPEN_CHAT);
        ctx1.setSearchQuery("Тест");
        ctx1.setOffset(0);
        ctx1.setLimit(5);
        ctx1.setHits(List.of(new PatentHit()));

        // 2. Создаем идентичный второй объект для проверки успешного сравнения
        UserContext ctx2 = new UserContext();
        ctx2.setUserId(123);
        ctx2.setChatId("456");
        ctx2.setSelectedBase("База");
        ctx2.setDate("2026-08-26");
        ctx2.setDatasetName("Массив");
        ctx2.setDatasetArrays(List.of("RU"));
        ctx2.setClassifiers("A61K");
        ctx2.setState(BotStates.INIT);
        ctx2.setBotEvent(BotEvents.USER_OPEN_CHAT);
        ctx2.setSearchQuery("Тест");
        ctx2.setOffset(0);
        ctx2.setLimit(5);
        ctx2.setHits(ctx1.getHits());

        // 3. Создаем третий объект, который отличается, для проверки ветки непересечения в equals
        UserContext ctx3 = new UserContext();
        ctx3.setUserId(999); // Другой ID

        // --- Тестируем equals() (Покрывает Missed Branches) ---
        // Сравнение с самим собой
        assertEquals(ctx1, ctx1);

        // Сравнение с идентичным объектом
        assertEquals(ctx1, ctx2);

        // Сравнение с отличающимся объектом
        assertNotEquals(ctx1, ctx3);

        // Сравнение с null
        assertNotEquals(null, ctx1);

        // Сравнение с объектом другого класса
        assertNotEquals("простая строка", ctx1);

        // --- Тестируем hashCode() ---
        assertEquals(ctx1.hashCode(), ctx2.hashCode(), "Хэш-коды идентичных объектов должны совпадать");
        assertNotEquals(ctx1.hashCode(), ctx3.hashCode(), "Хэш-коды разных объектов не должны совпадать");

        // --- Тестируем toString() ---
        String toStringResult = ctx1.toString();
        assertNotNull(toStringResult);
        assertTrue(toStringResult.contains("123"));
        assertTrue(toStringResult.contains("456"));
    }

    @Test
    void testBuilderIfPresent() {
        // Если у вас используется Lombok @Builder, этот тест покроет и его скрытые методы
        UserContext ctx = UserContext.builder()
                .userId(777)
                .chatId("888")
                .build();
        String string = ctx.toString();

        assertNotNull(ctx);
        assertEquals(777, ctx.getUserId());
        assertEquals("888", ctx.getChatId());

        assertTrue(string.contains("777"));
        assertTrue(string.contains("888"));
    }
}
