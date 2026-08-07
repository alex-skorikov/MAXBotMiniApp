package org.maxbot.miniapp.config;

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
        String chatId1 = event.getChatId();

        // Переносим всю логику на безопасный эластичный пул потоков
        return Mono.fromCallable(() -> {
                    StateMachine<BotStates, BotEvents> machine = factory.getStateMachine(machineId);

                    // 1. Очищаем старый ответ перед обработкой
                    machine.getExtendedState().getVariables().remove("response");

                    // 2. Блокирующе восстанавливаем стейт из Redis (безопасно внутри fromCallable)
                    persister.restore(machine, machineId).block();

                    log.info("==> [ДИСПЕТЧЕР] Чат: {}, Входящий ивент: {}, Текущий стейт из базы: {}",
                            chatId, event.getType(), machine.getState() != null ? machine.getState().getId() : "NULL");

                    // 3. Исправленный запуск машины: стартуем, только если состояние еще пустое
                    if (machine.getState() == null) {
                        machine.startReactively().block();
                    }

                    UserContext userContext = (UserContext) machine.getExtendedState()
                            .getVariables()
                            .get("userContext");

                    Message<BotEvents> message = MessageBuilder
                            .withPayload(event.getType())
                            .setHeader("event", event)
                            .setHeader("chatId", chatId)
                            .setHeader("userContext", userContext)
                            .build();

                    // 4. Синхронная отправка события (без бесконечных Flux-цепочек)
                    boolean accepted = machine.sendEvent(message);
                    log.info("==> [СТЕЙТ-МАШИНА] Результат обработки (синхронный): {}", accepted ? "ACCEPTED" : "DENIED");

                    BotResponse response = null;
                    if (accepted) {
                        response = (BotResponse) machine.getExtendedState().getVariables().get("response");
                    }

                    // 5. Сохраняем состояние в Redis
                    persister.persist(machine, userId, chatId1, event.getType());

                    // 6. Останавливаем реактивные стримы машины
                    machine.stopReactively().block();

                    return response;
                })
                .subscribeOn(Schedulers.boundedElastic()); // Изолируем выполнение от Netty EventLoop
    }
}
