package org.maxbot.miniapp.util;

import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.statemachine.BotStates;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

@Component
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {
    }

    private static final Logger log = LoggerFactory.getLogger(MaxMapper.class);
    private final ContextRepository contextRepository;

    public MaxMapper(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    private static final Map<String, PayloadInfo> PAYLOAD_MAPPING = Map.of(
            "PATENTS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Патенты"),
            "PROM_SAMPLE", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Промобразцы"),
            "MODELS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Полезные модели"),
            "DATE_INPUT", new PayloadInfo(BotEvents.USER_INPUT_DATE, "Выбор даты"),
            "DATE_SELECTED", new PayloadInfo(BotEvents.USER_SELECTED_DATE, "Дата выбрана"),


            "SEARCH_PATENT", new PayloadInfo(BotEvents.USER_SEARCH_PATENT, "Поиск патентов"),


//            "SEARCH_ARRAYS", new PayloadInfo(BotEvents.USER_SELECT_SEARCH_ARRAY, "Поисковые массивы"),
//            "CLASSIFIERS", new PayloadInfo(BotEvents.USER_SELECT_CLASSIFIERS, "Классификаторы"),


            "BACK", new PayloadInfo(BotEvents.BACK, "Назад")
    );

    public BotEvent toEvent(UpdateDto upd, int chatId) {
        if (upd == null) return null;

        // Загружаем контекст по валидному chatId, переданному из контроллера
        UserContext userContext = contextRepository.load(String.valueOf(chatId));
        log.info("MaxMapper found UserContext: {}", userContext);


        BotEvent event = new BotEvent();

        int validUserId = upd.getUserId() != 0 ? upd.getUserId() : (upd.getCallback() != null && upd.getCallback().getUser() != null ? upd.getCallback().getUser().getUserId() : 0);
        event.setUserId(String.valueOf(validUserId));

        event.setChatId(String.valueOf(chatId));

        // Сохраняем callbackId для выбора варианта ответа
        String callbackId = Optional.ofNullable(upd)
                .map(UpdateDto::getCallback)
                .map(CallbackDto::getCallbackId)
                .orElse(null);

        event.setCallbackId(callbackId);

        String updateType = upd.getUpdateType();
        if (updateType == null) return event;

        // 1. ПЕРВЫЙ СТАРТ
        if ("bot_started".equals(updateType)) {
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
                    // Для всех остальных кнопок
                    event.setType(info.eventType());
                    event.setPayloadDescription(info.description());
                }
            }
        }

        if ("message_created".equals(updateType) && userContext.getState().equals(BotStates.FILTER_DATE)) {
            MessageDto msg = upd.getMessage();
            String text = msg.getBody().getText();

            Map<String, Object> filters = userContext.getFilters();
            filters.put("data", upd.getMessage().getBody().getText());
            userContext.setFilters(filters);

            event.setText(text);
            event.setType(BotEvents.USER_SELECTED_DATE);
            event.setPayloadDescription("Ввод даты");
            return event;
        }

        if ("message_created".equals(updateType) && userContext.getState().equals(BotStates.SELECT_DATE)) {
            MessageDto msg = upd.getMessage();
            String text = msg.getBody().getText();

            Map<String, Object> filters = userContext.getFilters();
            filters.put("search_string", upd.getMessage().getBody().getText());
            userContext.setFilters(filters);

            event.setText(text);
            event.setType(BotEvents.USER_SEARCH_PATENT);
            event.setPayloadDescription("Ввод поискового запроса");
            return event;
        }


        log.info("MaxMapper found Event: {}", event);
        return event;
    }
}
