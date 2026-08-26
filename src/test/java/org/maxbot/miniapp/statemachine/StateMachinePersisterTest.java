package org.maxbot.miniapp.statemachine;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.HashMapContextRepository;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.access.StateMachineAccess;
import org.springframework.statemachine.access.StateMachineAccessor;
import org.springframework.statemachine.state.State;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

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
    private org.springframework.statemachine.ExtendedState extendedState;

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

    @Test
    void shouldPersistSuccessfullyWhenContextExistsInStateMachine() {
        // Given: Контекст лежит в памяти стейт-машины
        String userId = "111";
        String chatId = "222";
        BotEvents botEvent = BotEvents.USER_OPEN_CHAT;

        UserContext userContext = new UserContext();
        userContext.setUserId(111);

        // Настраиваем моки для извлечения контекста из extendedState
        when(stateMachine.getState()).thenReturn(state);
        when(state.getId()).thenReturn(BotStates.BASE_SELECTED);
        when(stateMachine.getExtendedState()).thenReturn(extendedState);

        Map<Object, Object> variables = new HashMap<>();
        variables.put("userContext", userContext);
        when(extendedState.getVariables()).thenReturn(variables);

        // When
        persister.persist(stateMachine, userId, chatId, botEvent);

        // Then: Проверяем, что данные синхронизировались и ушли в репозиторий
        UserContext savedContext = contextRepository.load(userId);
        assertNotNull(savedContext);
        assertEquals(BotStates.BASE_SELECTED, savedContext.getState());
        assertEquals(botEvent, savedContext.getBotEvent());
        assertEquals(chatId, savedContext.getChatId());
    }

    @Test
    void shouldPersistWithDbFallbackWhenContextMissingInStateMachine() {
        // Given: В памяти стейт-машины контекста нет, но он есть в БД (fallback)
        String userId = "333";
        String chatId = "444";

        UserContext dbContext = new UserContext();
        dbContext.setUserId(333);
        contextRepository.save(dbContext);

        when(stateMachine.getState()).thenReturn(state);
        when(state.getId()).thenReturn(BotStates.INIT);
        when(stateMachine.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>()); // Пустые переменные

        // When
        persister.persist(stateMachine, userId, chatId, BotEvents.USER_OPEN_CHAT);

        // Then: Убеждаемся, что подтянулся контекст из БД и обновился
        UserContext savedContext = contextRepository.load(userId);
        assertNotNull(savedContext);
        assertEquals(BotStates.INIT, savedContext.getState());
        assertEquals("444", savedContext.getChatId());
    }

    @Test
    void shouldCreateNewContextOnFirstStartAndParseUserId() {
        // Given: Первая сессия, контекста нет нигде
        String userId = "555";
        String chatId = "666";

        when(stateMachine.getState()).thenReturn(state);
        when(state.getId()).thenReturn(BotStates.INIT);
        when(stateMachine.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());

        // When
        persister.persist(stateMachine, userId, chatId, BotEvents.USER_OPEN_CHAT);

        // Then: Проверяем создание нового инстанса и парсинг ID
        UserContext savedContext = contextRepository.load(userId);
        assertNotNull(savedContext);
        assertEquals(555, savedContext.getUserId());
        assertEquals("666", savedContext.getChatId());
    }

    @Test
    void shouldHandleNumberFormatExceptionWhenUserIdIsInvalid() {
        // Given: Создаем мок репозитория СПЕЦИАЛЬНО для этого теста,
        // чтобы обойти жесткий Integer.parseInt внутри HashMapContextRepository
        org.maxbot.miniapp.repository.ContextRepository mockRepository =
                Mockito.mock(org.maxbot.miniapp.repository.ContextRepository.class);

        // Создаем отдельный экземпляр персистера с мок-репозиторием
        StateMachinePersister persisterWithMockRepo = new StateMachinePersister(mockRepository);

        String invalidUserId = "abc";
        String chatId = "777";

        // Настраиваем мок-репозиторий: на любой запрос возвращаем null (первый старт)
        when(mockRepository.load(any())).thenReturn(null);

        // Настраиваем стейт-машину
        when(stateMachine.getState()).thenReturn(state);
        when(state.getId()).thenReturn(BotStates.INIT);
        when(stateMachine.getExtendedState()).thenReturn(extendedState);
        when(extendedState.getVariables()).thenReturn(new HashMap<>());

        // When: Вызываем персистер. Теперь load() вернет null, и код зайдет в try-catch персистера!
        persisterWithMockRepo.persist(stateMachine, invalidUserId, chatId, BotEvents.USER_OPEN_CHAT);

        // Then: Верифицируем, что персистер попытался спасти ситуацию и сохранить новый UserContext с userId = 0
        ArgumentCaptor<UserContext> contextCaptor = ArgumentCaptor.forClass(UserContext.class);
        verify(mockRepository, times(1)).save(contextCaptor.capture());

        UserContext savedContext = contextCaptor.getValue();
        assertNotNull(savedContext);
        assertEquals(0, savedContext.getUserId(), "UserId должен остаться 0 из-за NumberFormatException");
        assertEquals("777", savedContext.getChatId());
    }

}
