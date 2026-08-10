package org.maxbot.miniapp.statemachine;

public enum BotStates {
    INIT,
    SELECT_BASE,       // Выбор базы данных

    FILTER_DATE,       // Ввод/выбор даты
    SELECT_DATE,       // Ввод/выбор даты


    SELECT_FILTERS,    // Главное меню фильтров (Дата, Массивы, Классификаторы)
    FILTER_ARRAYS,     // Выбор поисковых массивов
    FILTER_CLASSIFIERS,// Выбор классификаторов
    CONFIRM_SEARCH,     // Экран подтверждения и отправки запроса
    DONE
}


