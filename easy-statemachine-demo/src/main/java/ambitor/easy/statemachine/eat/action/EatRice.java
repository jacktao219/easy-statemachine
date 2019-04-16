package ambitor.easy.statemachine.eat.action;


import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Random;

/**
 * @author Ambitor
 */
@Slf4j
@Component
public class EatRice implements WorkFlowAction<String, String> {

    @Override
    public void execute(StateContext<String, String> context) {
        log.info("开始吃饭");
        boolean delicious = new Random().nextBoolean();
        String result;
        //菜做的不好吃老公洗碗，否者老婆洗碗
        if (!delicious) {
            result = "Husband";
        } else {
            result = "Wife";
        }
        addHeader(context, "Eat_Status", result);
    }


}
