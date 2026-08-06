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
            SelectBaseHandler selectBase,
            FilterMenuHandler filterMenu,
            DateFilterHandler filterDate,
            SaveDateHandler saveDate,
            FilterClassifiersHandler filterClassifiers,
            SaveClassifierHandler saveClassifier
    ) {
        map.put(BotStates.INIT, init);
        map.put(BotStates.SELECT_BASE, selectBase);
        map.put(BotStates.SELECT_FILTERS, filterMenu);
//        map.put(BotStates.FILTER_ARRAYS, filterMenu);
        map.put(BotStates.FILTER_DATE, filterDate);
        map.put(BotStates.FILTER_CLASSIFIERS, filterClassifiers);
        map.put(BotStates.DONE, saveDate); // пример
    }

    public StepHandler getHandler(BotStates state) {
        return map.get(state);
    }
}



