package org.maxbot.miniapp.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.ExtendedState;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StateMachineDispatcherTest {

    @Mock
    private StateMachineFactory<BotStates, BotEvents> factory;

    @Mock
    private StateMachinePersister persister;

    @Mock
    private StateMachine<BotStates, BotEvents> stateMachine;

    @Mock
    private ExtendedState extendedState;

    @Mock
    private StateMachineEventResult<BotStates, BotEvents> eventResult;

    private StateMachineDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        dispatcher = new StateMachineDispatcher(factory, persister);
    }

    @Test
    void shouldReturnEmptyMonoWhenEventTypeIsNull() {
        // Given
        int chatId = 123;
        BotEvent event = new BotEvent();
        event.setUserId("12345");
        event.setType(null); // Необрабатываемое системное событие

        // When
        Mono<BotResponse> result = dispatcher.dispatch(chatId, event);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Должен вернуть Mono.empty()

        verifyNoInteractions(factory, persister);
    }

    @Test
    void shouldDispatchEventSuccessfullyWhenTransitionIsAccepted() {
        // Given
        int chatId = 123;
        BotEvent event = new BotEvent();
        event.setUserId("12345");
        event.setChatId("999");
        event.setType(BotEvents.USER_OPEN_CHAT);

        UserContext mockUserContext = new UserContext();
        BotResponse expectedResponse = BotResponse.builder().text("Привет!").build();

        // Мокаем extendedState и переменные через Mockito вместо реальной HashMap,
        // чтобы обойти физическое удаление remove("response") на первом шаге
        when(stateMachine.getExtendedState()).thenReturn(extendedState);

        // Настраиваем безопасное поведение мапы переменных
        Map<Object, Object> mockVariables = mock(Map.class);
        when(extendedState.getVariables()).thenReturn(mockVariables);

        // Переносим возврат контекста пользователя и ответа бота на мок
        when(mockVariables.get("userContext")).thenReturn(mockUserContext);
        when(mockVariables.get("response")).thenReturn(expectedResponse);

        // Мокаем реактивное поведение
        when(factory.getStateMachine("12345")).thenReturn(stateMachine);
        when(persister.restore(eq(stateMachine), eq("12345"))).thenReturn(Mono.empty());
        when(stateMachine.startReactively()).thenReturn(Mono.empty());
        when(stateMachine.stopReactively()).thenReturn(Mono.empty());

        // Мокаем результат отправки ивента (ACCEPTED)
        when(eventResult.getResultType()).thenReturn(StateMachineEventResult.ResultType.ACCEPTED);
        when(stateMachine.sendEvent(any(Mono.class))).thenReturn(Flux.just(eventResult));

        // When
        Mono<BotResponse> result = dispatcher.dispatch(chatId, event);

        // Then
        StepVerifier.create(result)
                .expectNext(expectedResponse)
                .verifyComplete();

        // Верифицируем, что удаление и извлечение вызывались по контракту диспетчера
        verify(mockVariables, times(1)).remove("response");
        verify(stateMachine, times(1)).startReactively();

        // Верифицируем сохранение контекста в БД после успешного перехода
        verify(persister, times(1)).persist(stateMachine, "12345", "999", BotEvents.USER_OPEN_CHAT);
    }

    @Test
    void shouldReturnEmptyMonoWhenTransitionIsDenied() {
        // Given
        int chatId = 123;
        BotEvent event = new BotEvent();
        event.setUserId("12345");
        event.setType(BotEvents.USER_OPEN_CHAT);

        Map<Object, Object> variables = new HashMap<>();
        variables.put("userContext", new UserContext());
        when(stateMachine.startReactively()).thenReturn(Mono.empty());
        when(factory.getStateMachine("12345")).thenReturn(stateMachine);
        when(stateMachine.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(variables);

        Mockito.lenient().when(persister.restore(any(), any())).thenReturn(Mono.empty());

        when(stateMachine.stopReactively()).thenReturn(Mono.empty());

        // Симулируем отклонение ивента стейт-машиной (DENIED)
        when(eventResult.getResultType()).thenReturn(StateMachineEventResult.ResultType.DENIED);
        when(stateMachine.sendEvent(any(Mono.class))).thenReturn(Flux.just(eventResult));

        // When
        Mono<BotResponse> result = dispatcher.dispatch(chatId, event);

        // Then
        StepVerifier.create(result)
                .verifyComplete(); // Результат не ACCEPTED -> возвращается Mono.empty()

        // Персист все равно должен вызваться по doOnSuccess
        verify(persister, times(1)).persist(eq(stateMachine), any(), any(), any());
    }

}
