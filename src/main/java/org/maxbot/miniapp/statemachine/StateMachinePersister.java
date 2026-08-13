package org.maxbot.miniapp.statemachine;

import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineContext;
import org.springframework.statemachine.support.DefaultExtendedState;
import org.springframework.statemachine.support.DefaultStateMachineContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@Component
public class StateMachinePersister {

    private static final Logger log = LoggerFactory.getLogger(StateMachinePersister.class);
    private final ContextRepository contextRepository;

    public StateMachinePersister(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    public Mono<Void> restore(StateMachine<BotStates, BotEvents> stateMachine, String chatId) {
        return Mono.defer(() -> {
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

            // 3. Выполняем РЕАКТИВНЫЙ сброс регионов вместо doWithAllRegions и resetStateMachine
            // Это предотвращает появление бесконечного цикла блокировок внутри Project Reactor
            return Flux.fromIterable(stateMachine.getStateMachineAccessor().withAllRegions())
                    .flatMap(accessor -> accessor.resetStateMachineReactively(context))
                    .then();
        });
    }

    public void persist(StateMachine<BotStates, BotEvents> stateMachine,
                        String userId,
                        String chatId,
                        BotEvents botEvent) {
        if (stateMachine.getState() == null) {
            return;
        }

        UserContext userContext = contextRepository.load(chatId);

        // Если в Redis контекста ещё нет (самый первый старт), тогда создаем новый
        if (userContext == null) {
            userContext = new UserContext();
            try {
                if (userId != null) userContext.setUserId(Integer.parseInt(userId));
            } catch (NumberFormatException e) {
                log.warn("Не удалось распарсить userId: {}", userId);
            }
            userContext.setChatId(chatId);
        }

        // 2. Синхронизируем текущий стейт машины с полем внутри актуального UserContext
        BotStates currentState = stateMachine.getState().getId();
        userContext.setState(currentState);
        userContext.setBotEvent(botEvent);
        userContext.setChatId(chatId);

        // 3. Сохраняем обновленный контекст обратно в Redis
        contextRepository.save(userContext);
        log.info("💾 Стейт [{}] успешно синхронизирован и сохранен в Redis для чата {}", currentState, chatId);
    }

}
