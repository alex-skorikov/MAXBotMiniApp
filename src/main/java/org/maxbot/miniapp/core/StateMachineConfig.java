package org.maxbot.miniapp.core;

import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.maxbot.miniapp.util.StepAction;
import org.maxbot.miniapp.util.ValidDateGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.statemachine.config.EnableStateMachineFactory;
import org.springframework.statemachine.config.StateMachineConfigurerAdapter;
import org.springframework.statemachine.config.builders.StateMachineConfigurationConfigurer;
import org.springframework.statemachine.config.builders.StateMachineStateConfigurer;
import org.springframework.statemachine.config.builders.StateMachineTransitionConfigurer;
import org.springframework.statemachine.listener.StateMachineListenerAdapter;
import org.springframework.statemachine.state.State;

@Configuration
@EnableStateMachineFactory
public class StateMachineConfig extends StateMachineConfigurerAdapter<BotStates, BotEvents> {

    private final StepAction stepAction;
    private final ValidDateGuard validDateGuard;

    public StateMachineConfig(StepAction stepAction,
                              ValidDateGuard validDateGuard) {
        this.stepAction = stepAction;
        this.validDateGuard = validDateGuard;
    }

    @Override
    public void configure(StateMachineStateConfigurer<BotStates, BotEvents> states) throws Exception {
        states
                .withStates()
                .initial(BotStates.INIT)
                .state(BotStates.SELECT_BASE, stepAction)
                .state(BotStates.BASE_SELECTED, stepAction)
                .state(BotStates.FILTER_DATE, stepAction)
                .state(BotStates.SELECT_DATE, stepAction) // Хранит логику ожидания ввода поисковой строки
                .state(BotStates.SEARCH, stepAction)      // Экран выполнения самого поиска и результатов
                .state(BotStates.FILTER_SEARCH_ARRAY, stepAction)
                .state(BotStates.SELECT_SEARCH_ARRAY, stepAction)
                .state(BotStates.FILTER_CLASSIFIERS, stepAction)
                .state(BotStates.SELECT_CLASSIFIERS, stepAction)
                .state(BotStates.DONE, stepAction);
    }

    @Override
    public void configure(StateMachineTransitionConfigurer<BotStates, BotEvents> transitions) throws Exception {
        transitions
                // СТАРТ БОТА
                .withExternal()
                .source(BotStates.INIT)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.USER_OPEN_CHAT)
                .and()

                // ВЫБОР БАЗЫ -> ГЛАВНОЕ МЕНЮ ФИЛЬТРОВ
                .withExternal()
                .source(BotStates.SELECT_BASE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECT_BASE)
                .and()

                // ПОДМЕНЮ: ДАТА
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_DATE)
                .event(BotEvents.USER_INPUT_DATE)
                .and()

                // Если введенная дата валидна
                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECTED_DATE)
                .guard(validDateGuard)
                .and()

                // Если введенная дата не валидна
                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.FILTER_DATE)
                .event(BotEvents.USER_SELECTED_DATE)
                .guard(validDateGuard.negate())
                .action(stepAction)
                .and()

                // ПОДМЕНЮ: ПОИСКОВЫЕ МАССИВЫ
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_SEARCH_ARRAY)
                .event(BotEvents.USER_SEARCH_ARRAY)
                .and()

                .withExternal()
                .source(BotStates.FILTER_SEARCH_ARRAY)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECT_ARRAY)
                .and()

                // ПОДМЕНЮ: КЛАССИФИКАТОРЫ
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_CLASSIFIERS)
                .event(BotEvents.USER_SEARCH_CLASSIFIERS)
                .and()

                .withExternal()
                .source(BotStates.FILTER_CLASSIFIERS)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECT_CLASSIFIERS)
                .and()

                // КНОПКА "СТАРТ ПОИСКА" -> Стей ожидания ввода текста (SELECT_DATE)
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.SELECT_DATE)
                .event(BotEvents.USER_PROCEED_TO_SEARCH) // Ивент от инлайн-кнопки "START_SEARCH"
                .and()

                // ПОЛЬЗОВАТЕЛЬ ПРИСЛАЛ ТЕКСТ ЗАПРОСА -> Переходим непосредственно к поиску патентов
                .withExternal()
                .source(BotStates.SELECT_DATE)
                .target(BotStates.SEARCH)
                .event(BotEvents.USER_SEARCH_PATENT) // Вызывается внутри handleMessageCreated
                .and()

                // Циклический переход для пагинации (Кнопки Вперёд / Назад)
                .withExternal()
                .source(BotStates.SEARCH)
                .target(BotStates.SEARCH)
                .event(BotEvents.USER_SEARCH_PATENT)
                .and()

                // Переход по кнопке «Сбросить» из экрана поиска в самое начало
                .withExternal()
                .source(BotStates.SEARCH)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.BACK_TO_START)
                .and()

                //Из меню ВЫБОР БАЗЫ -> ПОИСК
                .withExternal()
                .source(BotStates.SELECT_BASE)
                .target(BotStates.SELECT_DATE)
                .event(BotEvents.USER_PROCEED_TO_SEARCH)
                .and()

                //Из меню ВЫБОР БАЗЫ -> СБРОС
                .withExternal()
                .source(BotStates.SELECT_BASE)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.BACK_TO_START)
                .and()

                //Из меню ВЫБОР ФИЛЬТРОВ -> ПОИСК
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.SELECT_DATE)
                .event(BotEvents.USER_PROCEED_TO_SEARCH)
                .and()

                //Из меню ВЫБОР ФИЛЬТРОВ -> СБРОС
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.BACK_TO_START)
                .and()

                // Из результатов поиска -> Сбросить
                .withExternal()
                .source(BotStates.SELECT_DATE)
                .target(BotStates.INIT)
                .event(BotEvents.BACK_TO_START)
                .and()

                // ==========================================
                // КНОПКИ «НАЗАД»
                // ==========================================
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.BACK)
                .and()

                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK)
                .and()

                .withExternal()
                .source(BotStates.FILTER_SEARCH_ARRAY)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK)
                .and()

                .withExternal()
                .source(BotStates.FILTER_CLASSIFIERS)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK)
                .and()

                // Из экрана ввода строки поиска возвращаемся обратно в меню фильтров
                .withExternal()
                .source(BotStates.SELECT_DATE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK);
    }


    @Override
    public void configure(StateMachineConfigurationConfigurer<BotStates, BotEvents> config) throws Exception {
        config
                .withConfiguration()
                .listener(new StateMachineListenerAdapter<>() {
                    @Override
                    public void eventNotAccepted(Message<BotEvents> event) {
                        System.out.println("⚠️ СТЕЙТ-МАШИНА ОТВЕРГЛА ИВЕНТ: " + event.getPayload());
                    }

                    @Override
                    public void stateChanged(State<BotStates, BotEvents> from, State<BotStates, BotEvents> to) {
                        System.out.println("🔄 Стейт изменился: " + (from != null ? from.getId() : "null") + " -> " + to.getId());
                    }
                });
    }
}
