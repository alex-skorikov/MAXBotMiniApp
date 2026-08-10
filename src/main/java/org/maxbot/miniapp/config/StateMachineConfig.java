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

    /**
     * КРИТИЧЕСКИЙ ФИКС: Регистрируем стейты и привязываем StepAction на ВХОД в состояния
     */
    @Override
    public void configure(StateMachineStateConfigurer<BotStates, BotEvents> states) throws Exception {
        states
                .withStates()
                .initial(BotStates.INIT) // Задаем стартовую точку бота

                // 🟢 Назначаем stepAction на вход в конкретные состояния
                .state(BotStates.SELECT_BASE, stepAction)
                .state(BotStates.SELECT_FILTERS, stepAction)
                .state(BotStates.FILTER_DATE, stepAction)
                .state(BotStates.FILTER_ARRAYS, stepAction)
                .state(BotStates.CONFIRM_SEARCH, stepAction)

                // Регистрируем структуру всех остальных стейтов перечислением
                .states(java.util.EnumSet.allOf(BotStates.class));
    }

    /**
     * Конфигурация переходов (Транзишенов) - теперь БЕЗ .action(stepAction)
     */
    @Override
    public void configure(StateMachineTransitionConfigurer<BotStates, BotEvents> transitions) throws Exception {
        transitions
                // Переход 1: Открытие чата/выбор базы (Добро пожаловать)
                .withExternal()
                .source(BotStates.INIT)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.USER_OPEN_CHAT)

                .and()

                // Переход 2: База выбрана -> Выберите фильтры
                .withExternal()
                .source(BotStates.SELECT_BASE)
                .target(BotStates.SELECT_FILTERS)
                .event(BotEvents.USER_SELECT_BASE)

                .and()

                // Переход 3. Из меню фильтров заходим в подменю выбора Даты
                .withExternal()
                .source(BotStates.SELECT_FILTERS)
                .target(BotStates.FILTER_DATE)
                .event(BotEvents.USER_INPUT_DATE)

                .and()

                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.SELECT_DATE)
                .event(BotEvents.USER_SELECTED_DATE)
                .guard(validDateGuard)        // опционально

                .and()

                // Переход 4. Из меню фильтров заходим в подменю настроек Массивов или Классификаторов
                .withExternal()
                .source(BotStates.SELECT_FILTERS)
                .target(BotStates.FILTER_ARRAYS)
                .event(BotEvents.USER_SELECT_FILTERS)

                .and()

                // Переход 5. к экрану подтверждения поиска
                .withExternal()
                .source(BotStates.SELECT_FILTERS)
                .target(BotStates.CONFIRM_SEARCH)
                .event(BotEvents.USER_CONFIRM)

                .and()

                // ==========================================
                // РЕВЕРСИВНЫЙ ХОД (Кнопки «Назад»)
                // ==========================================

                // Из главного меню фильтров возвращаемся обратно к стартовому выбору баз
                .withExternal()
                .source(BotStates.SELECT_FILTERS)
                .target(BotStates.SELECT_BASE)
                .event(BotEvents.BACK)

                .and()

                // Из подменю Даты возвращаемся в главное меню фильтров
                .withExternal()
                .source(BotStates.FILTER_DATE)
                .target(BotStates.SELECT_FILTERS)
                .event(BotEvents.BACK)

                .and()

                // Из подменю Массивов возвращаемся в главное меню фильтров
                .withExternal()
                .source(BotStates.FILTER_ARRAYS)
                .target(BotStates.SELECT_FILTERS)
                .event(BotEvents.BACK)

                .and()

                // Из экрана подтверждения возвращаемся обратно в меню фильтров
                .withExternal()
                .source(BotStates.CONFIRM_SEARCH)
                .target(BotStates.SELECT_FILTERS)
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
