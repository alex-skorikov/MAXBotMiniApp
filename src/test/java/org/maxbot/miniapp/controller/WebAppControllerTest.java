package org.maxbot.miniapp.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentService;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebAppControllerTest {

    private PatentService patentSearchService;
    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        this.patentSearchService = Mockito.mock(PatentService.class);

        WebAppController controller = new WebAppController(patentSearchService);
        this.webTestClient = WebTestClient.bindToController(controller)
                .configureClient()
                .baseUrl("/api/patents")
                .build();
        this.webTestClient = WebTestClient.bindToController(controller).build();

    }

    @Test
    void searchSuccessWithNextPage() {

        PatentSearchRequest request = PatentSearchRequest.builder()
                .queryMode("qn")
                .query("Нейросети")
                .limit(10)
                .offset(0)
                .build();

        PatentSearchResponse mockResponse = new PatentSearchResponse();
        mockResponse.setTotal(25);
        mockResponse.setAvailable(25);
        mockResponse.setHits(List.of(new PatentHit(), new PatentHit()));

        Mockito.when(patentSearchService.searchPatents(Mockito.any()))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post()
                .uri("/api/patents/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(PatentSearchPagedResponse.class)
                .value(response -> {
                    assertNotNull(response);
                    assertEquals(2, response.getItems().size());
                    PatentSearchPagedResponse.Pagination pagination = response.getPagination();
                    assertNotNull(pagination);
                    assertEquals(1, pagination.getPage());
                    assertEquals(10, pagination.getPageSize());
                    assertEquals(25, pagination.getTotal());
                    assertTrue(pagination.isHasNext());
                });
    }


    @Test
    void searchSuccessLastPage() {
        // Given
        PatentSearchRequest request = new PatentSearchRequest();
        request.setQueryMode("qn");
        request.setQuery("Протез");
        request.setLimit(10);
        request.setOffset(20);

        PatentSearchResponse mockResponse = new PatentSearchResponse();
        mockResponse.setTotal(25); // При офсете 20 и лимите 10 мы забираем последние 5 элементов, hasNext = false
        mockResponse.setAvailable(25);
        mockResponse.setHits(List.of(new PatentHit()));

        Mockito.when(patentSearchService.searchPatents(request))
                .thenReturn(Mono.just(mockResponse));

        webTestClient.post().uri("/api/patents/search").contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isOk().expectBody(PatentSearchPagedResponse.class).value(response -> {
                    assertNotNull(response);
                    PatentSearchPagedResponse.Pagination pagination = response.getPagination();
                    assertEquals(3, pagination.getPage());
                    assertFalse(pagination.isHasNext());
                });

    }

}