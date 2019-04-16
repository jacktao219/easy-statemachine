package ambitor.easy.statemachine.eat.action;


import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.sf.enumerate.SFGrantEvent;
import ambitor.easy.statemachine.sf.enumerate.SFGrantState;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HusbandWash implements WorkFlowAction<String, String> {

    @Override
    public void execute(StateContext<String, String> context) {
        log.info("老公洗碗，随便洗洗");
    }


}
