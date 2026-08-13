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
import org.springframework.boot.test.mock.mockito.SpyBean;
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

    @SpyBean
    private ValidDateGuard validDateGuard;

    private StateMachine<BotStates, BotEvents> stateMachine;

    @BeforeEach
    void setUp() {
        this.stateMachine = stateMachineFactory.getStateMachine();
        this.stateMachine.startReactively().block();
    }

    @Test
    void testInitialStateAndBaseTransitions() {
        assertEquals(BotStates.INIT, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        assertEquals(BotStates.SELECT_BASE, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        assertEquals(BotStates.BASE_SELECTED, stateMachine.getState().getId());
    }

    @Test
    void testDateTransitionWithGuardSuccess() {
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        stateMachine.sendEvent(BotEvents.USER_INPUT_DATE);
        assertEquals(BotStates.FILTER_DATE, stateMachine.getState().getId());

        Mockito.doReturn(true).when(validDateGuard).evaluate(any());

        stateMachine.sendEvent(BotEvents.USER_SELECTED_DATE);

        assertEquals(BotStates.BASE_SELECTED, stateMachine.getState().getId());
    }

    @Test
    void testDateTransitionWithGuardDenied() {
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        stateMachine.sendEvent(BotEvents.USER_INPUT_DATE);
        assertEquals(BotStates.FILTER_DATE, stateMachine.getState().getId());

        // Симулируем невалидный ввод
        Mockito.doReturn(false).when(validDateGuard).evaluate(any());

        stateMachine.sendEvent(BotEvents.USER_SELECTED_DATE);

        // Машина отклонит переход дальше и останется в FILTER_DATE
        assertEquals(BotStates.FILTER_DATE, stateMachine.getState().getId());
    }

    @Test
    void testSearchAndPaginationTransitions() {
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);
        stateMachine.sendEvent(BotEvents.USER_PROCEED_TO_SEARCH);
        assertEquals(BotStates.SELECT_DATE, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.USER_SEARCH_PATENT);
        assertEquals(BotStates.SEARCH, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.USER_SEARCH_PATENT);
        assertEquals(BotStates.SEARCH, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.BACK_TO_START);
        assertEquals(BotStates.SELECT_BASE, stateMachine.getState().getId());
    }

    @Test
    void testBackButtons() {
        stateMachine.sendEvent(BotEvents.USER_OPEN_CHAT);
        stateMachine.sendEvent(BotEvents.USER_SELECT_BASE);

        stateMachine.sendEvent(BotEvents.USER_SEARCH_CLASSIFIERS);
        assertEquals(BotStates.FILTER_CLASSIFIERS, stateMachine.getState().getId());

        stateMachine.sendEvent(BotEvents.BACK);
        assertEquals(BotStates.BASE_SELECTED, stateMachine.getState().getId());
    }
}
