package ambitor.easy.statemachine;

import ambitor.easy.statemachine.workflow.model.StateMachineTask;
import ambitor.easy.statemachine.workflow.service.StateMachineService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.List;

/**
 * Created by Ambitor on 2019/3/20
 */
@SpringBootApplication(scanBasePackages = {"ambitor"})
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class);
        StateMachineService stateMachineService = context.getBean(StateMachineService.class);
        List<StateMachineTask> tasks = stateMachineService.execute();
        for (StateMachineTask task : tasks) {
            stateMachineService.processTask(task);
        }
    }
}
