package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.statemachine.StateMachineDispatcher;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.util.MaxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class MaxWebhookController {

    private final MaxMapper maxMapper;
    private final StateMachineDispatcher dispatcher;
    private static final Logger log = LoggerFactory.getLogger(MaxWebhookController.class);
    private final MaxApiClient maxApiClient;


    public MaxWebhookController(@Value("${max.token}") String token,
                                MaxMapper maxMapper,
                                StateMachineDispatcher dispatcher,
                                MaxApiClient maxApiClient) {
        this.maxMapper = maxMapper;
        this.dispatcher = dispatcher;
        this.maxApiClient = maxApiClient;
    }

    @PostMapping("/webhook")
    public Mono<Void> webhook(@RequestBody Mono<UpdateDto> updateDto) {
        return updateDto
                .doOnNext(upd -> log.info(">>> Incoming webhook: {}", upd))
                .flatMap(upd -> {
                    // 1. Гарантированное извлечение chatId с фоллбэками
                    int chatId = upd.getChatId();
                    if (chatId == 0 && upd.getMessage() != null && upd.getMessage().getRecipient() != null) {
                        chatId = upd.getMessage().getRecipient().getChatId();
                    }
                    if (chatId == 0 && upd.getUser() != null && upd.getUser().getUserId() != 0) {
                        chatId = upd.getUser().getUserId();
                    }

                    if (chatId == 0) {
                        log.warn("⚠️ Не удалось извлечь chatId для апдейта: {}", upd.getUpdateType());
                        return Mono.empty();
                    }


                    // 2. Гарантированное извлечение userId с фоллбэками (включая callback)
                    int userId = upd.getUserId();
                    if (userId == 0 && upd.getMessage() != null && upd.getMessage().getRecipient() != null) {
                        userId = upd.getMessage().getRecipient().getUserId();
                    }
                    if (userId == 0 && upd.getCallback() != null && upd.getCallback().getUser() != null) {
                        userId = upd.getCallback().getUser().getUserId();
                    }
                    if (userId == 0 && upd.getMessage() != null
                            && upd.getMessage().getSender() != null
                            && upd.getMessage().getSender().getUserId() != 0) {
                        userId = upd.getMessage().getSender().getUserId();
                    }
                    if (userId == 0 && upd.getUser() != null && upd.getUser().getUserId() != 0) {
                        userId = upd.getUser().getUserId();
                    }

                    if (userId == 0) {
                        log.warn("⚠️ Не удалось извлечь userId для апдейта: {}", upd.getUpdateType());
                        return Mono.empty();
                    }


                    // Передаем в маппер ОБОИХ вычисленных ID (после Шага 2)
                    BotEvent event = maxMapper.toEvent(upd, chatId, userId);
                    if (event == null) {
                        return Mono.empty();
                    }

                    int finalChatId = chatId;

                    // 3. Диспетчеризация
                    if (event.getCallbackId() == null) {
                        return dispatcher.dispatch(finalChatId, event)
                                .flatMap(resp -> {
                                    if (resp == null) return Mono.empty();
                                    return maxApiClient.sendMessage(finalChatId, resp);
                                });
                    } else {
                        return dispatcher.dispatch(finalChatId, event)
                                .flatMap(resp -> {
                                    if (resp == null) return Mono.empty();
                                    return maxApiClient.sendAnswer(event.getCallbackId(), resp);
                                });
                    }
                })
                .doOnError(error -> log.error("❌ Критическая ошибка при обработке вебхука", error))
                .onErrorComplete();
    }


}
