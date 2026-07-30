package org.maxbot.miniapp.stepalgo;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.maxbot.miniapp.dto.bot.UpdateDto;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public abstract class AbstractAlgo {

    @Getter
    protected List<AlgoStatus> performed = new ArrayList<>();

    public void run(AlgoStatus start, UpdateDto upd) throws AlgoException {
        performed.add(start);
        while (!AlgoStatus.END.equals(performed.get(performed.size() - 1))) {
            if (performed.size() >= maxStepNumber()) {
                throw new AlgoException("Сценарий <" + this.getClass().getName() + "> зациклился. Шаги" + performed);
            }
            switch (performed.get(performed.size() - 1)) {
                case STEP_0: {
                    performed.add(step0(upd));
                    break;
                }
                case STEP_1: {
                    performed.add(step1(upd));
                    break;
                }
                case STEP_2: {
                    performed.add(step2(upd));
                    break;
                }
                case STEP_3: {
                    performed.add(step3(upd));
                    break;
                }
                case STEP_4: {
                    performed.add(step4(upd));
                    break;
                }
                case STEP_5: {
                    performed.add(step5(upd));
                    break;
                }
                case STEP_6: {
                    performed.add(step6(upd));
                    break;
                }
                case STEP_7: {
                    performed.add(step7(upd));
                    break;
                }
                case END: {
                    break;
                }
            }
        }
    }
    protected abstract int maxStepNumber();
    protected AlgoStatus step0(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step1(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step2(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step3(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step4(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step5(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step6(UpdateDto upd) {
        return AlgoStatus.END;
    }

    protected AlgoStatus step7(UpdateDto upd) {
        return AlgoStatus.END;
    }
}
