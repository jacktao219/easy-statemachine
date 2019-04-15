package ambitor.easy.statemachine.service;

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
        task.setMachineType("sf");
        task.setRequestData("mock模拟状态机Task");
        task.setRetryTimes(3);
        task.setTransactionId(String.valueOf(System.currentTimeMillis()));
        tasks.add(task);

        StateMachineTask task1 = new StateMachineTask();
        task1.setMachineState("准备食材");
        task1.setNextRunTime(new Date());
        task1.setScanStatus(TaskStatus.open.name());
        task1.setCurrentTrytimes(0);
        task1.setMachineType("eat");
        task1.setRequestData("mock模拟状态机Task");
        task1.setRetryTimes(3);
        task1.setTransactionId(String.valueOf(System.currentTimeMillis()));
        tasks.add(task1);

        StateMachineTask task2 = new StateMachineTask();
        task2.setMachineState(SFGrantState.WAIT_CREATE_CARDII.name());
        task2.setNextRunTime(new Date());
        task2.setScanStatus(TaskStatus.open.name());
        task2.setCurrentTrytimes(0);
        task2.setMachineType("SF");
        task2.setRequestData("mock模拟状态机Task");
        task2.setRetryTimes(3);
        task2.setTransactionId(String.valueOf(System.currentTimeMillis()));
        tasks.add(task2);
        return tasks;
    }
}
