package ambitor.easy.statemachine.sf.service;

import ambitor.easy.statemachine.sf.enumerate.SFGrantState;
import ambitor.easy.statemachine.workflow.model.StateMachineTask;
import ambitor.easy.statemachine.workflow.model.TaskStatus;
import ambitor.easy.statemachine.workflow.service.StateMachineTaskService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Ambitor on 2019/4/2
 */
@Component
@Slf4j
public class StateMachineTaskServiceImpl implements StateMachineTaskService {
    /**
     * 新增状态机
     * @param task 状态机
     * @return 影响行
     */
    @Override
    public int insertSelective(StateMachineTask task) {
        log.info("新增状态机 transactionId ->{}", task.getTransactionId());
        return 1;
    }

    /**
     * 根据主键修改状态机任务
     * @param task 状态机
     * @return 影响行
     */
    @Override
    public int updateByPrimaryKeySelective(StateMachineTask task) {
        log.info("修改状态机 transactionId ->{}", task.getTransactionId());
        return 1;
    }

    /**
     * 获取需要执行的状态机任务
     * @return 状态机任务
     */
    @Override
    public List<StateMachineTask> getExecuteTask() {
        List<StateMachineTask> tasks = new ArrayList<>();
        StateMachineTask task = new StateMachineTask();
        task.setMachineState(SFGrantState.WAIT_CREATE_CARDII.name());
        task.setNextRunTime(new Date());
        task.setScanStatus(TaskStatus.open.name());
        task.setCurrentTrytimes(0);
        task.setMachineType("SF");
        task.setRequestData("mock模拟状态机Task");
        task.setRetryTimes(3);
        task.setTransactionId(String.valueOf(System.currentTimeMillis()));
        tasks.add(task);
        return tasks;
    }
}
