package org.maxbot.miniapp.core;

import org.maxbot.miniapp.handlers.*;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
public class HandlerRegistry {

    private static final Map<BotStates, StepHandler> map = new EnumMap<>(BotStates.class);

    public HandlerRegistry(
            InitHandler init,
            BaseSelectHandler selectBase,
            DateFilterHandler filterDate,
            DateSelectedHandler dateSelectedHandler,
            SearchHandler searchHandler
    ) {
        // Старт
        map.put(BotStates.SELECT_BASE, init);
        // Выбор базы, показываем главное меню фильтров
        map.put(BotStates.SELECT_FILTERS, selectBase);
        // Выбор фильтра ДАТА, запрос даты в формате yyyy-mm-dd
        map.put(BotStates.FILTER_DATE, filterDate);
        // Дата выбрана, запрос текста для поиска
        map.put(BotStates.SELECT_DATE, dateSelectedHandler);
        // Ввод поискового запроса
        map.put(BotStates.SEARCH, searchHandler);
        

    }

    public StepHandler getHandler(BotStates state) {
        return map.get(state);
    }
}



