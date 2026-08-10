package org.maxbot.miniapp.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {}

    private final ContextRepository contextRepository;

    private static final Map<String, PayloadInfo> PAYLOAD_MAPPING = Map.ofEntries(
            entry("PATENTS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Патенты")),
            entry("PROM_SAMPLE", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Промобразцы")),
            entry("MODELS", new PayloadInfo(BotEvents.USER_SELECT_BASE, "Полезные модели")),
            entry("DATE_INPUT", new PayloadInfo(BotEvents.USER_INPUT_DATE, "Выбор даты")),
            entry("DATE_SELECTED", new PayloadInfo(BotEvents.USER_SELECTED_DATE, "Дата выбрана")),
            entry("SEARCH_ARRAYS", new PayloadInfo(BotEvents.USER_SEARCH_ARRAY, "Переход к поисковым массивам")),
            entry("CLASSIFIERS", new PayloadInfo(BotEvents.USER_SEARCH_CLASSIFIERS, "Переход к классификаторам")),
            entry("COUNTRY_INPUT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Россия и страны СНГ")),
            entry("RST_INPUT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Минимум РСТ")),
            entry("INDUSTRIAL_INPUT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Промышленные образцы")),
            entry("SMALL_PF_INPUT", new PayloadInfo(BotEvents.USER_SELECT_ARRAY, "Страны с малым ПФ")),
            entry("SEARCH_PATENT", new PayloadInfo(BotEvents.USER_SEARCH_PATENT, "Поиск патентов")),
            entry("BACK", new PayloadInfo(BotEvents.BACK, "Назад"))
    );

    public BotEvent toEvent(UpdateDto upd, int chatId) {
        if (upd == null || upd.getUpdateType() == null) return null;

        UserContext userContext = contextRepository.load(String.valueOf(chatId));
        BotEvent event = initBasicEvent(upd, chatId);

        switch (upd.getUpdateType()) {
            case "bot_started" -> handleBotStarted(event);
            case "bot_stopped" -> handleBotStopped(chatId);
            case "message_callback" -> handleMessageCallback(upd, event, userContext);
            case "message_created" -> handleMessageCreated(upd, event, userContext);
            default -> log.debug("Получен необрабатываемый тип апдейта: {}", upd.getUpdateType());
        }

        log.info(">>> MaxMapper обработал событие. Event: {}, UserContext: {}", event, userContext);
        return event;
    }

    private BotEvent initBasicEvent(UpdateDto upd, int chatId) {
        BotEvent event = new BotEvent();
        int validUserId = upd.getUserId() != 0 ? upd.getUserId() :
                Optional.ofNullable(upd.getCallback())
                        .map(CallbackDto::getUser)
                        .map(org.maxbot.miniapp.dto.bot.SenderDto::getUserId)
                        .orElse(0);

        event.setUserId(String.valueOf(validUserId));
        event.setChatId(String.valueOf(chatId));
        event.setCallbackId(Optional.ofNullable(upd.getCallback()).map(CallbackDto::getCallbackId).orElse(null));
        return event;
    }

    private void handleBotStarted(BotEvent event) {
        event.setType(BotEvents.USER_OPEN_CHAT);
        event.setPayloadDescription("Старт бота");
    }

    private void handleBotStopped(int chatId) {
        contextRepository.delete(String.valueOf(chatId));
    }

    private void handleMessageCallback(UpdateDto upd, BotEvent event, UserContext userContext) {
        if (upd.getCallback() == null) return;

        String payload = upd.getCallback().getPayload();
        event.setPayload(payload);

        PayloadInfo info = PAYLOAD_MAPPING.get(payload);
        if (info == null) return;

        // Бизнес-мутации контекста на основе инлайн-кликов
        boolean needSave = false;
        if (info.eventType() == BotEvents.USER_SELECT_BASE) {
            userContext.setSelectedBase(info.description());
            needSave = true;
        } else if (info.eventType() == BotEvents.USER_SELECT_ARRAY) {
            userContext.setSearchArrays(info.description());
            needSave = true;
        }

        if (needSave) {
            contextRepository.save(userContext);
        }

        event.setType(info.eventType());
        event.setPayloadDescription(info.description());
    }

    private void handleMessageCreated(UpdateDto upd, BotEvent event, UserContext userContext) {
        MessageDto msg = upd.getMessage();
        String text = (msg != null && msg.getBody() != null) ? msg.getBody().getText() : null;
        event.setText(text);

        BotStates currentState = userContext.getState();
        if (currentState == null) return;

        switch (currentState) {
            case FILTER_DATE -> {
                userContext.setDate(text);
                contextRepository.save(userContext);
                event.setType(BotEvents.USER_SELECTED_DATE);
                event.setPayloadDescription("Ввод даты");
            }
            case FILTER_CLASSIFIERS -> {
                userContext.setClassifiers(text);
                contextRepository.save(userContext);
                event.setType(BotEvents.USER_SELECT_CLASSIFIERS);
                event.setPayloadDescription("Ввод классификатора");
            }
            case SEARCH -> {
                userContext.getFilters().put("search_query", text);
                contextRepository.save(userContext);
                event.setType(BotEvents.USER_SEARCH_PATENT);
                event.setPayloadDescription("Ввод поискового запроса");
            }
            default -> log.debug("Текстовое сообщение пропущено для стейта: {}", currentState);
        }
    }
}
