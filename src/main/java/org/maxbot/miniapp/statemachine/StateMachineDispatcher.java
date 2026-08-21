package org.maxbot.miniapp.statemachine;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class StateMachineDispatcher {

    private final StateMachineFactory<BotStates, BotEvents> factory;
    private final StateMachinePersister persister;
    private static final Logger log = LoggerFactory.getLogger(StateMachineDispatcher.class);

    public StateMachineDispatcher(StateMachineFactory<BotStates, BotEvents> factory,
                                  StateMachinePersister persister) {
        this.factory = factory;
        this.persister = persister;
    }

    public Mono<BotResponse> dispatch(int chatId, BotEvent event) {
        String machineId = String.valueOf(chatId);
        String userId = event.getUserId();
        String eventChatId = event.getChatId();

        if (event.getType() == null) {
            log.info("⚠️ [ДИСПЕТЧЕР] Получено необрабатываемое системное событие (null). Пропускаем.");
            return Mono.empty();
        }

        return Mono.defer(() -> {
            StateMachine<BotStates, BotEvents> machine = factory.getStateMachine(machineId);

            // 1. Очищаем старый ответ
            machine.getExtendedState().getVariables().remove("response");

            // 2. Восстанавливаем стейт из Redis
            return persister.restore(machine, machineId)
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(Mono.defer(() -> {
                        log.info("==> [ДИСПЕТЧЕР] Чат: {}, Входящий ивент: {}, Текущий стейт из базы: {}",
                                chatId, event.getType(), machine.getState() != null ? machine.getState().getId() : "NULL");

                        Mono<Void> ensureStarted = Mono.defer(machine::startReactively);

                        UserContext userContext = (UserContext) machine.getExtendedState()
                                .getVariables()
                                .get("userContext");

                        Message<BotEvents> message = MessageBuilder
                                .withPayload(event.getType())
                                .setHeader("event", event)
                                .setHeader("chatId", chatId)
                                .setHeader("userContext", userContext)
                                .build();

                        // 4. Запускаем стрим, отправляем событие и дожидаемся обработки
                        return ensureStarted
                                .then(Mono.defer(() -> machine.sendEvent(Mono.just(message))
                                        .take(1)
                                        .singleOrEmpty()
                                ))
                                .flatMap(result -> {
                                    log.info("==> [СТЕЙТ-МАШИНА] Результат обработки: {}", result.getResultType());

                                    if (result.getResultType() == StateMachineEventResult.ResultType.ACCEPTED) {
                                        // 5. Вытаскиваем сгенерированный хендлером ответ
                                        BotResponse response = (BotResponse) machine.getExtendedState().getVariables().get("response");
                                        log.info("==> [ДИСПЕТЧЕР] Ответ успешно извлечен: {}", response != null ? "ДА" : "НЕТ (NULL)");
                                        return Mono.justOrEmpty(response);
                                    }
                                    return Mono.empty();
                                })
                                // 6. Сохраняем стейт в Redis только после успешного завершения транзишена
                                .doOnSuccess(res -> persister.persist(machine, userId, eventChatId, event.getType()))
                                .doFinally(signalType -> machine.stopReactively().subscribeOn(Schedulers.boundedElastic()).subscribe());
                    }));
        });
    }
}
