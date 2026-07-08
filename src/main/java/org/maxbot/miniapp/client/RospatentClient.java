package org.maxbot.miniapp.client;

import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class RospatentClient {

    @Value("${rospatent.token}")
    private String token;

    private final RestTemplate rest = new RestTemplate();

    public PatentSearchResponse search(String query) {

        String url = "https://searchplatform.rospatent.gov.ru/patsearch/v0.2/search?q="
                + URLEncoder.encode(query, StandardCharsets.UTF_8);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<PatentSearchResponse> response =
                rest.exchange(url, HttpMethod.GET, entity, PatentSearchResponse.class);

        return response.getBody();
    }
}

