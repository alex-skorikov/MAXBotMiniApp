package org.maxbot.miniapp.controller;

import org.maxbot.miniapp.dto.miniapp.MiniAppActionRequest;
import org.maxbot.miniapp.dto.miniapp.MiniAppActionResponse;
import org.maxbot.miniapp.dto.miniapp.MiniAppInitRequest;
import org.maxbot.miniapp.dto.miniapp.MiniAppInitResponse;
import org.maxbot.miniapp.service.MaxMiniAppService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/miniapp")
public class MaxMiniAppController {

    private final MaxMiniAppService service;

    public MaxMiniAppController(MaxMiniAppService service) {
        this.service = service;
    }

    @PostMapping("/init")
    public MiniAppInitResponse init(@RequestBody MiniAppInitRequest req) {
        return service.init(req);
    }

    @PostMapping("/action")
    public MiniAppActionResponse action(@RequestBody MiniAppActionRequest req) {
        return service.handleAction(req);
    }
}
