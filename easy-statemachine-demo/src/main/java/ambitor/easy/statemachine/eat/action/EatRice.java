package ambitor.easy.statemachine.eat.action;


import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author Ambitor
 */
@Slf4j
@Component
public class EatRice implements WorkFlowAction<String, String> {

    @Override
    public void execute(StateContext<String, String> context) {
        log.info("开始吃饭");
        addHeader(context, "Eat_Status", "Husband");
    }


}
