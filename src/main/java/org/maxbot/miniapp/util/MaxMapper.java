package org.maxbot.miniapp.util;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {}
    private static final Logger log = LoggerFactory.getLogger(MaxMapper.class);
    private final ContextRepository contextRepository;

    public MaxMapper(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    private static final Map<String, PayloadInfo> PAYLOAD_MAPPING = Map.of(
            "PATENTS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Патенты"),
            "PROM_SAMPLE", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Промобразцы"),
            // 🟢 ИСПРАВЛЕНО: Добавили букву S на конце, чтобы совпадало с payload=MODELS из кнопки
            "MODELS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Полезные модели"),
            "ARRAYS_PANEL", new PayloadInfo(BotEvents.USER_SELECT_FILTERS, "Фильтры"),
            "DATE_PANEL", new PayloadInfo(BotEvents.USER_INPUT_DATE, "Дата"),
            "BACK", new PayloadInfo(BotEvents.BACK, "Назад")
    );

    public BotEvent toEvent(UpdateDto upd, int chatId) {
        if (upd == null) return null;

        // Загружаем контекст по валидному chatId, переданному из контроллера
        UserContext userContext = contextRepository.load(String.valueOf(chatId));

        BotEvent event = new BotEvent();

        // 🟢 ИСПРАВЛЕНО: Безопасно вытаскиваем userId, если на кнопке он равен 0
        int validUserId = upd.getUserId() != 0 ? upd.getUserId() : (upd.getCallback() != null && upd.getCallback().getUser() != null ? upd.getCallback().getUser().getUserId() : 0);
        event.setUserId(String.valueOf(validUserId));

        // 🟢 ИСПРАВЛЕНО: Подставляем валидный chatId из аргумента метода, а не upd.getChatId()
        event.setChatId(String.valueOf(chatId));

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
                // Если нажата кнопка НАЗАД
                if (info.eventType().equals(BotEvents.BACK)) {
                    event.setType(BotEvents.BACK);
                    event.setPayloadDescription(info.description());
                } else {
                    // Для всех остальных кнопок (включая MODELS)
                    event.setType(info.eventType());
                    event.setPayloadDescription(info.description());
                }
            }
        }

        log.info("MaxMapper found Event: {}", event);
        return event;
    }
}
