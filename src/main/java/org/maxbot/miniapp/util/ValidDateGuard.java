package org.maxbot.miniapp.util;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.guard.Guard;
import org.springframework.stereotype.Component;

@Component
public class ValidDateGuard implements Guard<BotStates, BotEvents> {

    @Override
    public boolean evaluate(StateContext<BotStates, BotEvents> context) {
        BotEvent event = (BotEvent) context.getMessageHeader("event");
        UserContext userContext = (UserContext) context.getMessageHeader("userContext");

        // Если нет события или текста — переход отклонен
        if (event == null || event.getText() == null) {
            return false;
        }

        String text = event.getText();

        // Примитивная проверка формата YYYY-MM-DD
        if (text.matches("\\d{4}-\\d{2}-\\d{2}")) {

            if (userContext != null && userContext.getFilters() != null) {
                userContext.getFilters().put("date", text);
            }

            return true;
        }

        return false;
    }

    public Guard<BotStates, BotEvents> negate() {
        return ctx -> !evaluate(ctx);
    }
}


