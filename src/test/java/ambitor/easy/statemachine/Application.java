package ambitor.easy.statemachine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Created by Ambitor on 2019/3/20
 */
@SpringBootApplication(scanBasePackages = {"ambitor"})
public class Application {


    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Application.class);
        StateMachineHelper stateMachineHelper = context.getBean(StateMachineHelper.class);
        stateMachineHelper.execute();
    }
}
