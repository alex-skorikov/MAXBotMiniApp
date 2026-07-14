package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.RospatentClient;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PatentSearchService {

    private final RospatentClient client;

    public PatentSearchService(RospatentClient client) {
        this.client = client;
    }

    public PatentSearchResponse search(
            String query,
            String queryMode,
            Integer limit,
            Integer offset
    ) {
        if ("q".equalsIgnoreCase(queryMode)) {
            return client.searchByQuery(query, limit, offset);
        }

        if ("qn".equalsIgnoreCase(queryMode)) {
            return client.searchByNumber(query);
        }

        throw new IllegalArgumentException("Unknown queryMode: " + queryMode);
    }

}
