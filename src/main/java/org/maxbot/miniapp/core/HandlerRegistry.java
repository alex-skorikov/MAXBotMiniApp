package org.maxbot.miniapp.core;

import org.maxbot.miniapp.handlers.ArrayFilterHandler;
import org.maxbot.miniapp.handlers.ArraySelectHandler;
import org.maxbot.miniapp.handlers.BaseSelectHandler;
import org.maxbot.miniapp.handlers.ClassifiersFilterHandler;
import org.maxbot.miniapp.handlers.ClassifiersSelectHandler;
import org.maxbot.miniapp.handlers.DateFilterHandler;
import org.maxbot.miniapp.handlers.InitHandler;
import org.maxbot.miniapp.handlers.SearchHandler;
import org.maxbot.miniapp.handlers.SearchQueryWaitingHandler;
import org.maxbot.miniapp.handlers.StepHandler;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class HandlerRegistry {

    private final Map<BotStates, StepHandler> map = new EnumMap<>(BotStates.class);

    public HandlerRegistry(
            InitHandler init,
            BaseSelectHandler selectBase,
            DateFilterHandler filterDate,
            SearchHandler searchHandler,
            ArrayFilterHandler arrayFilterHandler,
            ArraySelectHandler arraySelectHandler,
            ClassifiersFilterHandler classifiersFilterHandler,
            ClassifiersSelectHandler classifiersSelectHandler,
            SearchQueryWaitingHandler searchQueryWaitingHandler
    ) {
        // Старт
        map.put(BotStates.SELECT_BASE, init);
        // Выбор базы, показываем главное меню фильтров
        map.put(BotStates.BASE_SELECTED, selectBase);
        // Выбор фильтра ДАТА, запрос даты в формате yyyy-mm-dd
        map.put(BotStates.FILTER_DATE, filterDate);
        // Поисковая строк получена
        map.put(BotStates.SELECT_DATE, searchQueryWaitingHandler);
        // Выбор поискового массива
        map.put(BotStates.FILTER_SEARCH_ARRAY, arrayFilterHandler);
        // Поисковый массив выбран
        map.put(BotStates.SELECT_SEARCH_ARRAY, arraySelectHandler);
        // Выбор классификатора
        map.put(BotStates.FILTER_CLASSIFIERS, classifiersFilterHandler);
        // Классификатор выбран
        map.put(BotStates.SELECT_CLASSIFIERS, classifiersSelectHandler);
        // Ввод поискового запроса
        map.put(BotStates.SEARCH, searchHandler);
    }

    public StepHandler getHandler(BotStates state) {
        return map.get(state);
    }
}



