package ambitor.easy.statemachine.parser.yml;

import ambitor.easy.statemachine.core.action.Action;
import ambitor.easy.statemachine.core.configurer.StateMachineConfigurer;
import ambitor.easy.statemachine.core.configurer.adapter.AbstractStateMachineConfigurerAdapter;
import ambitor.easy.statemachine.core.enumerate.TransitionType;
import ambitor.easy.statemachine.core.exception.StateMachineException;
import ambitor.easy.statemachine.core.guard.DefaultGuard;
import ambitor.easy.statemachine.core.interceptor.StateMachineInterceptor;
import ambitor.easy.statemachine.core.interceptor.StateMachineInterceptorConfigurer;
import ambitor.easy.statemachine.core.state.config.StateConfigurer;
import ambitor.easy.statemachine.core.transition.config.ChoiceTransitionConfigurer;
import ambitor.easy.statemachine.core.transition.config.StandardTransitionConfigurer;
import ambitor.easy.statemachine.core.transition.config.TransitionConfigurer;
import ambitor.easy.statemachine.parser.StateMachineParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.StringUtils;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * yml解析器
 * Created by Ambitor on 2019/4/9
 * @author Ambitor
 */
@Slf4j
@Component
public class StateMachineYmlParser implements ApplicationContextAware, StateMachineParser<StateMachineYmlConfig> {

    public static final String POINT = ".";

    /**
     * 状态机YML配置解析方法
     * @param config
     * @return
     */
    @Override
    public StateMachineConfigurer parser(StateMachineYmlConfig config) {
        return new AbstractStateMachineConfigurerAdapter<String, String>() {
            @Override
            public void configure(StateConfigurer<String, String> states) {
                stateParser(states, config);
            }

            @Override
            public void configure(TransitionConfigurer<String, String> transitions) {
                transitionParser(transitions, config);
            }

            @Override
            public void configure(StateMachineInterceptorConfigurer<String, String> interceptors) {
                interceptorParser(interceptors, config);
            }

            @Override
            public String getName() {
                return config.getName();
            }
        };
    }

    /**
     * 拦截器解析
     * @param interceptors 拦截器配置
     * @param config       yml配置文件
     */
    private void interceptorParser(StateMachineInterceptorConfigurer<String, String> interceptors, StateMachineYmlConfig config) {
        List<String> interceptor = config.getInterceptor();
        if (interceptor == null || interceptor.size() == 0) {
            log.info("No StateMachineInterceptorConfigurer Found");
            return;
        }
        for (String interceptorName : interceptor) {
            StateMachineInterceptor expect = getBeanByClassName(StateMachineInterceptor.class, interceptorName);
            if (expect != null) {
                interceptors.register(expect);
            }
        }
    }

    /**
     * 转换器配置
     * @param transitions 转换器配置
     * @param config      yml配置文件
     */
    @SuppressWarnings("unchecked")
    private void transitionParser(TransitionConfigurer<String, String> transitions, StateMachineYmlConfig config) {
        List<StateMachineYmlConfig.TransitionEntry> configTransitions = config.getTransitions();
        if (configTransitions != null && configTransitions.size() > 0) {
            for (int i = 0; i < configTransitions.size(); i++) {
                if (i > 0) {
                    transitions.and();
                }
                StateMachineYmlConfig.TransitionEntry entry = configTransitions.get(i);
                String type = entry.getType();
                String source = entry.getSource();
                Action<String, String> action = getBeanByClassName(Action.class, entry.getAction());
                String errorActionName = entry.getErrorAction();
                Action<String, String> errorAction = null;
                if (!StringUtils.isEmpty(errorActionName) && errorActionName.contains(POINT)) {
                    //支持ClassName.method() 通过方法返回匿名类,只支持无参方法
                    String[] array = errorActionName.split("\\" + POINT);
                    String className = array[0];
                    Action<String, String> error = getBeanByClassName(Action.class, className);
                    if (error == null) {
                        throw new StateMachineException("can not found error class " + className);
                    }
                    String methodName = array[1];
                    Method method = ReflectionUtils.findMethod(error.getClass(), methodName);
                    if (method == null) {
                        throw new StateMachineException("can not found method " + methodName + " in class " + className);
                    }
                    Object object = ReflectionUtils.invokeMethod(method, error);
                    if (!(object instanceof Action)) {
                        throw new StateMachineException(config.getName() + " machine " + errorActionName + " errorAction return value is not Action instance");
                    }
                    errorAction = (Action<String, String>) object;
                } else {
                    errorAction = getBeanByClassName(Action.class, errorActionName);
                }
                String event = entry.getEvent();
                if (TransitionType.standard.name().equals(type)) {
                    String target = entry.getTarget();
                    StandardTransitionConfigurer<String, String> standard = transitions.standardTransition();
                    standard.source(source).target(target).event(event).action(action, errorAction);
                    transitions = standard;
                } else if (TransitionType.choice.name().equals(type)) {
                    StateMachineYmlConfig.ChoiceTransitionVO first = entry.getFirst();
                    StateMachineYmlConfig.ChoiceTransitionVO then = entry.getThen();
                    StateMachineYmlConfig.ChoiceTransitionVO last = entry.getLast();
                    ChoiceTransitionConfigurer<String, String> choice = transitions.choiceTransition();
                    choice.source(source);
                    if (first != null) {
                        choice.first(first.getTarget(), DefaultGuard.condition(first.getStatus(), first.getEquals()));
                    }
                    if (then != null) {
                        choice.then(then.getTarget(), DefaultGuard.condition(then.getStatus(), then.getEquals()));
                    }
                    if (last != null) {
                        choice.last(last.getTarget());
                    }
                    choice.event(event).action(action, errorAction);
                    transitions = choice;
                } else {
                    throw new StateMachineException(type + " type not support");
                }
            }
        } else {
            throw new StateMachineException("please defined transitions with .yml config");
        }
    }

    /**
     * 状态解析
     * @param states 状态配置
     * @param config yml配置文件
     */
    private void stateParser(StateConfigurer<String, String> states, StateMachineYmlConfig config) {
        StateMachineYmlConfig.StateEntry stateEntry = config.getStates();
        if (stateEntry == null) {
            throw new StateMachineException("please defined states with .yml config");
        }
        StateConfigurer<String, String> stateConfig = states.newStatesConfigurer();
        // 定义初始状态
        stateConfig.initial(stateEntry.getInit());
        // 定义挂起状态，遇到此状态后状态机自动挂起，直到再次手动触发事件(回调中)，状态机继续往下执行
        List<String> suspend = stateEntry.getSuspend();
        if (suspend != null && suspend.size() > 0) {
            for (String s : suspend) {
                stateConfig.suspend(s);
            }
        }
        // 定义其它状态集合
        stateConfig.states(new HashSet<>(stateEntry.getOther()));
        // 定义结束状态
        List<String> end = stateEntry.getEnd();
        if (end != null && end.size() > 0) {
            for (String s : end) {
                stateConfig.end(s);
            }
        }
    }

    private <T> T getBeanByClassName(Class<T> clazz, String expect) {
        if (expect != null && expect.length() > 0) {
            Map<String, T> interceptorMap = applicationContext.getBeansOfType(clazz);
            for (String key : interceptorMap.keySet()) {
                if (key.toLowerCase().contains(expect.toLowerCase()) || expect.toLowerCase().contains(key.toLowerCase())) {
                    return interceptorMap.get(key);
                }
            }
        }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private ApplicationContext applicationContext;

}
