package org.maxbot.miniapp.client;

import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class RospatentClient {

    private final WebClient webClient;

    private static final String URL =
            "https://searchplatform.rospatent.gov.ru/patsearch/v0.2/search";

    public RospatentClient(@Value("${rospatent.token}") String token) {
        this.webClient = WebClient.builder()
                .baseUrl(URL)
                .defaultHeader("Authorization", "Bearer " + token)
                .defaultHeader("User-Agent", "curl/8.0.1")
                .defaultHeader("Accept", "*/*")
                .defaultHeader("Connection", "keep-alive")
                .defaultHeader("Accept-Encoding", "gzip, deflate, br")
                .build();
    }

    public PatentSearchResponse search(String query) {

        Map<String, String> body = Map.of("q", query);

        Map<String, Object> json = webClient.post()
                .contentType(MediaType.APPLICATION_JSON)
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
