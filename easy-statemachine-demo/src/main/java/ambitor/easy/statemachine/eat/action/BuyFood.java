package ambitor.easy.statemachine.eat.action;


import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BuyFood implements WorkFlowAction<String, String> {

    @Override
    public void execute(StateContext<String, String> context) {
        String state = context.getSource().getId();
        log.info("去沃尔玛买菜");
    }


}
