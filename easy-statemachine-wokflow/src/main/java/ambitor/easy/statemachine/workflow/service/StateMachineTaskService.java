package ambitor.easy.statemachine.workflow.service;

import ambitor.easy.statemachine.workflow.model.StateMachineTask;

import java.util.List;

/**
 * 任务service
 * Created by Ambitor on 2019/3/20
 */
public interface StateMachineTaskService {

    /**
     * 新增状态机
     * @param task 状态机
     * @return 影响行
     */
    int insertSelective(StateMachineTask task);

    /**
     * 根据机器code查询
     * @param code 机器码
     * @return 状态机
     */
    StateMachineTask findByCode(String code);

    /**
     * 根据机器TransactionId查询
     * @param transactionId 唯一编号
     * @return 状态机
     */
    StateMachineTask findByTransactionId(String transactionId);

    /**
     * 根据主键修改状态机任务
     * @param task 状态机
     * @return 影响行
     */
    int updateByPrimaryKeySelective(StateMachineTask task);

    /**
     * 获取需要执行的状态机任务
     * @return 状态机任务
     */
    List<StateMachineTask> getExecuteTask();


}
