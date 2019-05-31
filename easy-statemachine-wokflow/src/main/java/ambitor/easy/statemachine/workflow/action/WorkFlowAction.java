package ambitor.easy.statemachine.workflow.action;

import ambitor.easy.statemachine.core.action.Action;
import ambitor.easy.statemachine.core.context.MessageHeaders;
import ambitor.easy.statemachine.core.context.StateContext;
import ambitor.easy.statemachine.core.exception.StateMachineException;
import ambitor.easy.statemachine.core.exception.StateMachineRetryException;
import ambitor.easy.statemachine.workflow.model.StateMachineTask;

import java.util.function.Consumer;

import static ambitor.easy.statemachine.workflow.model.StateMachineConstant.TASK_HEADER;

/**
 * 抽象Action，提供方法，方便Action实现类操作Task
 * Created by Ambitor on 2019/4/3
 * @author Ambitor
 */
public interface WorkFlowAction<S, E> extends Action<S, E> {
    /**
     * 从上下文中拿到StateMachineTask对象
     * @param context 上下文
     * @return task
     */
    default StateMachineTask getStateMachineTask(StateContext<S, E> context) {
        MessageHeaders headers = context.getMessage().getHeaders();
        return (StateMachineTask) headers.getHeader(TASK_HEADER);
    }

    /**
     * 异常处理Action, action抛出异常后会进入errorAction，详细请看Actions.errorCallingAction实现
     * @param consumer 业务逻辑
     * @return errorAction
     */
    default Action<S, E> errorAction(Consumer<StateContext<S, E>> consumer) {
        return (s) -> {
            StateMachineTask task = getStateMachineTask(s);
            //如果是最后一次重试
            Exception e = s.getException();
            if (e == null) {
                throw new StateMachineRetryException("未知异常");
            }
            boolean isRetryException = e instanceof StateMachineRetryException;
            //非重试异常
            if (!isRetryException) {
                consumer.accept(s);
            } else {
                //重试异常且最后一次尝试
                if (task.isLastRetry()) {
                    consumer.accept(s);
                } else {
                    throw (StateMachineRetryException) e;
                }
            }
        };
    }
}
