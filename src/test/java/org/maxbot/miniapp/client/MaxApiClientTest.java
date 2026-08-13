package org.maxbot.miniapp.client;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class MaxApiClientTest {
    private MockWebServer mockWebServer;
    private MaxApiClient maxApiClient;
    private final String testToken = "Bearer test-token-123";

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();// 2. Получаем динамический URL запущенного мок-сервера
        String mockUrl = mockWebServer.url("/").toString();

        WebClient baseWebClient = WebClient.builder().build();

        this.maxApiClient = new MaxApiClient(testToken, mockUrl, baseWebClient);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Test
    void sendMessageSuccess() throws InterruptedException {
        int chatId = 555;
        BotResponse responseBody = BotResponse.builder().build();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json"));

        Mono<Void> result = maxApiClient.sendMessage(chatId, responseBody);

        StepVerifier.create(result)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/messages?chat_id=555", recordedRequest.getPath());
        assertEquals(testToken, recordedRequest.getHeader("Authorization"));
    }

    @Test
    void sendAnswerSuccess() throws InterruptedException {
        String callbackId = "cb_999";
        BotResponse responseBody = BotResponse.builder().build();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200));

        Mono<Void> result = maxApiClient.sendAnswer(callbackId, responseBody);

        StepVerifier.create(result)
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/answers?callback_id=cb_999", recordedRequest.getPath());
        assertNotNull(recordedRequest.getBody().readUtf8());
    }

    @Test
    void sendMessageErrorHandling() {
        int chatId = 111;
        BotResponse responseBody = BotResponse.builder().build();
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500));

        Mono<Void> result = maxApiClient.sendMessage(chatId, responseBody);

        StepVerifier.create(result)
                .expectError()
                .verify();
    }
}
