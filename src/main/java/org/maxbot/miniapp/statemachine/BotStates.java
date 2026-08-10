package org.maxbot.miniapp.statemachine;

public enum BotStates {
    INIT,
    SELECT_BASE,       // Выбор базы данных
    SELECT_FILTERS,    // Главное меню фильтров (Дата, Массивы, Классификаторы)

    FILTER_DATE,       // Ввод/выбор даты
    SELECT_DATE,       // Ввод/выбор даты

    SEARCH_ARRAY,      // Поисковые массивы

    SEARCH,     // Поиск

    DONE
}


