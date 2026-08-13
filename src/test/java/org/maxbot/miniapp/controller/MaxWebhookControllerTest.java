package org.maxbot.miniapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
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
import static org.mockito.ArgumentMatchers.anyString;
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
    void webhookSuccessSendMessage() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(123);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getCallbackId()).thenReturn(null);

        BotResponse mockResponse = BotResponse.builder().notify(false).build();

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(123)))
                .thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(123, mockEvent))
                .thenReturn(Mono.just(mockResponse));
        Mockito.when(maxApiClient.sendMessage(123, mockResponse))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxApiClient, Mockito.times(1))
                .sendMessage(123, mockResponse);
        Mockito.verify(maxApiClient, Mockito.never())
                .sendAnswer(anyString(), any(BotResponse.class));

    }

    @Test
    void webhookSuccessSendAnswer() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(456);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(mockEvent.getCallbackId()).thenReturn("cb_789");

        BotResponse mockResponse = BotResponse.builder().build();

        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(456)))
                .thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(456, mockEvent))
                .thenReturn(Mono.just(mockResponse));
        Mockito.when(maxApiClient.sendAnswer("cb_789", mockResponse))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxApiClient, Mockito.times(1))
                .sendAnswer("cb_789", mockResponse);
        Mockito.verify(maxApiClient, Mockito.never())
                .sendMessage(anyInt(), any(BotResponse.class));

    }

    @Test
    void webhookChatIdFallbackExtraction() {
        // Given
        // когда chatId равен 0, но лежит внутри получателя
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(0);

        MessageDto messageDto = new MessageDto();
        RecipientDto recipientDto = new RecipientDto();
        recipientDto.setChatId(999);
        messageDto.setRecipient(recipientDto);
        updateDto.setMessage(messageDto);

        BotEvent mockEvent = Mockito.mock(BotEvent.class);
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(999)))
                .thenReturn(mockEvent);
        Mockito.when(dispatcher.dispatch(999, mockEvent))
                .thenReturn(Mono.empty());

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxMapper, Mockito.times(1))
                .toEvent(any(UpdateDto.class), eq(999));

    }

    @Test
    void webhookZeroChatIdReturnsEmpty() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(0); // Нигде нет валидного chatId 

        // When & Then
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

        Mockito.verify(maxMapper, Mockito.never())
                .toEvent(any(), anyInt());

    }

    @Test
    void webhookNullEventReturnsEmpty() {
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(111);

        // Маппер возвращает null (например, неизвестный боту тип апдейта)
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(111)))
                .thenReturn(null);

        // When & Then
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
        // Given
        UpdateDto updateDto = new UpdateDto();
        updateDto.setChatId(222);

        // Симулируем падение маппера с ошибкой (например, во время десериализации)
        Mockito.when(maxMapper.toEvent(any(UpdateDto.class), eq(222)))
                .thenThrow(new RuntimeException("Mapping exception"));

        // When & Then
        // Благодаря вашему .onErrorComplete() контроллер вернет 200 OK, погасив ошибку в стриме
        webTestClient.post()
                .uri("/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(updateDto)
                .exchange()
                .expectStatus().isOk();

    }
}