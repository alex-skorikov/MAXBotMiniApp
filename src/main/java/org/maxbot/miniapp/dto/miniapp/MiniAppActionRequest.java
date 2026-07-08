package org.maxbot.miniapp.dto.miniapp;

import lombok.Data;

import java.util.Map;

@Data
public class MiniAppActionRequest {

    private String action; // "search"
    private Map<String, String> input; // {"query": "нейросеть обработка изображений"}
    private String userId;
    private String sessionId;
}

