package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DateSelectedHandler implements StepHandler {

    private final MaxApiClient apiClient;

    public DateSelectedHandler(MaxApiClient apiClient) {
        this.apiClient = apiClient;
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
//        List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();
//
//        buttons.add(List.of(BotResponse.Button.builder()
//                .type("callback")
//                .text("\uD83D\uDD19 Назад к выбору даты")
//                .payload("BACK")
//                .build()));
//
//
//        return BotResponse.builder()
//                .text("Фильтр по дате установлен \nВведите поисковый запрос:")
//                .attachments(List.of(BotResponse.Attachment.builder()
//                        .type("inline_keyboard")
//                        .payload(BotResponse.InlineKeyboardPayload.builder().buttons(buttons).build())
//                        .build()
//                )).build();

        BotResponse botResponse = BotResponse.builder()
                .text("Фильтр по дате установлен")
                .build();
        apiClient.sendMessage(Integer.parseInt(event.getChatId()), botResponse);
        return botResponse;

    }
}
