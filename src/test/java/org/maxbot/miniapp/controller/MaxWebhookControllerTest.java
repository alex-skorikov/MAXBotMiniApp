package org.maxbot.miniapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.RecipientDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.statemachine.StateMachineDispatcher;
import org.maxbot.miniapp.util.MaxMapper;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;

class MaxWebhookControllerTest {

    private MaxMapper maxMapper;
    private StateMachineDispatcher dispatcher;
    private MaxApiClient maxApiClient;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.maxMapper = Mockito.mock(MaxMapper.class);
        this.dispatcher = Mockito.mock(StateMachineDispatcher.class);
        this.maxApiClient = Mockito.mock(MaxApiClient.class);

        MaxWebhookController controller = new MaxWebhookController(
                "test-token",
                maxMapper,
                dispatcher,
                maxApiClient
        );

        this.webTestClient = WebTestClient.bindToController(controller).build();

    }

/*    @Test
    void webhookSuccessSendMessage() {
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(123);
        updateDto.setUserId(23454);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getCallbackId()).thenReturn(null);

        BotResponse mockResponse = BotResponse.builder().notify(false).build();

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), anyInt(), anyInt()))
                .thenReturn(mockEvent);

        Mockito.when(dispatcher.dispatch(anyInt(), any(BotEvent.class)))
                .thenReturn(Mono.just(mockResponse));

        Mockito.when(maxApiClient.sendMessage(anyInt(), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        // Вызов эндпоинта
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxApiClient, Mockito.times(1))
                .sendMessage(anyInt(), any(BotResponse.class));

        Mockito.verify(maxApiClient, Mockito.never())
                .sendAnswer(anyString(), any(BotResponse.class));
    }*/

/*    @Test
    void webhookSuccessSendAnswer() {
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(456);
        updateDto.setUserId(23454);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getCallbackId()).thenReturn("cb_789");

        BotResponse mockResponse = BotResponse.builder().build();

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), anyInt(), anyInt()))
                .thenReturn(mockEvent);

        Mockito.when(dispatcher.dispatch(anyInt(), any(BotEvent.class)))
                .thenReturn(Mono.just(mockResponse));

        Mockito.when(maxApiClient.sendAnswer(anyString(), any(BotResponse.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        // Проверяем факт вызова метода с любыми аргументами
        Mockito.verify(maxApiClient, Mockito.times(1))
                .sendAnswer(anyString(), any(BotResponse.class));

        Mockito.verify(maxApiClient, Mockito.never())
                .sendMessage(anyInt(), any(BotResponse.class));
    }*/

    @Test
    void webhookChatIdFallbackExtraction() {
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(0);
        updateDto.setUserId(23454);

        MessageDto messageDto = new MessageDto();
        RecipientDto recipientDto = new RecipientDto();
        recipientDto.setChatId(999);
        messageDto.setRecipient(recipientDto);
        updateDto.setMessage(messageDto);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);

        // Настраиваем маппер: первый ID — chatId (999), второй — userId (23454)
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(999), eq(23454)))
                .thenReturn(mockEvent);

        // Настраиваем диспетчер: он должен получить правильный chatId (999)
        Mockito.when(dispatcher.dispatch(eq(999), any(BotEvent.class)))
                .thenReturn(Mono.empty());

        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        // Проверяем точный вызов с верными аргументами
        Mockito.verify(maxMapper, Mockito.times(1))
                .toEvent(any(UpdateDto.class), eq(999), eq(23454));
    }


    @Test
    void webhookZeroChatIdReturnsEmpty() {
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(0); // Нигде нет валидного chatId 

        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxMapper, Mockito.never())
                .toEvent(any(), anyInt(), anyInt());

    }

    @Test
    void webhookNullEventReturnsEmpty() {
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(111);

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(111), anyInt()))
                .thenReturn(null);

        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(dispatcher, Mockito.never())
                .dispatch(anyInt(), any());

    }

    @Test
    void webhookCriticalErrorCompletesGracefully() {
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(222);

        // Симулируем падение маппера с ошибкой (например, во время десериализации)
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(222), anyInt()))
                .thenThrow(new RuntimeException("Mapping exception"));

        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

    }
}