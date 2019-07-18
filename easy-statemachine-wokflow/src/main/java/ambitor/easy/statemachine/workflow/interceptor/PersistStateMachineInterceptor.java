package ambitor.easy.statemachine.workflow.interceptor;

import static ambitor.easy.statemachine.workflow.model.StateMachineConstant.TASK_HEADER;

import ambitor.easy.statemachine.core.StateMachine;
import ambitor.easy.statemachine.core.context.Message;
import ambitor.easy.statemachine.core.interceptor.AbstractStateMachineInterceptor;
import ambitor.easy.statemachine.core.state.State;
import ambitor.easy.statemachine.core.transition.Transition;
import ambitor.easy.statemachine.workflow.model.StateMachineConstant;
import ambitor.easy.statemachine.workflow.model.StateMachineLog;
import ambitor.easy.statemachine.workflow.model.StateMachineTask;
import ambitor.easy.statemachine.workflow.service.StateMachineLogService;
import ambitor.easy.statemachine.workflow.service.StateMachineTaskService;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.PropertyFilter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 持久化拦截器，状态发生改变后把当前状态信息持久化
 *
 * @author Ambitor
 */
@Slf4j
@Component
public class PersistStateMachineInterceptor<S, E> extends AbstractStateMachineInterceptor<S, E> {

    //加上action唯一执行key，方便日志查看
    public static final String TRANSITION_UNIQUE_ID = "transition_unique_id";
    public static final int MID_TEXT_LENGTH = 1677721;
    @Autowired
    private StateMachineTaskService stateMachineTaskService;
    @Autowired
    private StateMachineLogService stateMachineLogService;

    @Override
    public void afterStateChange(State<S, E> target, Message<E> message, Transition<S, E> transition,
        StateMachine<S, E> stateMachine) {
        log.info("状态改变持久化到数据库");
        StateMachineTask task =
            (StateMachineTask)message.getHeaders().getHeaders().get(StateMachineConstant.TASK_HEADER);
        String tid = task.getId() + "-" + System.currentTimeMillis();
        log.info("状态发生改变 tid->{}", tid);
        message.getHeaders().addHeader(TRANSITION_UNIQUE_ID, tid);
        //上下文
        String context = getContext(message);
        //保存转换日志
        String response = getResponse(task.getResponseData());
        saveLog(task.getMachineCode(), message.getPayload().toString(), transition.getSource().getId().toString(),
            target.getId().toString(), Transition.SUCCESS, response, context);
        //修改数据库
        StateMachineTask update = new StateMachineTask();
        update.setId(task.getId());
        update.setTransactionId(task.getTransactionId());
        update.setMachineState(target.getId().toString());
        update.setRequestData(task.getRequestData());
        update.setResponseData(response);
        update.setMachineContext(context);
        stateMachineTaskService.updateByPrimaryKeySelective(update);
    }

    @Override
    public Exception stateMachineError(StateMachine<S, E> stateMachine, Message<E> eventMsg, Exception e) {
        Transition<S, E> transition = stateMachine.transition();
        //保存转换日志
        StateMachineTask task =
            (StateMachineTask)stateMachine.getEvent().getHeaders().getHeaders().get(StateMachineConstant.TASK_HEADER);
        String errorMsg = ExceptionUtils.getStackTrace(e);
        String context = getContext(eventMsg);
        saveLog(task.getMachineCode(), transition.getEvent().toString(), stateMachine.getState().getId().toString(),
            transition.getTarget().getId().toString(), Transition.FAILED, errorMsg, context);
        //修改数据库
        StateMachineTask update = new StateMachineTask();
        update.setTransactionId(task.getTransactionId());
        update.setId(task.getId());
        update.setMachineContext(context);
        update.setResponseData(errorMsg);
        stateMachineTaskService.updateByPrimaryKeySelective(update);
        return e;
    }

    private void saveLog(String code, String event, String source, String target, String result, String response,
        String context) {
        StateMachineTask original = stateMachineTaskService.findByCode(code);
        //保存log
        StateMachineLog record = new StateMachineLog();
        record.setMachineCode(code);
        record.setEvent(event);
        record.setSource(source);
        record.setTarget(target);
        record.setTransitionResult(result);
        record.setRequest(original.getRequestData());
        record.setResponse(response);
        record.setMachineContext(context);
        stateMachineLogService.insertSelective(record);
    }

    private String getContext(Message<E> message) {
        String context =
            JSON.toJSONString(message.getHeaders(), (PropertyFilter)(o, key, value) -> !TASK_HEADER.equals(key));
        context = context.length() > MID_TEXT_LENGTH ? context.substring(0, MID_TEXT_LENGTH) : context;
        return context;
    }

    private String getResponse(String response) {
        return response.length() > MID_TEXT_LENGTH ? response.substring(0, MID_TEXT_LENGTH) : response;
    }

}
