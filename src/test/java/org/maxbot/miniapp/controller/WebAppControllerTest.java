package org.maxbot.miniapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.dto.webapp.SessionInitRequest;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WebAppControllerTest {

    @Mock
    private PatentService patentService;
    @Mock
    private ContextRepository contextRepository;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        WebAppController controller = new WebAppController(patentService, contextRepository);
        this.webTestClient = WebTestClient.bindToController(controller).build();
    }

    @Test
    void getUserContextShouldReturnExistingContext() {
        // Given
        UserContext mockCtx = UserContext.builder().chatId("123").limit(10).offset(0).build();
        when(contextRepository.load("123")).thenReturn(mockCtx);

        // When & Then
        webTestClient.get()
                .uri("/api/session/123")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserContext.class)
                .value(ctx -> {
                    assertNotNull(ctx);
                    assertEquals("123", ctx.getChatId());
                    assertEquals(10, ctx.getLimit());
                });
    }

    @Test
    void getUserContextShouldReturnDefaultWhenEmpty() {
        // Given
        when(contextRepository.load("777")).thenReturn(null);

        // When & Then
        webTestClient.get()
                .uri("/api/session/777")
                .exchange()
                .expectStatus().isOk()
                .expectBody(UserContext.class)
                .value(ctx -> {
                    assertNotNull(ctx);
                    assertEquals("777", ctx.getChatId());
                    assertEquals(5, ctx.getLimit()); // defaultIfEmpty
                });
    }

    @Test
    void sessionInitShouldCreateAndSaveContext() {
        // Given
        SessionInitRequest request = new SessionInitRequest();
        request.setUserId("444");
        request.setChatId("555");

        when(contextRepository.load("444")).thenReturn(null); // Эмулируем первую привязку

        // When & Then
        webTestClient.post()
                .uri("/api/session/init")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS");

        verify(contextRepository, times(1)).save(any(UserContext.class));
    }

    @Test
    void searchShouldTriggerSyncAndReturnPagedResponse() {
        // Given
        PatentSearchRequest req = new PatentSearchRequest();
        req.setQuery("Двигатель");
        req.setLimit(5);
        req.setOffset(0);

        // Формируем фильтры для покрытия веток метода syncUserContext
        PatentSearchRequest.Filter filter = new PatentSearchRequest.Filter();
        PatentSearchRequest.DatePublished datePublished = new PatentSearchRequest.DatePublished();
        PatentSearchRequest.Range range = new PatentSearchRequest.Range();
        range.setGt("2025-01-01");
        datePublished.setRange(range);
        filter.setDatePublished(datePublished);

        PatentSearchRequest.Classification classification = new PatentSearchRequest.Classification();
        classification.setValues(List.of("A61K"));
        filter.setClassification(classification);
        req.setFilter(filter);
        req.setDatasets(List.of("RU"));

        PatentSearchResponse rawResponse = new PatentSearchResponse();
        PatentHit hit = new PatentHit();
        hit.setId("PAT123");
        rawResponse.setHits(List.of(hit));

        when(patentService.searchPatents(any(PatentSearchRequest.class))).thenReturn(Mono.just(rawResponse));
        when(contextRepository.load("999")).thenReturn(new UserContext());

        // When & Then
        webTestClient.post()
                .uri("/api/search?userId=999")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk();

        // Даем асинхронному Mono.fromRunnable() в .doOnNext() успеть выполниться
        verify(contextRepository, timeout(1000)).save(any(UserContext.class));
    }

    @Test
    void getDocumentFromContextShouldReturnHitWhenFound() {
        // Given
        UserContext ctx = new UserContext();
        PatentHit hit = new PatentHit();
        hit.setId("DOC77");
        ctx.setHits(List.of(hit));

        when(contextRepository.load("user1")).thenReturn(ctx);

        // When & Then
        webTestClient.get()
                .uri("/api/docs/DOC77?userId=user1")
                .exchange()
                .expectStatus().isOk()
                .expectBody(PatentHit.class)
                .value(resultHit -> {
                    assertNotNull(resultHit);
                    assertEquals("DOC77", resultHit.getId());
                });
    }

    @Test
    void getDocumentFromContextShouldReturnNotFoundWhenMissing() {
        // Given
        when(contextRepository.load("user2")).thenReturn(null);

        // When & Then
        webTestClient.get()
                .uri("/api/docs/DOC_MISSING?userId=user2")
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void exportDocumentShouldReturnJsonFileAttachment() {
        // Given
        UserContext ctx = new UserContext();
        PatentHit hit = new PatentHit();
        hit.setId("UA12345");
        ctx.setHits(List.of(hit));

        when(contextRepository.load("user3")).thenReturn(ctx);

        // When & Then
        webTestClient.get()
                .uri("/api/docs/export?docId=UA12345&userId=user3")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
                .expectHeader()
                .valueEquals(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"patent_UA12345.json\"")
                .expectBody(String.class)
                .value(json -> {
                    assertNotNull(json);
                    assertTrue(json.contains("UA12345"), "Экспортируемый файл должен содержать ID документа");
                });
    }

    @Test
    void exportDocumentShouldReturnNotFoundWhenCacheIsEmpty() {
        // Given
        UserContext emptyCtx = new UserContext();
        emptyCtx.setHits(Collections.emptyList());
        when(contextRepository.load("user4")).thenReturn(emptyCtx);

        // When & Then
        webTestClient.get()
                .uri("/api/docs/export?docId=UA12345&userId=user4")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}
