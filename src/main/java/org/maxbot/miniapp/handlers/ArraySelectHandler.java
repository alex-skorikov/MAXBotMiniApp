package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository;
import org.springframework.stereotype.Component;

@Component
public class ArraySelectHandler implements StepHandler {

    private final ContextRepository contextRepository;

    public ArraySelectHandler(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {

        ctx.setSearchArrays(event.getPayloadDescription());
        contextRepository.save(ctx);

        return BotResponse.builder()
                .text("Поисковый массив выбран")
                .build();
    }
}
