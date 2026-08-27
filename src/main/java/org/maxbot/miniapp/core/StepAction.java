package org.maxbot.miniapp.core;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.dto.bot.BotResponse;
import org.maxbot.miniapp.handlers.StepHandler;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.statemachine.StateContext;
import org.springframework.statemachine.action.Action;
import org.springframework.stereotype.Component;

/**
 * Action: единая точка вызова хендлеров
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StepAction implements Action<BotStates, BotEvents> {

    private final HandlerRegistry handlerRegistry;

/*    @Override
    public void execute(StateContext<BotStates, BotEvents> context) {

        if (context.getTarget() == null) {
            return;
        }
        BotStates targetState = context.getTarget().getId();

        BotEvent event = (BotEvent) context.getMessageHeader("event");
        UserContext userContext = (UserContext) context.getMessageHeader("userContext");

        StepHandler handler = handlerRegistry.getHandler(targetState);

        if (handler == null) {
            return;
        }

        BotResponse response = handler.handle(userContext, event);

        if (response != null) {
            context.getExtendedState().getVariables().put("response", response);
        }
    }*/

    @Override
    public void execute(StateContext<BotStates, BotEvents> context) {
        // 1. НАДЕЖНОЕ ОПРЕДЕЛЕНИЕ ЦЕЛЕВОГО СТЕЙТА (куда переходит машина)
        BotStates targetState = null;

        if (context.getTransition() != null && context.getTransition().getTarget() != null) {
            // Если вызвано как Transition Action (наш случай)
            targetState = context.getTransition().getTarget().getId();
        } else if (context.getTarget() != null) {
            // Фаллбэк для State Action
            targetState = context.getTarget().getId();
        }

        if (targetState == null) {
            log.warn("⚠️ [StepAction] Не удалось определить целевое состояние перехода");
            return;
        }

        // 2. ИЗВЛЕЧЕНИЕ ДАННЫХ ИЗ EXTENDED STATE (из памяти стейт-машины)
        UserContext userContext = (UserContext) context.getExtendedState().getVariables().get("userContext");
        BotEvent event = (BotEvent) context.getExtendedState().getVariables().get("botEvent");

        // Фаллбэк на заголовки, если в памяти пусто (для обратной совместимости)
        if (userContext == null) {
            userContext = (UserContext) context.getMessageHeader("userContext");
        }
        if (event == null) {
            event = (BotEvent) context.getMessageHeader("event");
        }

        if (userContext == null) {
            log.error("❌ [StepAction] UserContext отсутствует в контексте выполнения стейт-машины");
            return;
        }

        // Синхронизируем состояние в контексте пользователя перед вызовом хендлера
        userContext.setState(targetState);

        // 3. ПОЛУЧЕНИЕ И ВЫЗОВ ХЕНДЛЕРА
        StepHandler handler = handlerRegistry.getHandler(targetState);
        if (handler == null) {
            log.warn("⚠️ [StepAction] Хендлер для состояния {} не зарегистрирован в HandlerRegistry", targetState);
            return;
        }

        log.info("🎯 [StepAction] Вызываем хендлер [{}] для стейта [{}]", handler.getClass().getSimpleName(), targetState);
        BotResponse response = handler.handle(userContext, event);

        // 4. ЗАПИСЬ ОТВЕТА ДЛЯ ДИСПЕТЧЕРА
        if (response != null) {
            context.getExtendedState().getVariables().put("response", response);
        }
    }

}

