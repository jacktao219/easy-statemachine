package ambitor.easy.statemachine;

import ambitor.easy.statemachine.parser.yml.StateMachineYmlConfig;
import ambitor.easy.statemachine.workflow.model.StateMachineTask;
import ambitor.easy.statemachine.workflow.service.StateMachineService;
import com.alibaba.fastjson.JSON;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

    public static void analysis() throws IOException {
        //实例化解析器
        Yaml yaml = new Yaml();
        //配置文件地址
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath:statemachine\\*statemachine.yml");
        for (Resource resource : resources) {
            File file = resource.getFile();
            FileInputStream fileInputStream = new FileInputStream(file);
            //装载的对象，这里使用Map, 当然也可使用自己写的对象
            StateMachineYmlConfig config = yaml.loadAs(fileInputStream, StateMachineYmlConfig.class);
            System.out.println(JSON.toJSONString(config));
        }

    }

}
