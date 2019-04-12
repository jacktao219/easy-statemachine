package ambitor.easy.statemachine.eat.action;


import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.sf.enumerate.SFGrantEvent;
import ambitor.easy.statemachine.sf.enumerate.SFGrantState;
import ambitor.easy.statemachine.workflow.action.WorkFlowAction;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class WifeWash implements WorkFlowAction<SFGrantState, SFGrantEvent> {

    @Override
    public void execute(StateContext<SFGrantState, SFGrantEvent> context) {
        log.info("老婆洗碗，洗的很仔细");
    }
}
