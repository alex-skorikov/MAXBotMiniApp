package org.maxbot.miniapp.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.maxbot.miniapp.statemachine.BotEvents;
import org.maxbot.miniapp.statemachine.BotStates;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.statemachine.StateMachine;
import org.springframework.statemachine.config.StateMachineFactory;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.jupiter.api.Assertions.*;

@RunWith(SpringRunner.class)
@SpringBootTest
class StateMachineConfigTest {

    @Autowired
    StateMachineFactory<BotStates, BotEvents> factory;

    @Test
    void testMachine() {
        StateMachine<BotStates, BotEvents> machine = factory.getStateMachine("test");
        machine.start();
        machine.sendEvent(BotEvents.USER_OPEN_CHAT);
        machine.sendEvent(BotEvents.USER_SELECT_BASE);
//        machine.sendEvent(BotEvents.USER_CLICK_FILTERS);

        Assertions.assertEquals(machine.getState().getId(), BotStates.SELECT_BASE);

    }
}