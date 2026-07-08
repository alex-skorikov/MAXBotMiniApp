package org.maxbot.miniapp.client;

import org.maxbot.miniapp.dto.patent.PatentHit;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RospatentClient {

    private final RestTemplate rest = new RestTemplate();
    private static final String URL = "https://searchplatform.rospatent.gov.ru/patsearch/v0.2/search";

    public PatentSearchResponse search(String query) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("q", query);
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<Map> response = rest.exchange(URL, HttpMethod.POST, entity, Map.class);

        Map<String, Object> json = response.getBody();

        int total = (int) json.get("total");
        int available = (int) json.get("available");

        List<Map<String, Object>> rawHits = (List<Map<String, Object>>) json.get("hits");
        List<PatentHit> hits = new ArrayList<>();

        for (Map<String, Object> raw : rawHits) {

            Map<String, Object> common = (Map<String, Object>) raw.get("common");
            Map<String, Object> biblio = (Map<String, Object>) raw.get("biblio");
            Map<String, Object> ru = biblio != null ? (Map<String, Object>) biblio.get("ru") : null;
            Map<String, Object> snippet = (Map<String, Object>) raw.get("snippet");

            PatentHit hit = new PatentHit();
            hit.setId((String) raw.get("id"));
            hit.setTitle(ru != null ? (String) ru.get("title") : (String) snippet.get("title"));
            hit.setApplicant(snippet != null ? (String) snippet.get("applicant") : null);
            hit.setInventor(snippet != null ? (String) snippet.get("inventor") : null);
            hit.setIpc(snippet != null ? (String) snippet.get("classification") : null);
            hit.setDescription(snippet != null ? (String) snippet.get("description") : null);

            hits.add(hit);
        }

        PatentSearchResponse result = new PatentSearchResponse();
        result.setTotal(total);
        result.setAvailable(available);
        result.setHits(hits);

        return result;
    }
}
