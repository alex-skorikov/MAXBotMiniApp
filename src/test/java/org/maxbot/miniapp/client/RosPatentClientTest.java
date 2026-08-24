package org.maxbot.miniapp.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RosPatentClientTest {

    private MockWebServer mockWebServer;
    private RosPatentClient rospatentClient;
    private final String testToken = "rospatent-secret-token-123";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() throws IOException {
        this.mockWebServer = new MockWebServer();
        this.mockWebServer.start();
        WebClient webClient = WebClient.builder().build();

        String mockUrl = mockWebServer.url("/patsearch/v0.2/search").toString();

        this.rospatentClient = new RosPatentClient(webClient, testToken, mockUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        this.mockWebServer.shutdown();
    }

    @Test
    void searchReactiveSuccess() throws Exception {
        Map<String, Object> mockJsonResponse = Map.of(
                "total", 150,
                "available", 50,
                "hits", List.of(
                        Map.of("id", "RU100", "title", "Тестовый патент 1"),
                        Map.of("id", "RU200", "title", "Тестовый патент 2")
                )
        );
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(mockJsonResponse)));

        PatentSearchRequest request = PatentSearchRequest.builder()
                .queryMode("qn")
                .query("Искусственный интеллект")
                .limit(10)
                .offset(0)
                .build();

        Mono<PatentSearchResponse> result = rospatentClient.searchReactive(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(150, response.getTotal());
                    assertEquals(50, response.getAvailable());
                    assertEquals(2, response.getHits().size());
                })
                .verifyComplete();

        RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertEquals("POST", recordedRequest.getMethod());
        assertEquals("/patsearch/v0.2/search", recordedRequest.getPath());
        assertEquals("Bearer " + testToken, recordedRequest.getHeader("Authorization"));

        String requestBody = recordedRequest.getBody().readUtf8();
        assertTrue(requestBody.contains("\"qn\":\"Искусственный интеллект\""));
        assertTrue(requestBody.contains("\"limit\":10"));
        assertTrue(requestBody.contains("\"offset\":0"));

    }

/*    @Test
    void searchReactiveOnErrorResumeFallback() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Connection", "close")
                .setBody(""));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader("Connection", "close")
                .setBody(""));

        PatentSearchRequest request = PatentSearchRequest.builder()
                .queryMode("qn")
                .query("error query")
                .limit(5)
                .offset(0)
                .build();

        Mono<PatentSearchResponse> result = rospatentClient.searchReactive(request);

        StepVerifier.create(result)
                .assertNext(response -> {
                    assertNotNull(response);
                    assertEquals(0, response.getTotal());
                    assertEquals(0, response.getAvailable());
                    assertTrue(response.getHits().isEmpty());
                })
                .verifyComplete();
    }*/
}