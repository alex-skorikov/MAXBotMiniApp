package org.maxbot.miniapp.handlers;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.core.UserContext;

public interface StepHandler {
    BotResponse handle(UserContext ctx, BotEvent event);
}

