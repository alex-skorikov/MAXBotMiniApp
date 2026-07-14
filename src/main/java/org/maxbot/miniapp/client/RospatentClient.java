package org.maxbot.miniapp.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
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
                    .flatMap(body -> Mono.just(
                            ClientResponse
                                    .create(response.statusCode())
                                    .headers(h -> h.addAll(headers))
                                    .body(body)
                                    .build()
                    ));
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
    public PatentSearchResponse searchByNumber(String number) throws IOException {
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
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
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
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        PatentSearchResponse result = new PatentSearchResponse();

        result.setTotal(((Number) json.get("total")).intValue());
        result.setAvailable(((Number) json.get("available")).intValue());

        List<Map<String, Object>> rawHits = (List<Map<String, Object>>) json.get("hits");
        List<PatentHit> hits = new ArrayList<>();

        for (Map<String, Object> raw : rawHits) {
            PatentHit hit = mapper.convertValue(raw, PatentHit.class);
            hits.add(hit);
        }

        result.setHits(hits);
        return result;
    }


}
