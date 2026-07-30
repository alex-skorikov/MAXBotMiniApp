package org.maxbot.miniapp.stepalgo;

public enum AlgoStatus {

    END(-1),
    STEP_0(0),
    STEP_1(1),
    STEP_2(2),
    STEP_3(3),
    STEP_4(4),
    STEP_5(5),
    STEP_6(6),
    STEP_7(7);

    final Integer intValue;

    AlgoStatus(Integer intValue) {
        this.intValue = intValue;
    }
}
