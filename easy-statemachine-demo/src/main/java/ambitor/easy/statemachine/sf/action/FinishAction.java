package ambitor.easy.statemachine.sf.action;

import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.sf.enumerate.GrantEvent;
import ambitor.easy.statemachine.sf.enumerate.GrantState;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class FinishAction implements WorkFlowAction<GrantState, GrantEvent> {
    /**
     * 放款成功后的action
     */
    @Override
    public void execute(StateContext<GrantState, GrantEvent> context) {
       log.info("放款成功后生成还款计划");
    }
}