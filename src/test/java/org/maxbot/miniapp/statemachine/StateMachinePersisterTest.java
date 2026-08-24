package org.maxbot.miniapp.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.HashMapContextRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.access.StateMachineAccessor;
import org.springframework.statemachine.state.State;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateMachinePersisterTest {

    private HashMapContextRepository contextRepository;
    private StateMachinePersister persister;

    @Mock
    private StateMachine<BotStates, BotEvents> stateMachine;

    @Mock
    private StateMachineAccessor<BotStates, BotEvents> stateMachineAccessor;

    @Mock
    private StateMachineAccess<BotStates, BotEvents> stateMachineAccess;

    @Mock
    private State<BotStates, BotEvents> state;

    @BeforeEach
    void setUp() {
        contextRepository = new HashMapContextRepository();
        contextRepository.clear();
        persister = new StateMachinePersister(contextRepository);
    }

    @Test
    void shouldRestoreContextFromRepositorySuccessfully() {
        // Given
        String chatId = "12345";
        UserContext existingContext = new UserContext();
        existingContext.setUserId(12345);
        existingContext.setState(BotStates.BASE_SELECTED);
        contextRepository.save(existingContext);

        // Мокаем цепочку вызовов: stateMachine -> Accessor -> Regions -> Access -> reset
        when(stateMachine.getStateMachineAccessor()).thenReturn(stateMachineAccessor);
        when(stateMachineAccessor.withAllRegions()).thenReturn(Collections.singletonList(stateMachineAccess));
        when(stateMachineAccess.resetStateMachineReactively(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = persister.restore(stateMachine, chatId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        // Проверяем, что в метод сброса передался правильный контекст машины
        ArgumentCaptor<StateMachineContext<BotStates, BotEvents>> contextCaptor = ArgumentCaptor.forClass(StateMachineContext.class);
        verify(stateMachineAccess, times(1)).resetStateMachineReactively(contextCaptor.capture());

        StateMachineContext<BotStates, BotEvents> capturedContext = contextCaptor.getValue();
        assertEquals(BotStates.BASE_SELECTED, capturedContext.getState());
        assertNotNull(capturedContext.getExtendedState().getVariables().get("userContext"));
    }

    @Test
    void shouldRestoreWithInitStateWhenSavedStateIsNull() {
        // Given
        String chatId = "12345"; // Репозиторий создаст дефолтный контекст, где state == null

        when(stateMachine.getStateMachineAccessor()).thenReturn(stateMachineAccessor);
        when(stateMachineAccessor.withAllRegions()).thenReturn(Collections.singletonList(stateMachineAccess));
        when(stateMachineAccess.resetStateMachineReactively(any())).thenReturn(Mono.empty());

        // When
        Mono<Void> result = persister.restore(stateMachine, chatId);

        // Then
        StepVerifier.create(result)
                .verifyComplete();

        ArgumentCaptor<StateMachineContext<BotStates, BotEvents>> contextCaptor = ArgumentCaptor.forClass(StateMachineContext.class);
        verify(stateMachineAccess).resetStateMachineReactively(contextCaptor.capture());

        // Проверяем fallback к BotStates.INIT
        assertEquals(BotStates.INIT, contextCaptor.getValue().getState());
    }

    @Test
    void shouldNotPersistWhenStateMachineStateIsNull() {
        // Given
        when(stateMachine.getState()).thenReturn(null);

        // When
        persister.persist(stateMachine, "111", "222", BotEvents.USER_OPEN_CHAT);

        // Then
        // Проверяем побочный эффект: репозиторий пуст, сохранения не было
        assertNull(contextRepository.storageGetDirectly("222"));
    }

}
