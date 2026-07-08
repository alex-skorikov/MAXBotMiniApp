package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patents")
public class PatentSearchController {

    private final PatentSearchService service;

    public PatentSearchController(PatentSearchService service) {
        this.service = service;
    }

    @GetMapping("/search")
    public PatentSearchResponse search(@RequestParam String q) {
        System.out.println("Получен запрос: /api/patents/search " + q);
        return service.search(q);
    }
}
