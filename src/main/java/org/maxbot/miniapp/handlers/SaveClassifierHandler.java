package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.*;
import org.springframework.stereotype.Component;

@Component
public class SaveClassifierHandler implements StepHandler {

    @Override
    public BotResponse handle(UserContext ctx, BotEvent event) {
        ctx.getFilters().put("classifier", event.getText());
//        ctx.setState(BotStates.DONE);

        return null;
    }
}
