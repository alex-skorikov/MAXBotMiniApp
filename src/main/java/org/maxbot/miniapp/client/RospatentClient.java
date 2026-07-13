package org.maxbot.miniapp.client;

import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RospatentClient {

    private static final Logger log = LoggerFactory.getLogger(RospatentClient.class);

    @Value("${rospatent.token}")
    private String token;

    private final WebClient webClient;

    private static final String URL =
            "https://searchplatform.rospatent.gov.ru/patsearch/v0.2/search";

    public RospatentClient() {
        this.webClient = WebClient.builder()
                .baseUrl(URL)
                .defaultHeader("User-Agent", "curl/8.0.1")
                .defaultHeader("Accept", "*/*")
                .defaultHeader("Connection", "keep-alive")
                .defaultHeader("Accept-Encoding", "gzip, deflate, br")
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    // -----------------------------
    // ЛОГИРОВАНИЕ ЗАПРОСОВ
    // -----------------------------
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            log.info("=== Rospatent REQUEST ===");
            log.info("URI: {}", request.url());
            log.info("Method: {}", request.method());
            log.info("Headers:");
            request.headers().forEach((name, values) ->
                    values.forEach(value -> log.info("{}: {}", name, value))
            );
            return Mono.just(request);
        });
    }

    // -----------------------------
    // ЛОГИРОВАНИЕ ОТВЕТОВ
    // -----------------------------
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            log.info("=== Rospatent RESPONSE ===");
            log.info("Status: {}", response.statusCode());

            HttpHeaders headers = response.headers().asHttpHeaders();

            return response.bodyToMono(String.class)
                    .flatMap(body -> {
//                        log.info("Body: {}", body);
                        return Mono.just(
                                ClientResponse
                                        .create(response.statusCode())
                                        .headers(h -> h.addAll(headers))
                                        .body(body)
                                        .build()
                        );
                    });
        });
    }

    // -----------------------------
    // ПУБЛИЧНЫЕ МЕТОДЫ ПОИСКА
    // -----------------------------

    /** Обычный текстовый поиск (queryMode = "q") */
    public PatentSearchResponse searchByQuery(String query, Integer limit, Integer offset) {

        Map<String, Object> body = Map.of(
                "qn", query,
                "limit", limit,
                "offset", offset
        );
        return execute(body);
    }

    /** Поиск по номеру (queryMode = "qn") */
    public PatentSearchResponse searchByNumber(String number) {
        Map<String, Object> body = Map.of(
                "qn", number
        );

        return execute(body);
    }

    // -----------------------------
    // ЕДИНЫЙ МЕТОД ВЫПОЛНЕНИЯ ЗАПРОСА
    // -----------------------------
    private PatentSearchResponse execute(Map<String, Object> body) {

        Map<String, Object> json;

        try {
            json = webClient.post()
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            log.error("Rospatent API error", e);
            throw new RuntimeException("Rospatent API error: " + e.getMessage());
        }

        return mapResponse(json);
    }

    // -----------------------------
    // МАППИНГ ОТВЕТА В DTO
    // -----------------------------
    private PatentSearchResponse mapResponse(Map<String, Object> json) {

        int total = ((Number) json.get("total")).intValue();
        int available = ((Number) json.get("available")).intValue();

        List<Map<String, Object>> rawHits = (List<Map<String, Object>>) json.get("hits");
        List<PatentHit> hits = new ArrayList<>();

        for (Map<String, Object> raw : rawHits) {

            Map<String, Object> snippet = (Map<String, Object>) raw.get("snippet");

            PatentHit hit = new PatentHit();
            hit.setId((String) raw.get("id"));
            hit.setTitle((String) snippet.get("title"));
            hit.setDescription((String) snippet.get("description"));
            hit.setApplicant((String) snippet.get("applicant"));
            hit.setInventor((String) snippet.get("inventor"));

            Map<String, Object> classification =
                    (Map<String, Object>) snippet.get("classification");

            hit.setIpc((String) classification.get("ipc"));

            hits.add(hit);
        }

        PatentSearchResponse result = new PatentSearchResponse();
        result.setTotal(total);
        result.setAvailable(available);
        result.setHits(hits);

        return result;
    }
}
