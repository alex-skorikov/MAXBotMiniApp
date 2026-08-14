package org.maxbot.miniapp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.HandlerRegistry;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.handlers.StepHandler;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.mockito.Mockito;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.state.State;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;

class StepActionTest {

    private HandlerRegistry handlerRegistry;
    private StepAction stepAction;
    private StateContext<BotStates, BotEvents> context;
    private State<BotStates, BotEvents> targetState;
    private ExtendedState extendedState;
    private Map<Object, Object> variables;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        this.handlerRegistry = Mockito.mock(HandlerRegistry.class);
        this.stepAction = new StepAction(handlerRegistry);

        // Мокаем иерархию контекста Spring State Machine
        this.context = Mockito.mock(StateContext.class);
        this.targetState = Mockito.mock(State.class);
        this.extendedState = Mockito.mock(ExtendedState.class);
        this.variables = new HashMap<>();

        Mockito.when(context.getTarget()).thenReturn(targetState);
        Mockito.when(context.getExtendedState()).thenReturn(extendedState);
        Mockito.when(extendedState.getVariables()).thenReturn(variables);
    }

    @Test
    void executeSuccessStoresResponseInVariables() {
        BotStates state = BotStates.BASE_SELECTED;
        Mockito.when(targetState.getId()).thenReturn(state);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        UserContext mockUserContext = Mockito.mock(UserContext.class);
        Mockito.when(context.getMessageHeader("event")).thenReturn(mockEvent);
        Mockito.when(context.getMessageHeader("userContext")).thenReturn(mockUserContext);

        StepHandler mockHandler = Mockito.mock(StepHandler.class);
        BotResponse mockResponse = BotResponse.builder().text("Успешный ответ").build();

        Mockito.when(handlerRegistry.getHandler(state)).thenReturn(mockHandler);
        Mockito.when(mockHandler.handle(mockUserContext, mockEvent)).thenReturn(mockResponse);

        stepAction.execute(context);

        assertEquals(mockResponse, variables.get("response"));
        Mockito.verify(extendedState, Mockito.times(1)).getVariables();
    }

    @Test
    void executeReturnsEarlyWhenTargetStateIsNull() {
        Mockito.when(context.getTarget()).thenReturn(null);

        stepAction.execute(context);

        assertTrue(variables.isEmpty());
        Mockito.verify(handlerRegistry, Mockito.never()).getHandler(any());
    }

    @Test
    void executeReturnsEarlyWhenHandlerNotFound() {
        BotStates state = BotStates.FILTER_DATE;
        Mockito.when(targetState.getId()).thenReturn(state);
        Mockito.when(handlerRegistry.getHandler(state)).thenReturn(null);

        stepAction.execute(context);

        assertTrue(variables.isEmpty());
    }

    @Test
    void executeDoesNotStoreVariableWhenResponseIsNull() {
        BotStates state = BotStates.SEARCH;
        Mockito.when(targetState.getId()).thenReturn(state);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        UserContext mockUserContext = Mockito.mock(UserContext.class);
        Mockito.when(context.getMessageHeader("event")).thenReturn(mockEvent);
        Mockito.when(context.getMessageHeader("userContext")).thenReturn(mockUserContext);

        StepHandler mockHandler = Mockito.mock(StepHandler.class);
        Mockito.when(handlerRegistry.getHandler(state)).thenReturn(mockHandler);
        Mockito.when(mockHandler.handle(mockUserContext, mockEvent)).thenReturn(null);

        stepAction.execute(context);

        assertTrue(variables.isEmpty());
    }
}
