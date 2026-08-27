package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.statemachine.StateMachineDispatcher;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.core.MaxMapper;
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
    public Mono<Void> webhook(@RequestBody UpdateDto upd) {
        if (upd == null) {
            return Mono.empty();
        }

        log.info("\uD83D\uDCE1 >>> INCOMING WEBHOOK: {}", upd);

        // 1. Вычисляем гарантированно правильный chatId
        int resolvedChatId = upd.getChatId();
        if (resolvedChatId == 0 && upd.getMessage() != null && upd.getMessage().getRecipient() != null) {
            resolvedChatId = upd.getMessage().getRecipient().getChatId();
        }
        if (resolvedChatId == 0 && upd.getUser() != null && upd.getUser().getUserId() != 0) {
            resolvedChatId = upd.getUser().getUserId();
        }

        if (resolvedChatId == 0) {
            log.warn("⚠️ Не удалось извлечь chatId для апдейта: {}", upd.getUpdateType());
            return Mono.empty();
        }

        // 2. Вычисляем гарантированно правильный userId человека
        int resolvedUserId = upd.getUserId();

        if (resolvedUserId == 0 && upd.getCallback() != null && upd.getCallback().getUser() != null) {
            resolvedUserId = upd.getCallback().getUser().getUserId();
        }
        if (resolvedUserId == 0 && upd.getMessage() != null
                && upd.getMessage().getSender() != null
                && upd.getMessage().getSender().getUserId() != 0) {
            resolvedUserId = upd.getMessage().getSender().getUserId(); // Теперь здесь ЖЕЛЕЗНО запишется 329529068
        }
        if (resolvedUserId == 0 && upd.getUser() != null && upd.getUser().getUserId() != 0) {
            resolvedUserId = upd.getUser().getUserId();
        }
        if (resolvedUserId == 0 && upd.getMessage() != null && upd.getMessage().getRecipient() != null) {
            resolvedUserId = upd.getMessage().getRecipient().getUserId();
        }
        if (resolvedUserId == 0) {
            log.warn("⚠️ Не удалось извлечь userId для апдейта: {}", upd.getUpdateType());
            return Mono.empty();
        }

        // Фиксируем ID как константы для изоляции в реактивном потоке
        final int finalChatId = resolvedChatId;
        final int finalUserId = resolvedUserId;

        return Mono.defer(() -> {
                    // Передаем строго вычисленный finalUserId
                    BotEvent event = maxMapper.toEvent(upd, finalChatId, finalUserId);

                    if (event == null || event.getType() == null) {
                        return Mono.empty();
                    }

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
