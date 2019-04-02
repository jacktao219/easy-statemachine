package ambitor.easy.statemachine;

import ambitor.easy.statemachine.model.StateMachineTask;
import ambitor.easy.statemachine.service.StateMachineService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 状态机任务帮助类
 * Created by Ambitor on 2019/4/2
 */
@Slf4j
@Component
public class StateMachineHelper {
    @Autowired
    private StateMachineService stateMachineService;

    /**
     * 通过定时调度启动任务
     * 1、从数据库中将任务查询出来
     * 2、标记任务为运行中
     * 3、将任务放入到MQ中
     * 注意事务处理，忽略超时重试的场景
     */
    public void execute() {
        List<StateMachineTask> tasks = stateMachineService.execute();
        for (StateMachineTask task : tasks) {
            stateMachineService.processTask(task);
        }
    }
}
