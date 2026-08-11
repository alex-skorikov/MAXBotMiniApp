package org.maxbot.miniapp.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.client.MaxApiClient;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.BotResponse;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.repository.ContextRepository;
import org.maxbot.miniapp.service.PatentCardService;
import org.maxbot.miniapp.service.PatentSearchService;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.stereotype.Component;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.Optional;

import static java.util.Map.entry;

@Slf4j
@Component
@RequiredArgsConstructor
public class MaxMapper {

    private record PayloadInfo(BotEvents eventType, String description) {
    }
    private final MaxApiClient maxApiClient;
    private final ContextRepository contextRepository;
    private final PatentSearchService patentSearchService;

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

        // Динамические обработчики (пагинация и просмотр)
        if (payload != null && payload.startsWith("DOC_VIEW_")) {
            String docId = payload.substring("DOC_VIEW_".length());
            event.setType(null);
            event.setPayloadDescription("Просмотр документа " + docId);
            sendSinglePatentCardAsync(Integer.parseInt(event.getChatId()), docId);
            return;
        }

        if ("SEARCH_NEXT_PAGE".equals(payload) || "SEARCH_PREV_PAGE".equals(payload)) {
            int currentOffset = userContext.getSearchOffset();
            int limit = (userContext.getSearchLimit() > 0) ? userContext.getSearchLimit() : 5;

            if ("SEARCH_NEXT_PAGE".equals(payload)) {
                userContext.setSearchOffset(currentOffset + limit);
            } else {
                userContext.setSearchOffset(Math.max(0, currentOffset - limit));
            }
            contextRepository.save(userContext);
            event.setType(BotEvents.USER_SEARCH_PATENT);
            return;
        }

        PayloadInfo info = PAYLOAD_MAPPING.get(payload);
        if (info == null) {
            if ("BACK_TO_START".equals(payload)) {
                contextRepository.delete(String.valueOf(userContext.getChatId()));
                event.setType(BotEvents.BACK_TO_START);
                return;
            }
            return;
        }

        // =========================================================================
        // ИСПРАВЛЕНИЕ БАГА: Перед сохранением ПОВТОРНО перечитываем актуальный контекст из Redis,
        // чтобы не затереть данные, если стейт-машина изменила контекст в параллельном потоке.
        // =========================================================================
        UserContext freshContext = contextRepository.load(String.valueOf(userContext.getChatId()));
        if (freshContext == null) {
            freshContext = userContext;
        }

        if (info.eventType() == BotEvents.USER_SELECT_BASE) {
            freshContext.setSelectedBase(info.description());
            freshContext.setSearchOffset(0);
            contextRepository.save(freshContext); // Пишем ТОЛЬКО базу
        } else if (info.eventType() == BotEvents.USER_SELECT_ARRAY) {
            freshContext.setSearchArrays(info.description());
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
            case SELECT_DATE -> {
                // 1. Сохраняем поисковый запрос пользователя в карту фильтров в Redis
                userContext.setSearchQuery(text);
                contextRepository.save(userContext);

                // 2. Инициализируем ивент для стейт-машины, чтобы она переключилась в SEARCH
                event.setType(BotEvents.USER_SEARCH_PATENT); // Используем наш ивент
                event.setPayloadDescription("Ввод поискового запроса: " + text);
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

    // Метод асинхронного извлечения данных конкретного патента без изменения стейта (Шаги 53-59)
    private void sendSinglePatentCardAsync(int chatId, String docId) {
        // Шаги 53-55: GET /docs/{id}
        patentSearchService.searchReactive("id", docId, 1, 0)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(searchResponse -> {
                    if (searchResponse == null || searchResponse.getHits() == null || searchResponse.getHits().isEmpty()) {
                        return maxApiClient.sendMessage(chatId, BotResponse.builder()
                                .text("❌ Не удалось загрузить информацию по документу " + docId)
                                .build());
                    }

                    var hit = searchResponse.getHits().get(0);

                    // Экранируем ID для сборки полностью валидного адреса
                    String encodedId = java.net.URLEncoder.encode(hit.getId(), java.nio.charset.StandardCharsets.UTF_8);
                    String patentUrl = "https://rospatent.gov.ru" + encodedId;

                    // Шаг 56-58: Название, авторы, дата, МПК
                    // Шаг 59: Прямая текстовая ссылка (Платформа гарантированно сделает её кликабельной без ошибок 400)
                    String formattedText = PatentCardService.formatPatentCard(hit) +
                            "\n\n🔗 Ссылка на оригинал патента:\n" + patentUrl + "\n" +
                            "— — — — — — — — — — — — — — —";

                    BotResponse cardResponse = BotResponse.builder()
                            .text(formattedText)
                            .build(); // Отправляем без блока инлайн-кнопок

                    return maxApiClient.sendMessage(chatId, cardResponse);
                })
                .doOnError(err -> log.error("Критическая ошибка при загрузке документа {}", docId, err))
                .subscribe();
    }


}
