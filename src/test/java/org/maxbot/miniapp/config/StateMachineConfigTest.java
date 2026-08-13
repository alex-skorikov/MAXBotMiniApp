package org.maxbot.miniapp.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.StateMachineConfig;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.maxbot.miniapp.util.StepAction;
import org.maxbot.miniapp.util.ValidDateGuard;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;

@SpringBootTest(classes = {StateMachineConfig.class})
class StateMachineConfigTest {

    @Autowired
    private StateMachineFactory<BotStates, BotEvents> stateMachineFactory;

    @MockBean
    private StepAction stepAction;

    @MockBean
    private ValidDateGuard validDateGuard;

    private StateMachine<BotStates, BotEvents> stateMachine;

    @BeforeEach
    void setUp() {
        this.stateMachine = stateMachineFactory.getStateMachine();
        this.stateMachine.startReactively().block();
    }

    @Test
    void testInitialStateAndBaseTransitions() {
        // Проверяем начальное состояние
        assertEquals(BotStates.INIT, stateMachine.getState().getId());

        // INIT -> SELECT_BASE по ивенту USER_OPEN_CHAT
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        assertEquals(BotStates.SELECT_BASE, stateMachine.getState().getId());

        // SELECT_BASE -> BASE_SELECTED по ивенту USER_SELECT_BASE
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        assertEquals(BotStates.BASE_SELECTED, stateMachine.getState().getId());
    }

    @Test
    void testDateTransitionWithGuardSuccess() {
        // Переводим машину в состояние BASE_SELECTED
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);

        // Переходим в меню ввода даты
        stateMachine.sendEvent(BotEvents.USER_INPUT_DATE);
        assertEquals(BotStates.FILTER_DATE, stateMachine.getState().getId());

        // Симулируем, что Guard разрешает переход (дата валидна)
        Mockito.when(validDateGuard.evaluate(any())).thenReturn(true);

        // Отправляем ивент завершения выбора даты
        stateMachine.sendEvent(BotEvents.USER_SELECTED_DATE);

        // Переход должен успешно выполниться обратно в BASE_SELECTED
        assertEquals(BotStates.BASE_SELECTED, stateMachine.getState().getId());
    }

    @Test
    void testDateTransitionWithGuardDenied() {
        // Переводим машину в состояние FILTER_DATE
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        stateMachine.sendEvent(BotEvents.USER_INPUT_DATE);

        // Симулируем, что Guard ЗАПРЕЩАЕТ переход (например, неверный формат даты)
        Mockito.when(validDateGuard.evaluate(any())).thenReturn(false);

        // Отправляем ивент
        stateMachine.sendEvent(BotEvents.USER_SELECTED_DATE);

        // Машина должна ОТКЛОНИТЬ переход и остаться в FILTER_DATE
        assertEquals(BotStates.FILTER_DATE, stateMachine.getState().getId());
    }

    @Test
    void testSearchAndPaginationTransitions() {
        // Доходим до стейта SELECT_DATE (ожидание ввода строки поиска)
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        stateMachine.sendEvent(BotEvents.USER_PROCEED_TO_SEARCH);
        assertEquals(BotStates.SELECT_DATE, stateMachine.getState().getId());

        // Отправляем поисковый запрос -> переходим в SEARCH
        stateMachine.sendEvent(BotEvents.USER_SEARCH_PATENT);
        assertEquals(BotStates.SEARCH, stateMachine.getState().getId());

        // Циклический переход (пагинация) -> стейт должен остаться SEARCH
        stateMachine.sendEvent(BotEvents.USER_SEARCH_PATENT);
        assertEquals(BotStates.SEARCH, stateMachine.getState().getId());

        // Сброс кнопкой "Назад в начало"
        stateMachine.sendEvent(BotEvents.BACK_TO_START);
        assertEquals(BotStates.SELECT_BASE, stateMachine.getState().getId());
    }

    @Test
    void testBackButtons() {
        // Проверка кнопки "Назад" из подменю классификаторов
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);

        stateMachine.sendEvent(BotEvents.USER_SEARCH_CLASSIFIERS);
        assertEquals(BotStates.FILTER_CLASSIFIERS, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.BACK);
        assertEquals(BotStates.BASE_SELECTED, stateMachine.getState().getId());
    }
}
