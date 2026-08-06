package org.maxbot.miniapp.statemachine;

public enum BotStates {
    INIT,
    SELECT_BASE,       // Выбор базы данных
    SELECT_FILTERS,    // Главное меню фильтров (Дата, Массивы, Классификаторы)
    FILTER_DATE,       // Ввод/выбор даты
    FILTER_ARRAYS,     // Выбор поисковых массивов
    FILTER_CLASSIFIERS,// Выбор классификаторов
    CONFIRM_SEARCH,     // Экран подтверждения и отправки запроса
    DONE
}


