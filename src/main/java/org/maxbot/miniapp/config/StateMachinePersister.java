package org.maxbot.miniapp.config;

import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class StateMachinePersister {

    private final ContextRepository contextRepository;

    public StateMachinePersister(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    /**
     * Восстанавливает состояние автомата из Redis
     */
    public Mono<Void> restore(StateMachine<BotStates, BotEvents> stateMachine, String chatId) {
        return Mono.fromRunnable(() -> {
            // 1. Загружаем контекст из Redis
            UserContext userContext = contextRepository.load(chatId);
            BotStates savedState = userContext.getState() != null ? userContext.getState() : BotStates.INIT;

            // 2. Формируем переменные для StateMachine
            Map<Object, Object> variables = new HashMap<>();
            variables.put("userContext", userContext);
            DefaultExtendedState extendedState = new DefaultExtendedState(variables);

            StateMachineContext<BotStates, BotEvents> context =
                    new DefaultStateMachineContext<>(
                            savedState,
                            null,
                            null,
                            extendedState
                    );

            // 3. Выполняем сброс машины (который блокирует поток под капотом)
            stateMachine.getStateMachineAccessor()
                    .doWithAllRegions(accessor -> accessor.resetStateMachine(context));
        });
    }

    /**
     * Сохраняет текущее состояние и переменные в Redis
     */
    public void persist(StateMachine<BotStates, BotEvents> stateMachine,
                        String userId,
                        String chatId,
                        BotEvents botEvent) {
        if (stateMachine.getState() == null) {
            return; // Защита от сохранения неинициализированной машины
        }

        // 1. Извлекаем текущий UserContext из ExtendedState работающей машины
        UserContext userContext = (UserContext) stateMachine.getExtendedState()
                .getVariables()
                .get("userContext");

        // Если контекста почему-то нет в машине, создаем аварийный объект
        if (userContext == null) {
            userContext = new UserContext();
            userContext.setUserId(Integer.parseInt(userId));
            userContext.setChatId(chatId);
        }

        // 2. ОБЯЗАТЕЛЬНО: Синхронизируем текущий стейт машины с полем внутри UserContext
        BotStates currentState = stateMachine.getState().getId();
        userContext.setState(currentState);
        userContext.setBotEvent(botEvent);
        userContext.setChatId(chatId);
        // 3. Сохраняем обновленный контекст в Redis
        contextRepository.save(userContext);
    }
}
