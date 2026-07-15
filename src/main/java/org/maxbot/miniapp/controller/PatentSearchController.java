package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/patents")
public class PatentSearchController {

    private final PatentSearchService service;
    private static final Logger log = LoggerFactory.getLogger(PatentSearchController.class);

    public PatentSearchController(PatentSearchService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public PatentSearchPagedResponse search(@RequestBody PatentSearchRequest request) throws IOException {
        log.info(">>> POST PatentSearchController /api/patents/search called: {}", request);

        PatentSearchResponse raw = service.search(
                request.getQuery(),
                request.getQueryMode(),
                request.getLimit(),
                request.getOffset()
        );

        log.info(">>> RESPONSE PatentSearch size: {}", raw.getHits().size());

        PatentSearchPagedResponse response = getPatentSearchPagedResponse(request, raw);

        log.info(">>> RESPONSE PatentSearchController /api/patents/search: {}", response);
        return response;
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

    @GetMapping("/test")
    public String test() {
        log.info(">>> PatentSearchService /api/patents GET test");
        return "OK";
    }

}
