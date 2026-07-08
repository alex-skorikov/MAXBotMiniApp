package org.maxbot.miniapp.client;

import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.*;

@Component
public class RospatentClient {
    private final WebClient webClient;

    public RospatentClient(@Value("${rospatent.token}") String token) {

        this.webClient = WebClient.builder()
                .baseUrl("https://searchplatform.rospatent.gov.ru")
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public PatentSearchResponse search(String query) {

        Map<String, String> body = Map.of("q", query);

        Map<String, Object> json = webClient.post()
                .uri("/patsearch/v0.2/search")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        int total = (int) json.get("total");
        int available = (int) json.get("available");

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
