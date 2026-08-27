package org.maxbot.miniapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.RecipientDto;
import org.maxbot.miniapp.dto.bot.SenderDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.statemachine.StateMachineDispatcher;
import org.maxbot.miniapp.core.MaxMapper;
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

    @Test
    void webhookUserIdFallbackExtractionAndSendMessage() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(555);
        updateDto.setUserId(0); // Проверяем извлечение userId из вложенного sender

        org.maxbot.miniapp.dto.bot.MessageDto messageDto = new org.maxbot.miniapp.dto.bot.MessageDto();
        org.maxbot.miniapp.dto.bot.SenderDto senderDto = new org.maxbot.miniapp.dto.bot.SenderDto();
        senderDto.setUserId(777); // resolvedUserId станет 777
        messageDto.setSender(senderDto);
        updateDto.setMessage(messageDto);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getType()).thenReturn(org.maxbot.miniapp.statemachine.BotEvents.USER_OPEN_CHAT);
        Mockito.when(mockEvent.getCallbackId()).thenReturn(null); // callbackId == null -> ветка sendMessage

        org.maxbot.miniapp.dto.bot.BotResponse mockResponse = org.maxbot.miniapp.dto.bot.BotResponse.builder()
                .text("Test Send Message").build();

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(555), eq(777))).thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(eq(555), eq(mockEvent))).thenReturn(Mono.just(mockResponse));
        Mockito.when(maxApiClient.sendMessage(eq(555), eq(mockResponse))).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxApiClient, Mockito.times(1)).sendMessage(555, mockResponse);
        Mockito.verify(maxApiClient, Mockito.never()).sendAnswer(any(), any());
    }

    @Test
    void webhookSendAnswerWhenCallbackIdIsPresent() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(444);
        updateDto.setUserId(333);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getType()).thenReturn(org.maxbot.miniapp.statemachine.BotEvents.USER_OPEN_CHAT);
        Mockito.when(mockEvent.getCallbackId()).thenReturn("callback_id_abc"); // callbackId != null -> ветка sendAnswer

        org.maxbot.miniapp.dto.bot.BotResponse mockResponse = org.maxbot.miniapp.dto.bot.BotResponse.builder()
                .text("Test Send Answer").build();

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(444), eq(333))).thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(eq(444), eq(mockEvent))).thenReturn(Mono.just(mockResponse));
        Mockito.when(maxApiClient.sendAnswer(eq("callback_id_abc"), eq(mockResponse))).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxApiClient, Mockito.times(1)).sendAnswer("callback_id_abc", mockResponse);
        Mockito.verify(maxApiClient, Mockito.never()).sendMessage(anyInt(), any());
    }

    @Test
    void webhookZeroUserIdReturnsEmpty() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(111);
        updateDto.setUserId(0); // Нигде нет валидного userId

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxMapper, Mockito.never()).toEvent(any(), anyInt(), anyInt());
    }

    @Test
    void webhookChatIdExtractionFromUser() {
        // Given: chatId = 0, но есть объект User, откуда можно взять ID
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(0);
        updateDto.setUserId(111);

        SenderDto senderDto = new SenderDto();
        senderDto.setUserId(999);

        updateDto.setUser(senderDto);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(999), eq(111))).thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(eq(999), eq(mockEvent))).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void webhookUserIdExtractionFromCallbackUser() {
        // Given: userId = 0, но есть Callback -> User
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(222);
        updateDto.setUserId(0);

        CallbackDto callbackDto = new CallbackDto();
        SenderDto senderDto = new SenderDto();
        senderDto.setUserId(888); // resolvedUserId станет 888
        callbackDto.setUser(senderDto);
        updateDto.setCallback(callbackDto);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(222), eq(888))).thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(eq(222), eq(mockEvent))).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void webhookUserIdExtractionFromMainUser() {
        // Given: userId = 0, но есть корневой User
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(333);
        updateDto.setUserId(0);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(333), eq(777))).thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(eq(333), eq(mockEvent))).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void webhookUserIdExtractionFromMessageRecipient() {
        // Given: userId = 0, но есть Message -> Recipient
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(444);
        updateDto.setUserId(0);

        org.maxbot.miniapp.dto.bot.MessageDto messageDto = new org.maxbot.miniapp.dto.bot.MessageDto();
        org.maxbot.miniapp.dto.bot.RecipientDto recipientDto = new org.maxbot.miniapp.dto.bot.RecipientDto();
        recipientDto.setUserId(666); // resolvedUserId станет 666
        messageDto.setRecipient(recipientDto);
        updateDto.setMessage(messageDto);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(444), eq(666))).thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(eq(444), eq(mockEvent))).thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();
    }

}