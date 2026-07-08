package org.maxbot.miniapp.client;

import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Component
public class RospatentClient {

    private final RestTemplate rest = new RestTemplate();
    private static final String URL =
            "https://searchplatform.rosPatent.gov.ru/patsearch/v0.2/search";

    @Value("${rospatent.token}")
    private String token;

    public PatentSearchResponse search(String query) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        headers.set("Authorization", "Bearer " + token);
        headers.set("User-Agent", "Mozilla/5.0");
        headers.set("Origin", "https://searchplatform.rospatent.gov.ru");
        headers.set("Referer", "https://searchplatform.rospatent.gov.ru");

        Map<String, String> body = Map.of("q", query);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = rest.exchange(URL, HttpMethod.POST, entity, Map.class);

        Map<String, Object> json = response.getBody();

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

            Map<String, Object> classification = (Map<String, Object>) snippet.get("classification");
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
