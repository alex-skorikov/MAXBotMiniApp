package org.maxbot.miniapp.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.statemachine.state.State;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class StateMachineDispatcherTest {

    @Mock
    private StateMachineFactory<BotStates, BotEvents> factory;

    @Mock
    private StateMachinePersister persister;

    @Mock
    private StateMachine<BotStates, BotEvents> machine;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private State<BotStates, BotEvents> state;

    @Mock
    private StateMachineEventResult<BotStates, BotEvents> eventResult;

    @InjectMocks
    private StateMachineDispatcher dispatcher;

    private Map<Object, Object> variables;

    @BeforeEach
    void setUp() {
        variables = new HashMap<>();
    }

    @Test
    void shouldReturnEmptyWhenEventTypeIsNull() {
        int chatId = 123;
        BotEvent event = new BotEvent(); // По умолчанию type == null

        // When
        Mono<BotResponse> result = dispatcher.dispatch(chatId, event);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        verifyNoInteractions(factory, persister);
    }

}
