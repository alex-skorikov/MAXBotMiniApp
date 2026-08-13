package org.maxbot.miniapp.util;

import org.maxbot.miniapp.core.BotEvent;
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
        if (event == null || event.getText() == null) {
            return false;
        }
        String text = event.getText();
        return text.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    // Изменяем метод инверсии: создаем полноценный Guard
    public Guard<BotStates, BotEvents> negate() {
        return new Guard<BotStates, BotEvents>() {
            @Override
            public boolean evaluate(StateContext<BotStates, BotEvents> context) {
                return !ValidDateGuard.this.evaluate(context);
            }
        };
    }
}


