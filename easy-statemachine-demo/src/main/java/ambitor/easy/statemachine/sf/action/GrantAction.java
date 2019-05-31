package ambitor.easy.statemachine.sf.action;


import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.sf.enumerate.GrantEvent;
import ambitor.easy.statemachine.sf.enumerate.GrantState;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static ambitor.easy.statemachine.sf.enumerate.GrantConstant.GRANT_STATUS;
import static ambitor.easy.statemachine.sf.enumerate.GrantConstant.GRANT_SUCCESS;

@Component
@Slf4j
public class GrantAction implements WorkFlowAction<GrantState, GrantEvent> {

    /**
     * 放款
     * @param context 上下文
     */
    @Override
    public void execute(StateContext<GrantState, GrantEvent> context) {
        System.out.println("放款");
        addHeader(context, GRANT_STATUS, GRANT_SUCCESS);
    }

}
