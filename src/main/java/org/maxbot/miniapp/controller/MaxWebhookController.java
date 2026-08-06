package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.config.StateMachineDispatcher;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotAnswerMessage;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.service.PatentCardService;
import org.maxbot.miniapp.service.PatentSearchService;
import org.maxbot.miniapp.util.MaxMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
public class MaxWebhookController {

    private final MaxMapper maxMapper;
    private final StateMachineDispatcher dispatcher;
    private final Map<Integer, String> userState = new ConcurrentHashMap<>();
    private final PatentSearchService patentSearchService;
    private static final Logger log = LoggerFactory.getLogger(MaxWebhookController.class);
    private final MaxApiClient maxApiClient;

    public MaxWebhookController(@Value("${max.token}") String token,
                                MaxMapper maxMapper,
                                StateMachineDispatcher dispatcher,
                                PatentSearchService patentSearchService,
                                MaxApiClient maxApiClient) {
        this.maxMapper = maxMapper;
        this.dispatcher = dispatcher;
        this.patentSearchService = patentSearchService;
        this.maxApiClient = maxApiClient;
    }

    @PostMapping("/webhook")
    public Mono<Void> webhook(@RequestBody Mono<UpdateDto> updateDto) {
        return updateDto.flatMap(upd -> {
            log.info(">>> Incoming webhook: {}", upd);
            // 1. Извлекаем chatId из пришедшего вебхука
            int chatId;
            chatId = upd.getChatId();
            if (chatId == 0) {
                chatId = upd.getMessage().getRecipient().getChatId();
            }
            String callbackId = Optional.ofNullable(upd)
                    .map(UpdateDto::getCallback)
                    .map(CallbackDto::getCallbackId)
                    .orElse(null);

            // 2. Маппим DTO в событие для стейт-машины
            BotEvent event = maxMapper.toEvent(upd);
            int finalChatId = chatId;

            // 3. Передаем chatId и event в диспетчер
            if (callbackId == null) {
                return dispatcher.dispatch(finalChatId, event)
                        // 4. Отправляем ответ пользователю, если автомат его сгенерировал
                        .flatMap(resp -> maxApiClient.sendMessage2(finalChatId, resp));
            } else {
                return dispatcher.dispatch(finalChatId, event)
                        // 4. Отправляем ответ пользователю, если автомат его сгенерировал
                        .flatMap(resp -> maxApiClient.sendAnswer(callbackId, resp));
            }

        });
    }

    // ===========================
    // PATENT SEARCH
    // ===========================

    private Mono<Void> handlePatentSearch(String queryMode, String query, int userId, int chatId) {

        return patentSearchService.searchReactive(queryMode, query, 5, 0)
                .flatMap(raw -> {

                    if (raw.getHits().isEmpty()) {
                        userState.remove(userId);
                        BotAnswerMessage message = BotAnswerMessage.builder()
                                .text("Ничего не найдено.")
                                .build();
                        return maxApiClient.sendMessage(chatId, message);
                    }

                    List<Mono<Void>> messages = raw.getHits().stream()
                            .map(hit -> {
                                String patentUrl = "https://searchplatform.rospatent.gov.ru/doc/" + hit.getId();
                                BotAnswerMessage response = BotAnswerMessage.builder()
                                        .text(PatentCardService.formatPatentCard(hit))
                                        .attachments(List.of(
                                                BotAnswerMessage.Attachment.builder()
                                                        .type("inline_keyboard")
                                                        .payload(BotAnswerMessage.InlineKeyboardPayload.builder()
                                                                .buttons(List.of(List.of(
                                                                        BotAnswerMessage.Button.builder()
                                                                                .type("link")
                                                                                .text("Ссылка")
                                                                                .url(patentUrl)
                                                                                .build()
                                                                )))
                                                                .build())
                                                        .build()
                                        ))
                                        .build();

                                return maxApiClient.sendMessage(chatId, response);
                            })
                            .toList();

                    userState.remove(userId);

                    return Mono.when(messages);
                });
    }
}
