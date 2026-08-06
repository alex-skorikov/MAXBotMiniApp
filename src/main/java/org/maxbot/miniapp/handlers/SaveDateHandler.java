package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.*;
import org.springframework.stereotype.Component;

@Component
public class SaveDateHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        ctx.getFilters().put("date", event.getText());
//        ctx.setState(BotStates.DONE);

        return null;
    }
}

