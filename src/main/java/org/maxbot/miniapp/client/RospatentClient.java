package org.maxbot.miniapp.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RospatentClient {

    private static final Logger log = LoggerFactory.getLogger(RospatentClient.class);

    private final WebClient webClient;
    private final String token;

    private static final String URL =
            "https://searchplatform.rospatent.gov.ru/patsearch/v0.2/search";

    public RospatentClient(WebClient webClient, @Value("${rospatent.token}") String token) {
        this.webClient = webClient;
        this.token = token;
    }

    // -----------------------------
    // МЕТОДЫ ПОИСКА
    // -----------------------------

    // --- Обычный текстовый поиск (queryMode = "q") ---
    public PatentSearchResponse searchByQuery(String queryMode, String query, Integer limit, Integer offset) {

        Map<String, Object> body = Map.of(
                queryMode, query,
                "limit", limit,
                "offset", offset
        );
        return execute(body);
    }

    private PatentSearchResponse execute(Map<String, Object> body) {
        log.info(">>> REQUEST RospatentClient : {}", body);

        Map<String, Object> json;
        try {
            json = webClient.post()
                    .uri(URL)
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .block();
        } catch (Exception e) {
            log.error("Rospatent API error", e);
            throw new RuntimeException("Rospatent API error: " + e.getMessage());
        }

        log.info(">>> RESPONSE RospatentClient total: {}", json.get("total"));

        return json != null ? mapResponse(json) : new PatentSearchResponse();
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
