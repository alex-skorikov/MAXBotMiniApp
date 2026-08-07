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
            FilterMenuHandler filterMenu,
            DateFilterHandler filterDate,
            SaveDateHandler saveDate,
            FilterClassifiersHandler filterClassifiers,
            SaveClassifierHandler saveClassifier
    ) {
        // 🟢 При первом старте (переход в SELECT_BASE) показываем приветствие и выбор баз
        map.put(BotStates.SELECT_BASE, init);

        // 🟢 Когда база выбрана (переход в SELECT_FILTERS), показываем главное меню фильтров
        map.put(BotStates.SELECT_FILTERS, selectBase);

        // Оставляем остальные подменю фильтров без изменений
        map.put(BotStates.FILTER_DATE, filterDate);
        map.put(BotStates.FILTER_CLASSIFIERS, filterClassifiers);
        map.put(BotStates.DONE, saveDate);
    }

    public StepHandler getHandler(BotStates state) {
        return map.get(state);
    }
}



