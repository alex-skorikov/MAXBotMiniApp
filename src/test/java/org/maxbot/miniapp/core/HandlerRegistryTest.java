package org.maxbot.miniapp.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.maxbot.miniapp.handlers.ArrayFilterHandler;
import org.maxbot.miniapp.handlers.ArraySelectHandler;
import org.maxbot.miniapp.handlers.BaseSelectHandler;
import org.maxbot.miniapp.handlers.ClassifiersFilterHandler;
import org.maxbot.miniapp.handlers.ClassifiersSelectHandler;
import org.maxbot.miniapp.handlers.DateFilterHandler;
import org.maxbot.miniapp.handlers.InitHandler;
import org.maxbot.miniapp.handlers.SearchHandler;
import org.maxbot.miniapp.handlers.SearchQueryWaitingHandler;
import org.maxbot.miniapp.statemachine.BotStates;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@ExtendWith(MockitoExtension.class)
class HandlerRegistryTest {

    @Mock
    private InitHandler initHandler;
    @Mock
    private BaseSelectHandler baseSelectHandler;
    @Mock
    private DateFilterHandler dateFilterHandler;
    @Mock
    private SearchHandler searchHandler;
    @Mock
    private ArrayFilterHandler arrayFilterHandler;
    @Mock
    private ArraySelectHandler arraySelectHandler;
    @Mock
    private ClassifiersFilterHandler classifiersFilterHandler;
    @Mock
    private ClassifiersSelectHandler classifiersSelectHandler;
    @Mock
    private SearchQueryWaitingHandler searchQueryWaitingHandler;

    private HandlerRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new HandlerRegistry(
                initHandler,
                baseSelectHandler,
                dateFilterHandler,
                searchHandler,
                arrayFilterHandler,
                arraySelectHandler,
                classifiersFilterHandler,
                classifiersSelectHandler,
                searchQueryWaitingHandler
        );
    }

    @Test
    void shouldReturnCorrectHandlerForEachBotState() {
        // Проверяем маппинг каждого состояния согласно конструктору реестра
        assertEquals(initHandler, registry.getHandler(BotStates.SELECT_BASE));
        assertEquals(baseSelectHandler, registry.getHandler(BotStates.BASE_SELECTED));
        assertEquals(dateFilterHandler, registry.getHandler(BotStates.FILTER_DATE));
        assertEquals(searchQueryWaitingHandler, registry.getHandler(BotStates.SELECT_DATE));
        assertEquals(arrayFilterHandler, registry.getHandler(BotStates.FILTER_SEARCH_ARRAY));
        assertEquals(arraySelectHandler, registry.getHandler(BotStates.SELECT_SEARCH_ARRAY));
        assertEquals(classifiersFilterHandler, registry.getHandler(BotStates.FILTER_CLASSIFIERS));
        assertEquals(classifiersSelectHandler, registry.getHandler(BotStates.SELECT_CLASSIFIERS));
        assertEquals(searchHandler, registry.getHandler(BotStates.SEARCH));
    }

    @Test
    void shouldReturnNullWhenStateIsNotMapped() {
        // Проверяем граничный случай с BotStates.INIT (он не добавляется в мапу в конструкторе)
        assertNull(registry.getHandler(BotStates.INIT),
                "Для незамапленного стейта INIT должен возвращаться null");
    }

    @Test
    void shouldReturnNullWhenStateIsNull() {
        // Проверяем защиту от NullPointerException при передаче null
        assertNull(registry.getHandler(null),
                "При передаче null должен возвращаться null");
    }
}
