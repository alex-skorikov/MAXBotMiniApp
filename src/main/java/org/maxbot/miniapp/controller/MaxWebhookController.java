package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.config.StateMachineDispatcher;
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
                    // 1. Извлекаем chatId из пришедшего вебхука
                    int chatId;
                    chatId = upd.getChatId();
                    if (chatId == 0) {
                        chatId = upd.getMessage().getRecipient().getChatId();
                    }

                    // 2. Маппим DTO в событие для стейт-машины
                    BotEvent event = maxMapper.toEvent(upd, chatId);
                    int finalChatId = chatId;

                    // 3. Передаем chatId и event в диспетчер
                    if (event.getCallbackId() == null) {
                        return dispatcher.dispatch(finalChatId, event)
                                // 4. Отправляем ответ пользователю, если автомат его сгенерировал
                                .flatMap(resp -> maxApiClient.sendMessage(finalChatId, resp));
                    } else {
                        return dispatcher.dispatch(finalChatId, event)
                                // 4. Отправляем ответ пользователю, если автомат его сгенерировал
                                .flatMap(resp -> maxApiClient.sendAnswer(event.getCallbackId(), resp));
                    }

                });
    }
}
