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

        System.out.println("Получен запрос: /api/patents/search " + request);
        PatentSearchResponse raw = service.search(
                request.getQuery(),
                request.getQueryMode(),
                request.getLimit(),
                request.getOffset()
        );

        PatentSearchPagedResponse response = new PatentSearchPagedResponse();
        response.setItems(raw.getHits());

        PatentSearchPagedResponse.Pagination pagination =
                new PatentSearchPagedResponse.Pagination();

        int pageSize = request.getLimit();
        int page = (request.getOffset() / pageSize) + 1;

        pagination.setPage(page);
        pagination.setPageSize(pageSize);
        pagination.setTotal(raw.getTotal());
        pagination.setHasNext(request.getOffset() + pageSize < raw.getTotal());

        response.setPagination(pagination);

        return response;
    }

}
