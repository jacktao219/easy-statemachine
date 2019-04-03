package ambitor.easy.statemachine.sf.service;

import ambitor.easy.statemachine.workflow.model.StateMachineTask;
import ambitor.easy.statemachine.workflow.service.AbstractStateMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created by Ambitor on 2019/4/2
 */
@Slf4j
@Component
public class StateMachineServiceImpl extends AbstractStateMachineService {
    /**
     * 放入MQ
     * @param task
     */
    @Override
    public void sendToMq(StateMachineTask task) {
        log.info("发送状态机消息到MQ transactionId->{}", task.getTransactionId());
    }

    /**
     * 对machineTask加锁
     * @param transactionId 状态机的事务ID
     * @return true 成功 false 失败
     */
    @Override
    public boolean lock(String transactionId) {
        log.info("对状态机加锁 transactionId ->{}", transactionId);
        return true;
    }

    /**
     * 对machineTask解锁
     * @param transactionId 状态机的事务ID
     * @return true 成功 false 失败
     */
    @Override
    public boolean unLock(String transactionId) {
        log.info("对状态机解锁 transactionId ->{}", transactionId);
        return true;
    }
}
