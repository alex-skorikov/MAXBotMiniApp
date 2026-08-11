package org.maxbot.miniapp.statemachine;

public enum BotStates {
    INIT,
    SELECT_BASE,       // Выбор базы данных
    BASE_SELECTED,    // Главное меню фильтров (Дата, Массивы, Классификаторы)

    FILTER_DATE,       // Ввод/выбор даты
    SELECT_DATE,       // Ввод/выбор даты

    FILTER_SEARCH_ARRAY,      // Поисковые массивы
    SELECT_SEARCH_ARRAY,      // Поисковые массивы

    FILTER_CLASSIFIERS,
    SELECT_CLASSIFIERS,

    SEARCH,     // Поиск

    DONE
}


