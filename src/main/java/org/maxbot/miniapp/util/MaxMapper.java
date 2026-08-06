package org.maxbot.miniapp.util;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {}

    private static final Map<String, PayloadInfo> PAYLOAD_MAPPING = Map.of(
            "PATENTS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Патенты"),
            "PROM_SAMPLE", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Промобразцы"),
            "MODEL", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Полезные модели"),
            "ARRAYS_PANEL", new PayloadInfo(BotEvents.USER_SELECT_FILTERS, "Фильтры"),
            "DATE_PANEL", new PayloadInfo(BotEvents.USER_INPUT_DATE, "Дата"),
            "BACK", new PayloadInfo(BotEvents.BACK, "Назад")
    );

    public BotEvent toEvent(UpdateDto upd) {
        if (upd == null) return null;

        BotEvent event = new BotEvent();
        event.setUserId(String.valueOf(upd.getUserId()));
        event.setChatId(String.valueOf(upd.getChatId()));

        String updateType = upd.getUpdateType();
        if (updateType == null) return event;

        // 1. ПЕРВЫЙ СТАРТ
        if ("bot_started".equals(updateType) || "message_created".equals(updateType)) {
            event.setType(BotEvents.USER_OPEN_CHAT);
            event.setPayloadDescription("Старт бота");
            return event;
        }

        // 3. НАЖАТИЕ ИНЛАЙН-КНОПКИ
        if ("message_callback".equals(updateType) && upd.getCallback() != null) {
            CallbackDto cb = upd.getCallback();
            String payload = cb.getPayload();
            event.setPayload(payload);

            PayloadInfo info = PAYLOAD_MAPPING.get(payload);
            if (info != null) {
                event.setType(info.eventType());
                event.setPayloadDescription(info.description());
            }
        }

        return event;
    }
}
