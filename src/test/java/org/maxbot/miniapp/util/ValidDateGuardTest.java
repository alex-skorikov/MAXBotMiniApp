package org.maxbot.miniapp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.mockito.Mockito;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ValidDateGuardTest {

    private ValidDateGuard validDateGuard;
    private StateContext<BotStates, BotEvents> context;

    @BeforeEach
    void setUp() {
        this.validDateGuard = new ValidDateGuard();
        this.context = Mockito.mock(StateContext.class);
    }

    @Test
    void evaluateValidDateReturnsTrue() {
        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getText()).thenReturn("13.08.2026");
        Mockito.when(context.getMessageHeader("event")).thenReturn(mockEvent);

        assertTrue(validDateGuard.evaluate(context));
    }

    @Test
    void evaluateInvalidDateReturnsFalse() {
        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getText()).thenReturn("13-08-2026"); // Неверный формат
        Mockito.when(context.getMessageHeader("event")).thenReturn(mockEvent);

        assertTrue(validDateGuard.evaluate(context));
    }

    @Test
    void evaluateNullEventOrTextReturnsFalse() {
        // Кейс 1: Ивента вообще нет в заголовках
        Mockito.when(context.getMessageHeader("event")).thenReturn(null);
        assertFalse(validDateGuard.evaluate(context));

        // Кейс 2: Ивент есть, но текст внутри равен null
        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getText()).thenReturn(null);
        Mockito.when(context.getMessageHeader("event")).thenReturn(mockEvent);
        assertFalse(validDateGuard.evaluate(context));
    }

    @Test
    void negateMethodEvaluatesInverseLogicCorrectly() {
        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(context.getMessageHeader("event")).thenReturn(mockEvent);

        Guard<BotStates, BotEvents> negatedGuard = validDateGuard.negate();

        // Кейс 1: Для валидной даты инверсия должна вернуть false
        Mockito.when(mockEvent.getText()).thenReturn("01.01.2020");
        assertFalse(negatedGuard.evaluate(context));

        // Кейс 2: Для НЕВАЛИДНОЙ даты инверсия должна вернуть true (пропустить ошибку)
        Mockito.when(mockEvent.getText()).thenReturn("строка текста");
        assertTrue(negatedGuard.evaluate(context));

    }
}