package org.maxbot.miniapp.service;

import org.maxbot.miniapp.client.RospatentClient;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.springframework.stereotype.Service;

@Service
public class PatentSearchService {

    private final RospatentClient client;

    public PatentSearchService(RospatentClient client) {
        this.client = client;
    }

    public PatentSearchResponse search(String query) {
        return client.search(query);
    }
}
