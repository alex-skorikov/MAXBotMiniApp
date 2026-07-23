package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.RospatentClient;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class PatentSearchService {

    private final RospatentClient client;

    public PatentSearchService(RospatentClient client) {
        this.client = client;
    }

    public Mono<PatentSearchResponse> searchReactive(String queryMode, String query, Integer limit, Integer offset) {
        return client.searchReactive(queryMode, query, limit, offset);
    }
}
