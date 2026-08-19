package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.patent.PatentSearchPagedResponse;
import org.maxbot.miniapp.dto.patent.PatentSearchRequest;
import org.maxbot.miniapp.dto.patent.PatentSearchResponse;
import org.maxbot.miniapp.service.PatentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/patents")
public class WebAppController {

    private final PatentService service;
    private static final Logger log = LoggerFactory.getLogger(WebAppController.class);

    public WebAppController(PatentService service) {
        this.service = service;
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
        return service.searchPatents(req)
                .map(resp -> getPatentSearchPagedResponse(req, resp));
    }

}
