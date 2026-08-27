package org.maxbot.miniapp.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {
    }

    private final ContextRepository contextRepository;
    private final PatentService patentService;

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
            entry("START_SEARCH", new PayloadInfo(BotEvents.USER_PROCEED_TO_SEARCH, "Запуск поиска патентов")),
            entry("SEARCH_PATENT", new PayloadInfo(BotEvents.USER_SEARCH_PATENT, "Поиск патентов")),
            entry("BACK", new PayloadInfo(BotEvents.BACK, "Назад"))
    );

    public BotEvent toEvent(UpdateDto upd, int chatId, int userId) {
        if (upd == null || upd.getUpdateType() == null) return null;

        // Инициализируем событие, жестко прописывая правильные ID
        BotEvent event = new BotEvent();
        event.setUserId(String.valueOf(userId));
        event.setChatId(String.valueOf(chatId));
        event.setCallbackId(Optional.ofNullable(upd.getCallback()).map(CallbackDto::getCallbackId).orElse(null));

        UserContext userContext = contextRepository.load(String.valueOf(userId));
        if (userContext == null) {
            userContext = new UserContext();
            userContext.setUserId(userId);
        }
        userContext.setChatId(String.valueOf(chatId));

        switch (upd.getUpdateType()) {
            case "bot_started" -> handleBotStarted(event);
            case "bot_stopped" -> handleBotStopped(userId);
            case "message_callback" -> handleMessageCallback(upd, event, userContext);
            case "message_created" -> handleMessageCreated(upd, event, userContext);
            default -> log.debug("Получен необрабатываемый тип апдейта: {}", upd.getUpdateType());
        }

        log.info("🔄 >>> MaxMapper обработал событие: \n⚡ >>> Event: {}, \uD83D\uDC64 >>> UserContext: {}", event, userContext);
        return event;
    }

    private void handleBotStarted(BotEvent event) {
        event.setType(BotEvents.USER_OPEN_CHAT);
        event.setPayloadDescription("Старт бота");
    }

    private void handleBotStopped(int userId) {
        contextRepository.delete(String.valueOf(userId));
    }

    private void handleMessageCallback(UpdateDto upd, BotEvent event, UserContext userContext) {
        if (upd.getCallback() == null) return;

        String payload = upd.getCallback().getPayload();
        event.setPayload(payload);

        // Динамические обработчики (пагинация и просмотр)
        if (payload != null && payload.startsWith("DOC_VIEW_")) {
            String docId = payload.substring("DOC_VIEW_".length());
            patentService.sendSinglePatentCardAsync(Integer.parseInt(event.getChatId()), docId, userContext).subscribe();
            event.setType(null);
            event.setPayloadDescription("Изолированный просмотр документа " + docId);
            return;
        }

        if ("SEARCH_NEXT_PAGE".equals(payload) || "SEARCH_PREV_PAGE".equals(payload)) {
            int currentOffset = userContext.getOffset();
            int limit = (userContext.getLimit() > 0) ? userContext.getLimit() : 5;

            if ("SEARCH_NEXT_PAGE".equals(payload)) {
                userContext.setOffset(currentOffset + limit);
            } else {
                userContext.setOffset(Math.max(0, currentOffset - limit));
            }
            contextRepository.save(userContext);
            event.setType(BotEvents.USER_SEARCH_PATENT);
            return;
        }

        PayloadInfo info = PAYLOAD_MAPPING.get(payload);
        if (info == null) {
            if ("BACK_TO_START".equals(payload)) {
                UserContext freshCtx = contextRepository.load(String.valueOf(userContext.getUserId()));
                if (freshCtx == null) {
                    freshCtx = userContext;
                }

                freshCtx.setSelectedBase(null);
                freshCtx.setDatasetArrays(null);
                freshCtx.setDatasetName(null);
                freshCtx.setHits(null);
                freshCtx.setDate(null);
                freshCtx.setClassifiers(null);
                freshCtx.setSearchQuery(null);
                freshCtx.setOffset(0);
                contextRepository.save(freshCtx);

                log.info("🔄 Все фильтры поиска успешно сброшены в Redis для чата {}", userContext.getChatId());

                event.setType(BotEvents.BACK_TO_START);
                event.setCallbackId(null);
                event.setPayloadDescription("Сброс фильтров и возврат в начало");
                return;
            }
            return;
        }

        UserContext freshContext = contextRepository.load(String.valueOf(userContext.getUserId()));
        if (freshContext == null) {
            freshContext = userContext;
        }
        if (info.eventType() == BotEvents.USER_SELECT_BASE) {
            freshContext.setSelectedBase(info.description);
            freshContext.setOffset(0);
            contextRepository.save(freshContext); // Пишем ТОЛЬКО базу
        } else if (info.eventType() == BotEvents.USER_SELECT_ARRAY) {
            List<String> arrays = patentService.getDataSetArrayByDescription(info.description());
            freshContext.setDatasetName(info.description);
            freshContext.setDatasetArrays(arrays);
            contextRepository.save(freshContext); // Пишем ТОЛЬКО массив
        }

        event.setType(info.eventType());
        event.setPayloadDescription(info.description());
    }


    private void handleMessageCreated(UpdateDto upd, BotEvent event, UserContext userContext) {
        MessageDto msg = upd.getMessage();
        String text = (msg != null && msg.getBody() != null) ? msg.getBody().getText() : null;
        event.setText(text);

        BotStates currentState = userContext.getState();

        if (currentState == null) {
            log.warn("⚠️ [MAPPER] В UserContext отсутствует текущий стейт. Проверяем ветку по умолчанию.");
            event.setType(BotEvents.BACK_TO_START);
            event.setCallbackId(null);
            return;
        }

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
            case SELECT_DATE -> {
                // 1. Сохраняем поисковый запрос пользователя в карту фильтров в Redis
                userContext.setSearchQuery(text);
                contextRepository.save(userContext);

                // 2. Инициализируем ивент для стейт-машины, чтобы она переключилась в SEARCH
                event.setType(BotEvents.USER_SEARCH_PATENT);
                event.setPayloadDescription("Ввод поискового запроса: " + text);
            }
            case SEARCH -> {
                userContext.setSearchQuery(text);
                contextRepository.save(userContext);
                event.setType(BotEvents.USER_SEARCH_PATENT);
                event.setPayloadDescription("Ввод поискового запроса");
            }
            default -> {
                // На любое сообщение отправляем приветственное
                event.setType(BotEvents.BACK_TO_START);
                event.setCallbackId(null);
            }
        }
    }

}
