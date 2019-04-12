package ambitor.easy.statemachine.workflow.service;

import ambitor.easy.statemachine.workflow.model.StateMachineTask;

import java.util.List;

/**
 * 状态机Service
 * Created by Ambitor on 2019/4/2
 */
public interface StateMachineService {

    /**
     * 通过定时调度启动任务
     * 1、从数据库中将任务查询出来
     * 2、标记任务为运行中
     * 3、将任务放入到MQ中
     * 注意事务处理，忽略超时重试的场景
     * @return 返回获取到的task
     */
    List<StateMachineTask> execute();

    /**
     * 执行task
     * @param task 任务
     */
    <S, E> void processTask(StateMachineTask task);
}
