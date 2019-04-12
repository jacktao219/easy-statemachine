package ambitor.easy.statemachine.parser;

import ambitor.easy.statemachine.core.configurer.StateMachineConfigurer;

/**
 * 状态机解析器
 * Created by Ambitor on 2019/4/10
 * @author Ambitor
 */
public interface StateMachineParser<T> {

    /**
     * 解析器
     * @param config
     * @return
     */
    StateMachineConfigurer parser(T config);
}
