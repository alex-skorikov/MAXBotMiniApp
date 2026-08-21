package org.maxbot.miniapp.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.util.PatentsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;

@Component
public class RospatentClient {

    private static final Logger log = LoggerFactory.getLogger(RospatentClient.class);

    private final WebClient webClient;
    private final String token;

    private final String URL;

    public RospatentClient(WebClient webClient,
                           @Value("${rospatent.token}") String token,
                           @Value("${rospatent.url}") String url) {
        this.webClient = webClient;
        this.token = token;
        this.URL = url;
    }

    // -----------------------------
    // МЕТОДЫ ПОИСКА
    // -----------------------------

    // --- Async ---
    public Mono<PatentSearchResponse> searchReactive(PatentSearchRequest request) {
        Map<String, Object> body = PatentsUtil.patentRequestToMap(request);
        return executeReactive(body);
    }

    private Mono<PatentSearchResponse> executeReactive(Map<String, Object> body) {
        log.info(">>> REQUEST RospatentClient : {}", body);

        return webClient.post()
                .uri(URL)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .retryWhen(
                        Retry.backoff(1, Duration.ofSeconds(2))
                                .filter(e -> !(e instanceof TimeoutException))
                )
                .timeout(Duration.ofSeconds(30))
                .onErrorResume(e -> {
                    log.error("Rospatent API error", e);
                    return Mono.just(Map.of(
                            "total", 0,
                            "available", 0,
                            "hits", List.of()
                    ));
                })
                .map(this::mapResponse)
                .doOnNext(resp -> log.info(">>> RESPONSE RospatentClient total: {}", resp.getTotal()));
    }

    // --- МАППИНГ ОТВЕТА В DTO ---
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
