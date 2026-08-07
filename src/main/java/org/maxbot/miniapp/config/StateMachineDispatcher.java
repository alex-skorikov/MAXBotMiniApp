package org.maxbot.miniapp.config;

import org.maxbot.miniapp.controller.MaxWebhookController;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.StateMachineEventResult;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers; // НЕ ЗАБУДЬТЕ ИМПОРТ

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
        String chatId1 = event.getChatId();

        return Mono.defer(() -> {
            StateMachine<BotStates, BotEvents> machine = factory.getStateMachine(machineId);

            // Очищаем старый ответ перед отправкой нового события
            machine.getExtendedState().getVariables().remove("response");

            // Запускаем процесс восстановления на эластичном пуле потоков,
            // чтобы спрятать внутренний блокирующий .block() от Netty
            return persister.restore(machine, machineId)
                    .subscribeOn(Schedulers.boundedElastic())
                    .then(Mono.defer(() -> {
                        log.info("==> [ДИСПЕТЧЕР] Чат: {}, Входящий ивент: {}, Текущий стейт из базы: {}",
                                chatId, event.getType(), machine.getState() != null ? machine.getState().getId() : "NULL");
                        // Контекст уже гарантированно восстановлен персистером
                        UserContext userContext = (UserContext) machine.getExtendedState()
                                .getVariables()
                                .get("userContext");

//                        if (event.getType() == null) {
//                            log.warn("Получено неизвестное событие, которое привело к null-payload. Пропускаем обработку. {}", event);
//                            return Mono.empty(); // Или верните дефолтный ивент, например BotEvent.UNKNOWN
//                        }

                        Message<BotEvents> message = MessageBuilder
                                .withPayload(event.getType())
                                .setHeader("event", event)
                                .setHeader("chatId", chatId)
                                .setHeader("userContext", userContext)
                                .build();

                        return machine.startReactively()
                                .then(Mono.defer(() -> machine.sendEvent(Mono.just(message)).next()))
                                .flatMap(result -> {
                                    log.info("==> [СТЕЙТ-МАШИНА] Результат обработки: {}", result.getResultType());
                                    if (result.getResultType() == StateMachineEventResult.ResultType.ACCEPTED) {
                                        // Даем асинхронным Action завершиться (безопасное реактивное ожидание)
                                        return Mono.fromCallable(() -> (BotResponse) machine.getExtendedState().getVariables().get("response"));
                                    }
                                    return Mono.empty();
                                });
                    }))
                    // Сохраняем состояние при любом исходе обработки
                    .doOnTerminate(() -> persister.persist(machine, userId, chatId1, event.getType()))
                    .doOnError(error -> log.error("ОШИБКА В ДИСПЕТЧЕРЕ: ", error))
                    // Останавливаем реактивные стримы машины
                    .doFinally(signalType -> machine.stopReactively().subscribe());
        });
    }
}
