package ambitor.easy.statemachine.workflow.service;

import ambitor.easy.statemachine.core.StateMachine;
import ambitor.easy.statemachine.core.annotation.EnableWithStateMachine;
import ambitor.easy.statemachine.core.configurer.StateMachineConfigurer;
import ambitor.easy.statemachine.core.context.MessageHeaders;
import ambitor.easy.statemachine.core.exception.StateMachineException;
import ambitor.easy.statemachine.core.exception.StateMachineRetryException;
import ambitor.easy.statemachine.core.factory.StateMachineFactory;
import ambitor.easy.statemachine.parser.StateMachineParser;
import ambitor.easy.statemachine.parser.yml.StateMachineYmlConfig;
import ambitor.easy.statemachine.parser.yml.StateMachineYmlParser;
import ambitor.easy.statemachine.workflow.model.StateMachineTask;
import ambitor.easy.statemachine.workflow.model.TaskStatus;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.TargetClassAware;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static ambitor.easy.statemachine.workflow.model.StateMachineConstant.TASK_HEADER;

/**
 * 状态机service
 * @author Ambitor
 */
@Slf4j
@Component
public abstract class AbstractStateMachineService implements ApplicationContextAware, StateMachineService {

    /**
     * 通过状态机名称拿到状态机Configurer
     * @param stateMachineName 状态机名称
     * @return 配置
     */
    @SuppressWarnings("unchecked")
    public <S, E> StateMachineConfigurer<S, E> getByName(String stateMachineName) {
        if (StringUtils.isEmpty(stateMachineName)) {
            throw new StateMachineException("状态机名称为空");
        }
        StateMachineConfigurer configurer = configCache.get(stateMachineName);
        if (configurer != null) {
            return configurer;
        }
        Map<String, Object> stateMachineConfigs = context.getBeansWithAnnotation(EnableWithStateMachine.class);
        Collection<Object> values = stateMachineConfigs.values();
        if (!CollectionUtils.isEmpty(values)) {
            for (Object stateMachineConfig : values) {
                StateMachineConfigurer adapter = (StateMachineConfigurer) stateMachineConfig;
                if (stateMachineName.equals(adapter.getName())) {
                    configCache.put(stateMachineName, adapter);
                    return adapter;
                }
            }
        }
        //根据Yml初始化状态机配置，并注入spring bean 容器
        registerStateMachineConfigBean(stateMachineName);
        configurer = context.getBean(stateMachineName, StateMachineConfigurer.class);
        configCache.put(stateMachineName, configurer);
        return configurer;
    }

    /**
     * 通过定时调度启动任务
     * 1、从数据库中将任务查询出来
     * 2、标记任务为运行中
     * 3、将任务放入到MQ中
     * 注意事务处理，忽略超时重试的场景
     */
    @Override
    public List<StateMachineTask> execute() {
        List<StateMachineTask> list = taskService.getExecuteTask();
        for (StateMachineTask task : list) {
            updateAndSendToMq(task);
        }
        return list;
    }

    /**
     * 更新状态后推送到mq
     * @param task 任务
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSendToMq(StateMachineTask task) {
        Date now = new Date();
        task.setCurrentTrytimes((1 + task.getCurrentTrytimes()));
        task.setScanStatus(TaskStatus.running.name());
        task.setUpdateTime(now);
        int affect = taskService.updateByPrimaryKeySelective(task);
        if (affect > 0) {
            sendToMq(task);
        } else {
            log.info("修改状态机任务为Running失败，不放入MQ task->{}", task.getTransactionId());
        }
    }

    /**
     * 执行task
     * @param task task
     */
    @Override
    @SuppressWarnings("unchecked")
    public <S, E> void processTask(StateMachineTask task) {
        String transactionId = task.getTransactionId();
        log.info(" 状态机开始执行:{}", JSON.toJSONString(task));
        try {
            boolean locked = lock(transactionId);
            if (!locked) {
                log.info("状态机执行时获取锁失败 transactionId->{}", transactionId);
                throw new StateMachineRetryException("状态机执行时获取锁失败");
            }
            StateMachineConfigurer<S, E> configurer = getByName(task.getMachineType());
            //生成一个状态机
            StateMachine<S, E> stateMachine = StateMachineFactory.build(configurer);
            //获取状态机泛型类型
            Class genericClass = getGenericSuperclass(configurer);
            if (genericClass.isEnum()) {
                stateMachine.resetStateMachine((S) Enum.valueOf(genericClass, task.getMachineState()));
            } else if (genericClass == String.class) {
                stateMachine.resetStateMachine((S) task.getMachineState());
            }
            MessageHeaders headers = new MessageHeaders();
            headers.addHeader(TASK_HEADER, task);
            boolean accept = stateMachine.start(headers);
            StateMachineTask update = new StateMachineTask();
            update.setId(task.getId());
            log.info("{}状态机执行结束,accept：{},当前状态{},异常{}", transactionId, accept,
                    stateMachine.getState().getId(), stateMachine.getStateMachineError());
            if (!accept) {
                //没有接受的话保存异常信息到StateMachineTask
                update.setResponseData(JSON.toJSONString(stateMachine.getStateMachineError()));
                update.setScanStatus(task.isLastRetry() ? TaskStatus.close.name() : TaskStatus.error.name());
                update.setNextRunTime(localDateTime2Date(LocalDateTime.now().plusMinutes(5)));
            } else {
                //如果不是结束状态，并且接受了，则重置currentTryTimes
                update.setCurrentTrytimes(0);
                //设置response
                update.setResponseData(task.getResponseData());
                //设置scanStatus
                update.setScanStatus(stateMachine.getState().isSuspend() ? TaskStatus.suspend.name() : TaskStatus.open.name());
                //如果是结束状态
                if (stateMachine.getState().isEnd()) {
                    update.setScanStatus(TaskStatus.close.name());
                }
            }
            taskService.updateByPrimaryKeySelective(update);
        } catch (Exception e) {
            log.error("{}状态机执行发生异常", transactionId, e);
            StateMachineTask update = new StateMachineTask();
            //没有接受的话保存异常信息到StateMachineTask
            update.setId(task.getId());
            update.setResponseData(JSON.toJSONString(e));
            update.setScanStatus(task.isLastRetry() ? TaskStatus.close.name() : TaskStatus.error.name());
            update.setNextRunTime(localDateTime2Date(LocalDateTime.now().plusMinutes(5)));
            taskService.updateByPrimaryKeySelective(update);
        } finally {
            log.info("{} 释放锁", transactionId);
            unLock(transactionId);
        }
    }

    public static boolean isCglibProxy(@Nullable Object object) {
        return (object instanceof SpringProxy &&
                object.getClass().getName().contains(ClassUtils.CGLIB_CLASS_SEPARATOR));
    }

    public static Class<?> getTargetClass(Object candidate) {
        if (candidate == null) {
            throw new StateMachineException("candidate can not be null");
        }
        Class<?> result = null;
        if (candidate instanceof TargetClassAware) {
            result = ((TargetClassAware) candidate).getTargetClass();
        }
        if (result == null) {
            result = (isCglibProxy(candidate) ? candidate.getClass().getSuperclass() : candidate.getClass());
        }

        return result;
    }

    /**
     * 获取状态机状态的泛型Class对象
     * @param configurer 状态机
     * @return 状态机<S>状态的泛型Class
     */
    private static Class getGenericSuperclass(StateMachineConfigurer configurer) {
        Class aClass = genericSuperclassCache.get(configurer);
        if (aClass != null) {
            return aClass;
        }
        Class<?> result = getTargetClass(configurer);
        Type genericSuperclass = result.getGenericSuperclass();
        while (!(genericSuperclass instanceof ParameterizedTypeImpl)) {
            result = result.getSuperclass();
            genericSuperclass = result.getGenericSuperclass();
        }
        ParameterizedTypeImpl type = (ParameterizedTypeImpl) genericSuperclass;
        Class genericClass = (Class) type.getActualTypeArguments()[0];
        genericSuperclassCache.put(configurer, genericClass);
        return genericClass;
    }


    /**
     * 获取容器
     * @param applicationContext 上下文
     * @throws BeansException 异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        context = applicationContext;
    }

    /**
     * 根据Yml初始化状态机配置，并注入spring bean 容器
     */
    private void registerStateMachineConfigBean(String stateMachineName) {
        try {
            DefaultListableBeanFactory beanFactory = (DefaultListableBeanFactory) context.getAutowireCapableBeanFactory();
            StateMachineParser<StateMachineYmlConfig> stateMachineParser = beanFactory.getBean(StateMachineYmlParser.class);
            //实例化解析器
            Yaml yaml = new Yaml();
            //配置文件地址
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(CLASSPATH_STATEMACHINE_YML);
            for (Resource resource : resources) {
                File file = resource.getFile();
                FileInputStream fileInputStream = new FileInputStream(file);
                StateMachineYmlConfig config = yaml.loadAs(fileInputStream, StateMachineYmlConfig.class);
                String name = config.getName();
                if (name == null || name.length() <= 0) {
                    throw new StateMachineException("please defined name with .yml config");
                }
                if (!stateMachineName.equals(name)) {
                    continue;
                }
                log.info("load StateMachineYmlConfig {}", file.getName());
                StateMachineConfigurer stateMachineConfigurer = stateMachineParser.parser(config);
                if (beanFactory.containsBean(name)) {
                    throw new StateMachineException("StateMachine bean name '" + name + "' has conflicts with existing");
                }
                beanFactory.registerSingleton(name, stateMachineConfigurer);
            }
        } catch (FileNotFoundException e) {
            log.info("No StateMachineYmlConfig Found");
        } catch (IOException e) {
            throw new BeanCreationException("StateMachine.yml IOException", e);
        }
    }

    /**
     * 时间转换
     * @param localDateTime 时间
     */
    private static Date localDateTime2Date(LocalDateTime localDateTime) {
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zone).toInstant();
        return Date.from(instant);
    }


    /**
     * 放入MQ
     * @param task 任务
     */
    public abstract void sendToMq(StateMachineTask task);

    /**
     * 对machineTask加锁
     * @param transactionId 状态机的事务ID
     * @return true 成功 false 失败
     */
    public abstract boolean lock(String transactionId);

    /**
     * 对machineTask解锁
     * @param transactionId 状态机的事务ID
     * @return true 成功 false 失败
     */
    public abstract boolean unLock(String transactionId);

    @Autowired
    private StateMachineTaskService taskService;
    private static ApplicationContext context = null;
    private static Map<String, StateMachineConfigurer> configCache = new ConcurrentHashMap<>();
    private static Map<StateMachineConfigurer, Class> genericSuperclassCache = new ConcurrentHashMap<>();
    private static final String CLASSPATH_STATEMACHINE_YML = "classpath:statemachine\\*statemachine*.yml";

}
