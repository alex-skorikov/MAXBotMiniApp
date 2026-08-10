package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DateSelectedHandler  implements StepHandler {

    private final ContextRepository contextRepository;

    public DateSelectedHandler(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();
        UserContext userContext = contextRepository.load(String.valueOf(event.getChatId()));

        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад к выбору даты")
                .payload("BACK")
                .build()));


        return BotResponse.builder()
                .text("Введите поисковый запрос:")
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder().buttons(buttons).build())
                        .build()
                )).build();
    }
}
