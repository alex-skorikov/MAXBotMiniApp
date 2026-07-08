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


    @Value("${rospatent.token}")
    private String token;

    private final WebClient webClient;

    public RospatentClient() {
        this.webClient = WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create()
                                .resolver(spec -> spec.ndots(1)) // ← критично
                ))
                .build();
    }

    public PatentSearchResponse search(String query) {

        Map<String, String> body = Map.of("q", query);

        Map<String, Object> json = webClient.post()
                .uri("https://searchplatform.rospatent.gov.ru/patsearch/v0.2/search")
                .header("Authorization", "Bearer " + token)
                .header("User-Agent", "curl/8.0.1")
                .header("Accept", "*/*")
                .header("Connection", "keep-alive")
                .header("Accept-Encoding", "gzip, deflate, br")
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
