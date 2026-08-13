package org.maxbot.miniapp.controller;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MaxSubscriptionsControllerTest {

    private MockWebServer mockWebServer;
    private WebTestClient webTestClient;
    private final String testToken = "test-token-777";

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();

        String mockUrl = mockWebServer.url("/").toString();

        MaxSubscriptionsController controller = new MaxSubscriptionsController(testToken, mockUrl);

        this.webTestClient = WebTestClient.bindToController(controller).build();

    }

    @AfterEach
    void tearDown() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Test
    void createSubscriptionSuccess() throws Exception { // Изменили InterruptedException на Exception для ObjectMapper
        String expectedResponse = """
                {"status":"created","id":"sub_123"}
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(expectedResponse));

        webTestClient.post()
                .uri("/subscriptions")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/subscriptions", recordedRequest.getPath());
        assertEquals(testToken, recordedRequest.getHeader("Authorization"));

        String requestBody = recordedRequest.getBody().readUtf8();

        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        java.util.Map<String, Object> actualBody = mapper.readValue(requestBody, new com.fasterxml.jackson.core.type.TypeReference<>() {
        });

        assertEquals("https://logiq-synapse.ru/webhook", actualBody.get("url"));
    }

    @Test
    void getSubscriptionsSuccess() throws InterruptedException {
        String expectedResponse = """
                [{"id":"sub_123","url":"https://logiq-synapse.ru/webhook"}]
                """;

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .setBody(expectedResponse));

        webTestClient.get()
                .uri("/subscriptions")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .isEqualTo(expectedResponse);

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("GET", recordedRequest.getMethod());
        assertEquals("/subscriptions", recordedRequest.getPath());
        assertEquals(testToken, recordedRequest.getHeader("Authorization"));
    }
}