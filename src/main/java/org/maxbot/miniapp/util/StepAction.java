package org.maxbot.miniapp.util;

import lombok.RequiredArgsConstructor;
import org.maxbot.miniapp.core.*;
import org.maxbot.miniapp.handlers.StepHandler;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action: единая точка вызова хендлеров
 */
@Component
@RequiredArgsConstructor
public class StepAction implements Action<BotStates, BotEvents> {

    private final HandlerRegistry handlerRegistry;

    @Override
    public void execute(StateContext<BotStates, BotEvents> context) {

        if (context.getTarget() == null) {
            return;
        }
        BotStates targetState = context.getTarget().getId();

        BotEvent event = (BotEvent) context.getMessageHeader("event");
        UserContext userContext = (UserContext) context.getMessageHeader("userContext");

        StepHandler handler = handlerRegistry.getHandler(targetState);

        if (handler == null) {
            return;
        }

        BotResponse response = handler.handle(userContext, event);

        if (response != null) {
            context.getExtendedState().getVariables().put("response", response);
        }
    }

}

