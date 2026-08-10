package org.maxbot.miniapp.config;

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
                .initial(BotStates.INIT) // Задаем стартовую точку бота

                .state(BotStates.SELECT_BASE, stepAction)
                .state(BotStates.BASE_SELECTED, stepAction)
                .state(BotStates.FILTER_DATE, stepAction)
//                .state(BotStates.SELECT_DATE, stepAction)
                .state(BotStates.SEARCH, stepAction)
                .state(BotStates.FILTER_SEARCH_ARRAY, stepAction)
                .state(BotStates.SELECT_SEARCH_ARRAY, stepAction)
                .state(BotStates.FILTER_CLASSIFIERS, stepAction)
                .state(BotStates.SELECT_CLASSIFIERS, stepAction)

                .state(BotStates.DONE, stepAction)

                // Регистрируем структуру всех остальных стейтов перечислением
                .states(java.util.EnumSet.allOf(BotStates.class));
    }

    /**
     * Конфигурация переходов
     */
    @Override
    public void configure(StateMachineTransitionConfigurer<BotStates, BotEvents> transitions) throws Exception {
        transitions
                // Открытие чата/выбор базы (Добро пожаловать)
                .withExternal()
                .source(BotStates.INIT)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.USER_OPEN_CHAT)
                .and()

                // База выбрана -> Выберите фильтры
                .withExternal()
                .source(BotStates.SELECT_BASE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECT_BASE)
                .and()

                // Из меню фильтров заходим в подменю выбора Даты
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_DATE)
                .event(BotEvents.USER_INPUT_DATE)
                .and()

                // Запрос даты поиска, возврат в фильтры
                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECTED_DATE)
                .guard(validDateGuard)
                .and()

                // Из меню фильтров заходим в подменю выбора Поисковых массивов
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_SEARCH_ARRAY)
                .event(BotEvents.USER_SEARCH_ARRAY)
                .and()

                // Выбор поискового массива, возврат в меню фильтров
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_SEARCH_ARRAY)
                .event(BotEvents.USER_SELECT_ARRAY)
                .and()

                // Из меню фильтров заходим в подменю выбора Классификаторов
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.FILTER_CLASSIFIERS)
                .event(BotEvents.USER_SEARCH_CLASSIFIERS)
                .and()

                // Выбор Классификатора, возврат в меню фильтров
                .withExternal()
                .source(BotStates.FILTER_CLASSIFIERS)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.USER_SELECT_CLASSIFIERS)
                .and()

                // Запрос слов/строки поиска поиск
                .withExternal()
                .source(BotStates.SELECT_DATE)
                .target(BotStates.SEARCH)
                .event(BotEvents.USER_SEARCH_PATENT)
                .and()

                // ==========================================
                // РЕВЕРСИВНЫЙ ХОД (Кнопки «Назад»)
                // ==========================================

                // Из главного меню фильтров возвращаемся обратно к стартовому выбору баз
                .withExternal()
                .source(BotStates.BASE_SELECTED)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.BACK)
                .and()

                // Из подменю Даты возвращаемся в главное меню фильтров
                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK)
                .and()

                // Из подменю выбора массива возвращаемся в главное меню фильтров
                .withExternal()
                .source(BotStates.FILTER_SEARCH_ARRAY)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK)
                .and()

                // Из подменю выбора Классификатора возвращаемся в главное меню фильтров
                .withExternal()
                .source(BotStates.FILTER_CLASSIFIERS)
                .target(BotStates.BASE_SELECTED)
                .event(BotEvents.BACK)
                .and()

                // Из подменю ввода строки поиска возвращаемся в меню выбора даты
                .withExternal()
                .source(BotStates.SELECT_DATE)
                .target(BotStates.FILTER_DATE)
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
