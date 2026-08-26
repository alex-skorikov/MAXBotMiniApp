package org.maxbot.miniapp.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.maxbot.miniapp.core.BotEvent;
import org.maxbot.miniapp.core.UserContext;
import org.maxbot.miniapp.dto.bot.BodyDto;
import org.maxbot.miniapp.dto.bot.CallbackDto;
import org.maxbot.miniapp.dto.bot.MessageDto;
import org.maxbot.miniapp.dto.bot.SenderDto;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import org.maxbot.miniapp.repository.HashMapContextRepository;
import org.maxbot.miniapp.service.PatentService;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.mockito.Mockito;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;


class MaxMapperTest {

    private HashMapContextRepository repository;
    private PatentService patentService;
    private MaxMapper maxMapper;

    @BeforeEach
    void setUp() {
        this.repository = new HashMapContextRepository();
        this.patentService = Mockito.mock(PatentService.class);
        this.maxMapper = new MaxMapper(repository, patentService);
    }

    @Test
    void toEventReturnsNullWhenUpdateOrTypeIsNull() {
        assertNull(maxMapper.toEvent(null, 123, 1));

        UpdateDto emptyUpdate = new UpdateDto();
        assertNull(maxMapper.toEvent(emptyUpdate, 123, 1));
    }

    @Test
    void handleBotStartedSuccess() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("bot_started");
        update.setUserId(777);
        update.setChatId(123);
        BotEvent event = maxMapper.toEvent(update, 123, 777);
        assertNotNull(event);
        assertEquals("777", event.getUserId());
        assertEquals("123", event.getChatId());
        assertEquals(BotEvents.USER_OPEN_CHAT, event.getType());
        assertEquals("Старт бота", event.getPayloadDescription());

    }

    @Test
    void handleBotStoppedSuccess() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("bot_stopped");
        UserContext ctx = repository.load("123");
        repository.save(ctx);
        BotEvent event = maxMapper.toEvent(update, 123, 123);
        assertNotNull(event);
        UserContext deletedCtx = repository.load("123");
        assertNull(deletedCtx.getSelectedBase());
    }

    @Test
    void handleMessageCallbackWithPaginationNext() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_callback");

        CallbackDto callback = new CallbackDto();
        callback.setPayload("SEARCH_NEXT_PAGE");

        SenderDto sender = new SenderDto();
        sender.setUserId(888);
        callback.setUser(sender);
        update.setCallback(callback);

        UserContext userContext = repository.load("123");
        userContext.setOffset(10);
        userContext.setLimit(5);
        repository.save(userContext);
        BotEvent event = maxMapper.toEvent(update, 123, 123);
        assertNotNull(event);
        assertEquals(BotEvents.USER_SEARCH_PATENT, event.getType());

        UserContext updatedCtx = repository.load("123");
        assertEquals(15, updatedCtx.getOffset());

    }

    @Test
    void handleMessageCallbackWithBackToStart() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_callback");

        CallbackDto callback = new CallbackDto();
        callback.setPayload("BACK_TO_START");
        update.setCallback(callback);

        UserContext userContext = repository.load("123");
        userContext.setChatId("123"); // 🔥 Защита от NumberFormatException
        userContext.setUserId(123); // 🔥 Устанавливаем id, чтобы репозиторий корректно сохранил
        userContext.setSelectedBase("Патенты");
        repository.save(userContext);
        BotEvent event = maxMapper.toEvent(update, 123, 123);

        assertNotNull(event);
        assertEquals(BotEvents.BACK_TO_START, event.getType());

        UserContext updatedCtx = repository.load("123");
        assertNull(updatedCtx.getSelectedBase());
        assertEquals(0, updatedCtx.getOffset());

    }

    @Test
    void handleMessageCallbackWithDocView() {
        // Given
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_callback");
        update.setChatId(123);

        CallbackDto callback = new CallbackDto();
        callback.setPayload("DOC_VIEW_9999");
        update.setCallback(callback);

        UserContext userContext = repository.load("23454");
        userContext.setUserId(23454);
        userContext.setChatId("123");
        repository.save(userContext);

        Mockito.when(patentService.sendSinglePatentCardAsync(Mockito.anyInt(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.empty());

        BotEvent event = maxMapper.toEvent(update, 23454, 123);

        // Then
        assertNotNull(event);
        assertEquals(BotEvents.USER_VIEW_DOC_DETAILS, event.getType());
        assertEquals("Просмотр документа 9999", event.getPayloadDescription());

        // Верифицируем вызов сервиса с chatId = 123
        Mockito.verify(patentService, Mockito.times(1))
                .sendSinglePatentCardAsync(Mockito.eq(23454), Mockito.eq("9999"), Mockito.any());
    }

    @Test
    void handleMessageCreatedFilterDateState() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_created");

        MessageDto message = new MessageDto();
        BodyDto body = new BodyDto();
        body.setText("2026-08-13");
        message.setBody(body);
        update.setMessage(message);

        UserContext userContext = repository.load("123");
        userContext.setState(BotStates.FILTER_DATE);
        repository.save(userContext);
        BotEvent event = maxMapper.toEvent(update, 123, 123);

        assertNotNull(event);
        assertEquals(BotEvents.USER_SELECTED_DATE, event.getType());

        UserContext updatedCtx = repository.load("123");
        assertEquals("2026-08-13", updatedCtx.getDate());

    }

    @Test
    void handleMessageCreatedSelectDateState() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_created");

        MessageDto message = new MessageDto();
        BodyDto body = new BodyDto();
        body.setText("Поисковый запрос");
        message.setBody(body);
        update.setMessage(message);

        UserContext userContext = repository.load("123");
        userContext.setState(BotStates.SELECT_DATE);
        repository.save(userContext);

        BotEvent event = maxMapper.toEvent(update, 1232, 123);

        assertNotNull(event);
        assertEquals(BotEvents.USER_SEARCH_PATENT, event.getType());

        UserContext updatedCtx = repository.load("123");
        assertEquals("Поисковый запрос", updatedCtx.getSearchQuery());

    }

    @Test
    void handleMessageCreatedDefaultStateFallback() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_created");

        UserContext userContext = repository.load("123");
        userContext.setState(BotStates.INIT);
        repository.save(userContext);

        BotEvent event = maxMapper.toEvent(update, 123, 123);

        assertNotNull(event);
        assertEquals(BotEvents.BACK_TO_START, event.getType());

    }

    @Test
    void handleMessageCallbackSelectBaseStoresDataCorrectly() {
        int chatId = 12345;
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_callback");

        CallbackDto callback = new CallbackDto();
        callback.setPayload("PATENTS");
        update.setCallback(callback);

        // Загружаем и настраиваем контекст
        UserContext initialCtx = repository.load(String.valueOf(chatId));
        initialCtx.setUserId(chatId);

        initialCtx.setChatId(String.valueOf(chatId));

        initialCtx.setOffset(50);
        repository.save(initialCtx);
        BotEvent event = maxMapper.toEvent(update, chatId, 123);

        assertNotNull(event);
        assertEquals(BotEvents.USER_SELECT_BASE, event.getType());

        UserContext savedCtx = repository.load(String.valueOf(123));
        assertEquals("Патенты", savedCtx.getSelectedBase());
        assertEquals(0, savedCtx.getOffset());
    }

    @Test
    void handleMessageCreatedFilterClassifiersState() {
        // Given
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_created");

        MessageDto message = new MessageDto();
        BodyDto body = new BodyDto();
        body.setText("A61K31/00");
        message.setBody(body);
        update.setMessage(message);

        UserContext userContext = repository.load("123");
        userContext.setState(BotStates.FILTER_CLASSIFIERS);
        repository.save(userContext);

        // When
        BotEvent event = maxMapper.toEvent(update, 123, 123);

        // Then
        assertNotNull(event);
        assertEquals(BotEvents.USER_SELECT_CLASSIFIERS, event.getType());
        assertEquals("Ввод классификатора", event.getPayloadDescription());

        UserContext updatedCtx = repository.load("123");
        assertEquals("A61K31/00", updatedCtx.getClassifiers());
    }

    @Test
    void handleMessageCreatedSearchState() {
        // Given
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_created");

        MessageDto message = new MessageDto();
        BodyDto body = new BodyDto();
        body.setText("Устройство для фильтрации"); // Поисковый запрос в стейте SEARCH
        message.setBody(body);
        update.setMessage(message);

        UserContext userContext = repository.load("123");
        userContext.setState(BotStates.SEARCH);
        repository.save(userContext);

        // When
        BotEvent event = maxMapper.toEvent(update, 123, 123);

        // Then
        assertNotNull(event);
        assertEquals(BotEvents.USER_SEARCH_PATENT, event.getType());
        assertEquals("Ввод поискового запроса", event.getPayloadDescription());

        UserContext updatedCtx = repository.load("123");
        assertEquals("Устройство для фильтрации", updatedCtx.getSearchQuery());
    }

    @Test
    void handleMessageCallbackSelectArrayStoresDataCorrectly() {
        // Given
        int chatId = 12345;
        int userId = 123456;

        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_callback");

        CallbackDto callback = new CallbackDto();
        callback.setPayload("COUNTRY_INPUT");

        SenderDto sender = new SenderDto();
        sender.setUserId(userId);
        callback.setUser(sender);
        update.setCallback(callback);

        UserContext initialCtx = repository.load(String.valueOf(userId));
        initialCtx.setUserId(userId);
        initialCtx.setChatId(String.valueOf(chatId));
        repository.save(initialCtx);

        List<String> mockArrays = List.of("SU_PATENTS", "RU_PATENTS");

        Mockito.when(patentService.getDataSetArrayByDescription("Россия и страны СНГ"))
                .thenReturn(mockArrays);

        // When
        BotEvent event = maxMapper.toEvent(update, chatId, userId);

        // Then
        assertNotNull(event);
        assertEquals(BotEvents.USER_SELECT_ARRAY, event.getType());
        assertEquals("Россия и страны СНГ", event.getPayloadDescription());

        UserContext savedCtx = repository.load(String.valueOf(userId));
        assertEquals("Россия и страны СНГ", savedCtx.getDatasetName());
        assertEquals(mockArrays, savedCtx.getDatasetArrays());

        Mockito.verify(patentService, Mockito.times(1))
                .getDataSetArrayByDescription("Россия и страны СНГ");
    }


    @Test
    void handleMessageCreatedWhenCurrentStateIsNull() {
        // Given
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_created");

        MessageDto message = new MessageDto();
        BodyDto body = new BodyDto();
        body.setText("Привет");
        message.setBody(body);
        update.setMessage(message);

        // Контекст без установленного стейта (currentState == null)
        UserContext userContext = repository.load("999");
        userContext.setState(null);
        repository.save(userContext);

        // When
        BotEvent event = maxMapper.toEvent(update, 999, 999);

        // Then
        assertNotNull(event);
        assertEquals(BotEvents.BACK_TO_START, event.getType());
    }

}