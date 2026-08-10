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

import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Component
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {
    }

    private static final Logger log = LoggerFactory.getLogger(MaxMapper.class);
    private final ContextRepository contextRepository;

    public MaxMapper(ContextRepository contextRepository) {
        this.contextRepository = contextRepository;
    }

    private static final Map<String, PayloadInfo> PAYLOAD_MAPPING = Map.ofEntries(
            entry("PATENTS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Патенты")),
            entry("PROM_SAMPLE", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Промобразцы")),
            entry("MODELS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Полезные модели")),
            entry("DATE_INPUT", new PayloadInfo(BotEvents.USER_INPUT_DATE, "Выбор даты")),
            entry("DATE_SELECTED", new PayloadInfo(BotEvents.USER_SELECTED_DATE, "Дата выбрана")),

            entry("SEARCH_ARRAYS", new PayloadInfo(BotEvents.USER_SEARCH_ARRAY, "Переход к поисковым массивам")),
            entry("CLASSIFIERS", new PayloadInfo(BotEvents.USER_SEARCH_CLASSIFIERS, "Переход к классификаторам")),

            entry("COUNTRY_INPUT", new PayloadInfo(BotEvents.USER_SEARCH_ARRAY, "Выбор массива Россия и страны СНГ")),
            entry("RST_INPUT", new PayloadInfo(BotEvents.USER_SEARCH_ARRAY, "Выбор массива Минимум РСТ")),
            entry("INDUSTRIAL_INPUT", new PayloadInfo(BotEvents.USER_SEARCH_ARRAY, "Выбор массива Промышленные образцы")),
            entry("SMALL_PF_INPUT", new PayloadInfo(BotEvents.USER_SEARCH_ARRAY, "Выбор массива Страны с малым ПФ")),

            entry("COUNTRY_SELECT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Россия и страны СНГ")),
            entry("RST_SELECT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Минимум РСТ")),
            entry("INDUSTRIAL_SELECT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Промышленные образцы")),
            entry("SMALL_PF_SELECT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Страны с малым ПФ")),

            entry("SEARCH_PATENT", new PayloadInfo(BotEvents.USER_SEARCH_PATENT, "Поиск патентов")),
            entry("BACK", new PayloadInfo(BotEvents.BACK, "Назад"))
    );

    public BotEvent toEvent(UpdateDto upd, int chatId) {
        if (upd == null) return null;

        // Загружаем контекст по валидному chatId, переданному из контроллера
        UserContext userContext = contextRepository.load(String.valueOf(chatId));

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

        // Stop bot - удаляем контекст пользователя
        if ("bot_stopped".equals(updateType)) {
            contextRepository.delete(String.valueOf(chatId));
            return event;
        }

        // 3. НАЖАТИЕ ИНЛАЙН-КНОПКИ
        if ("message_callback".equals(updateType) && upd.getCallback() != null) {
            CallbackDto cb = upd.getCallback();
            String payload = cb.getPayload();
            event.setPayload(payload);

            PayloadInfo info = PAYLOAD_MAPPING.get(payload);

            if (info != null) {
                boolean needSave = false;

                // Автосохранение выбранной базы
                if (info.eventType() == BotEvents.USER_SELECT_BASE) {
                    userContext.setSelectedBase(info.description());
                    needSave = true;
                }
                // Автосохранение выбранного массива
                if (info.eventType() == BotEvents.USER_SELECT_ARRAY) {
                    userContext.setSearchArrays(info.description());
                    needSave = true;
                }
                if (needSave) {
                    contextRepository.save(userContext);
                }

                event.setType(info.eventType());
                event.setPayloadDescription(info.description());
            }
        }

        if ("message_created".equals(updateType) && BotStates.FILTER_DATE.equals(userContext.getState())) {
            MessageDto msg = upd.getMessage();
            String text = msg != null && msg.getBody() != null ? msg.getBody().getText() : null;

            userContext.setDate(text);
            contextRepository.save(userContext);

            event.setText(text);
            event.setType(BotEvents.USER_SELECTED_DATE);
            event.setPayloadDescription("Ввод даты");

            log.info(">>> MaxMapper found Event: {}", event);
            log.info(">>> MaxMapper found UserContext: {}", userContext);
            return event;
        }

        if ("message_created".equals(updateType) && BotStates.FILTER_CLASSIFIERS.equals(userContext.getState())) {
            MessageDto msg = upd.getMessage();
            String text = msg != null && msg.getBody() != null ? msg.getBody().getText() : null;

            userContext.setClassifiers(text);
            contextRepository.save(userContext);

            event.setText(text);
            event.setType(BotEvents.USER_SELECT_CLASSIFIERS);
            event.setPayloadDescription("Ввод поискового запроса");

            log.info(">>> MaxMapper found Event: {}", event);
            log.info(">>> MaxMapper found UserContext: {}", userContext);
            return event;
        }

        if ("message_created".equals(updateType) && BotStates.SEARCH.equals(userContext.getState())) {
            MessageDto msg = upd.getMessage();
            String text = msg != null && msg.getBody() != null ? msg.getBody().getText() : null;

            if (userContext.getFilters() != null) {
                userContext.getFilters().put("search_query", text);
                contextRepository.save(userContext);
            }

            event.setText(text);
            event.setType(BotEvents.USER_SEARCH_PATENT);
            event.setPayloadDescription("Ввод поискового запроса");

            log.info(">>> MaxMapper found Event: {}", event);
            log.info(">>> MaxMapper found UserContext: {}", userContext);
            return event;
        }

        log.info(">>> MaxMapper found Event: {}", event);
        log.info(">>> MaxMapper found UserContext: {}", userContext);
        return event;
    }
}
