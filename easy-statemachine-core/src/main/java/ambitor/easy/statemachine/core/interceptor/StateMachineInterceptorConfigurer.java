package ambitor.easy.statemachine.core.interceptor;

/**
 * 状态机拦截器配置
 * @author Ambitor
 * @param <S> 状态
 * @param <E> 事件
 */
public interface StateMachineInterceptorConfigurer<S, E> {

    /**
     * 注册拦截器
     * @param interceptor
     * @return
     */
    boolean register(StateMachineInterceptor<S, E> interceptor);
}
