package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/patents")
public class PatentSearchController {

    private final PatentSearchService service;
    private static final Logger log = LoggerFactory.getLogger(PatentSearchController.class);

    public PatentSearchController(PatentSearchService service) {
        this.service = service;
    }

    @PostMapping("/search")
    public PatentSearchPagedResponse search(@RequestBody PatentSearchRequest request) {

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

    @GetMapping("/test")
    public String test() {
        String result = "";
        try {
            result = result.concat("MaxBotService \t\t\t >>> OK \n");
            PatentSearchResponse raw = service.search("q", "Запрос",
                    5, 0);
            result = result.concat("PatentSearchService \t >>> OK");
        } catch (Exception e) {
            result = result.concat("MaxBotService >>> Fail: ").concat(e.getMessage());
        }
        return result;
    }

}
