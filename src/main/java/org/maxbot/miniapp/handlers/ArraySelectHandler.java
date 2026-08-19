package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.springframework.stereotype.Component;

@Component
public class ArraySelectHandler implements StepHandler {

    private final ContextRepository contextRepository;
    private final PatentService patentService;

    public ArraySelectHandler(ContextRepository contextRepository,
                              PatentService patentService) {
        this.contextRepository = contextRepository;
        this.patentService = patentService;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {

//        ctx.setSearchArrayName(patentService.getSearchArrayName(event.getPayload()));
        contextRepository.save(ctx);

        return BotResponse.builder()
                .text("Поисковый массив выбран")
                .notify(false)
                .build();
    }
}
