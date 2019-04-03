package ambitor.easy.statemachine.workflow.service;

import ambitor.easy.statemachine.workflow.model.StateMachineLog;

/**
 * 状态机日志记录
 */
public interface StateMachineLogService {

    int insertSelective(StateMachineLog record);

}