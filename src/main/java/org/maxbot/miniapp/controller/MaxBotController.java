package org.maxbot.miniapp.controller;


import org.maxbot.miniapp.client.MaxApiClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.maxbot.miniapp.service.PatentSearchService;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class MaxBotController {


    private final MaxApiClient maxApiClient;
    private final PatentSearchService patentSearchService;

    public MaxBotController(MaxApiClient maxApiClient,
                            PatentSearchService patentSearchService) {
        this.maxApiClient = maxApiClient;
        this.patentSearchService = patentSearchService;
        System.out.println(">>> MaxBotController LOADED");

    }

    @PostMapping("/max/message")
    public Object handleMessage(@RequestBody String message) {
        return patentSearchService.processIncomingMessage(message);
    }

    @PostMapping("/webhook")
    public Map<String, Object> handleUpdate(@RequestBody Map<String, Object> update) {
        return Map.of("ok", true);
    }
}
