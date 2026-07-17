package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Objects;

@RestController
@RequestMapping("/api/patents")
public class PatentSearchController {

    private final PatentSearchService service;
    private static final Logger log = LoggerFactory.getLogger(PatentSearchController.class);

    public PatentSearchController(PatentSearchService service) {
        this.service = service;
    }

    @PostMapping("/search-sync")
    public PatentSearchPagedResponse search_sync(@RequestBody PatentSearchRequest request) {

        PatentSearchResponse raw = service.search(
                request.getQueryMode(),
                request.getQuery(),
                request.getLimit(),
                request.getOffset()
        );

        return getPatentSearchPagedResponse(request, raw);
    }

    private static PatentSearchPagedResponse getPatentSearchPagedResponse(PatentSearchRequest request,
                                                                          PatentSearchResponse raw) {
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

    @PostMapping("/search")
    public Mono<PatentSearchPagedResponse> search(@RequestBody PatentSearchRequest req) {
        return service.searchReactive(req.getQueryMode(), req.getQuery(), req.getLimit(), req.getOffset())
                .map(resp -> getPatentSearchPagedResponse(req, resp));
    }

    @GetMapping("/test")
    public Mono<String> test() {
        return service.searchReactive("q", "Запрос", 5, 1)
                .map(resp -> "MaxBotService \t\t\t >>> OK\n" +
                        "PatentSearchService \t >>> OK")
                .onErrorResume(e -> Mono.just(
                        "MaxBotService >>> Fail: " + e.getMessage()
                ));
    }
}
