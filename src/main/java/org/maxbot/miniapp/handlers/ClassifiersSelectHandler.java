package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository;
import org.springframework.stereotype.Component;

@Component
public class ClassifiersSelectHandler implements StepHandler {

    private final ContextRepository contextRepository;

    public ClassifiersSelectHandler(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {

        ctx.setDate(event.getPayloadDescription());
        contextRepository.save(ctx);

        return BotResponse.builder()
                .notify(false)
                .text("Классификатор выбран")
                .build();
    }
}
