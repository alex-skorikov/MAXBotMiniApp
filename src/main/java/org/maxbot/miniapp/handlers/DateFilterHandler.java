package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.*;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DateFilterHandler implements StepHandler, Action<BotStates, BotEvents> {
    @Override
    public void execute(StateContext<BotStates, BotEvents> context) {
        UserContext userContext = context.getMessageHeaders().get("userContext", UserContext.class);
        BotEvent botEvent = context.getMessageHeaders().get("event", BotEvent.class);

        BotResponse response = handle(userContext, botEvent);
        context.getExtendedState().getVariables().put("response", response);
    }

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        List<List<BotResponse.Button>> buttons = new java.util.ArrayList<>();

        // Например, быстрые пресеты дат
        buttons.add(List.of(
                BotResponse.Button.builder().type("callback").text("За год").payload("YEAR").build(),
                BotResponse.Button.builder().type("callback").text("За 5 лет").payload("5_YEARS").build()
        ));

        // Нижняя кнопка НАЗАД в меню фильтров
        buttons.add(List.of(BotResponse.Button.builder()
                .type("callback")
                .text("\uD83D\uDD19 Назад в меню параметров")
                .payload("BACK")
                .build()));

        return BotResponse.builder()
                .text("📅 Настройка фильтра даты\nВыберите период или введите дату сообщением:")
                .attachments(List.of(BotResponse.Attachment.builder()
                        .type("inline_keyboard")
                        .payload(BotResponse.InlineKeyboardPayload.builder().buttons(buttons).build())
                        .build()
                )).build();
    }
}


