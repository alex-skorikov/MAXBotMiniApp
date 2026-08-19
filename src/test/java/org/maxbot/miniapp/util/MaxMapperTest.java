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
        assertNull(maxMapper.toEvent(null, 123));

        UpdateDto emptyUpdate = new UpdateDto();
        assertNull(maxMapper.toEvent(emptyUpdate, 123));
    }

    @Test
    void handleBotStartedSuccess() {
        UpdateDto update = new UpdateDto();
        update.setUpdateType("bot_started");
        update.setUserId(777);
        BotEvent event = maxMapper.toEvent(update, 123);
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
        BotEvent event = maxMapper.toEvent(update, 123);
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
        userContext.setSearchOffset(10);
        userContext.setSearchLimit(5);
        repository.save(userContext);
        BotEvent event = maxMapper.toEvent(update, 123);
        assertNotNull(event);
        assertEquals(BotEvents.USER_SEARCH_PATENT, event.getType());

        UserContext updatedCtx = repository.load("123");
        assertEquals(15, updatedCtx.getSearchOffset());

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
        BotEvent event = maxMapper.toEvent(update, 123);

        assertNotNull(event);
        assertEquals(BotEvents.BACK_TO_START, event.getType());

        UserContext updatedCtx = repository.load("123");
        assertNull(updatedCtx.getSelectedBase());
        assertEquals(0, updatedCtx.getSearchOffset());

    }

    @Test
    void handleMessageCallbackWithDocView() {
        // Given
        UpdateDto update = new UpdateDto();
        update.setUpdateType("message_callback");

        CallbackDto callback = new CallbackDto();
        callback.setPayload("DOC_VIEW_9999");
        update.setCallback(callback);

        Mockito.when(patentService.sendSinglePatentCardAsync(Mockito.anyInt(), Mockito.anyString(), Mockito.any()))
                .thenReturn(Mono.empty());

        // When
        BotEvent event = maxMapper.toEvent(update, 123);

        // Then
        assertNotNull(event);
        assertEquals(BotEvents.USER_VIEW_DOC_DETAILS, event.getType());
        assertEquals("Просмотр документа 9999", event.getPayloadDescription());

        Mockito.verify(patentService, Mockito.times(1))
                .sendSinglePatentCardAsync(Mockito.eq(123), Mockito.eq("9999"), Mockito.any());
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
        BotEvent event = maxMapper.toEvent(update, 123);

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

        BotEvent event = maxMapper.toEvent(update, 123);

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

        BotEvent event = maxMapper.toEvent(update, 123);

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

        initialCtx.setChatId(String.valueOf(chatId)); // Если тип int

        initialCtx.setSearchOffset(50);
        repository.save(initialCtx);
        BotEvent event = maxMapper.toEvent(update, chatId);

        assertNotNull(event);
        assertEquals(BotEvents.USER_SELECT_BASE, event.getType());

        UserContext savedCtx = repository.load(String.valueOf(chatId));
        assertEquals("Патенты", savedCtx.getSelectedBase());
        assertEquals(0, savedCtx.getSearchOffset());
    }


}