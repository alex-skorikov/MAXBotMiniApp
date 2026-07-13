package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/patents")
public class PatentSearchController {

    private final PatentSearchService service;

    public PatentSearchController(PatentSearchService service) {
        this.service = service;
    }

//    @GetMapping("/search")
//    public PatentSearchResponse search(@RequestParam String q) {
//        System.out.println("Получен запрос: /api/patents/search " + q);
//        return service.search(q);
//    }

    @PostMapping("/search")
    public PatentSearchPagedResponse search(@RequestBody PatentSearchRequest request) {

        PatentSearchResponse raw = service.search(
                request.getQuery(),
                request.getQueryMode(),
                request.getPage(),
                request.getPageSize(),
                request.getIncludeFacets()
        );

        PatentSearchPagedResponse response = new PatentSearchPagedResponse();
        response.setItems(raw.getHits());

        PatentSearchPagedResponse.Pagination pagination =
                new PatentSearchPagedResponse.Pagination();

        pagination.setPage(request.getPage());
        pagination.setPageSize(request.getPageSize());
        pagination.setTotal(raw.getTotal());
        pagination.setHasNext(request.getPage() * request.getPageSize() < raw.getTotal());

        response.setPagination(pagination);

        return response;
    }


}
