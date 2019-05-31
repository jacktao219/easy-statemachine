package ambitor.easy.statemachine.sf.statemachine;


import ambitor.easy.statemachine.core.annotation.EnableWithStateMachine;
import ambitor.easy.statemachine.core.configurer.adapter.AbstractStateMachineConfigurerAdapter;
import ambitor.easy.statemachine.core.guard.DefaultGuard;
import ambitor.easy.statemachine.core.interceptor.StateMachineInterceptorConfigurer;
import ambitor.easy.statemachine.core.state.config.StateConfigurer;
import ambitor.easy.statemachine.core.transition.config.TransitionConfigurer;
import ambitor.easy.statemachine.sf.action.CreateCardIIAction;
import ambitor.easy.statemachine.sf.action.DocumentCreditAction;
import ambitor.easy.statemachine.sf.action.FinishAction;
import ambitor.easy.statemachine.sf.action.GrantAction;
import ambitor.easy.statemachine.sf.enumerate.GrantEvent;
import ambitor.easy.statemachine.sf.enumerate.GrantState;
import ambitor.easy.statemachine.workflow.interceptor.PersistStateMachineInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.EnumSet;

import static ambitor.easy.statemachine.sf.enumerate.GrantConstant.*;

@Slf4j
@EnableWithStateMachine
public class GrantStateMachineConfig extends AbstractStateMachineConfigurerAdapter<GrantState, GrantEvent> {

    @Override
    public void configure(StateConfigurer<GrantState, GrantEvent> states) {
        states
                .newStatesConfigurer()
                // 定义初始状态
                .initial(GrantState.WAIT_CREATE_CARDII)
                // 定义挂起状态，遇到此状态后状态机自动挂起，直到再次手动触发事件(回调中)，状态机继续往下执行
                .suspend(GrantState.WAIT_DOCUMENT_CREDIT_CALLBACK)
                // 放款校验状态
                .suspend(GrantState.WAIT_GRANT_CHECK)
                // 定义所有状态集合
                .states(EnumSet.allOf(GrantState.class))
                //定义结束状态
                .end(GrantState.CREATE_CARDII_FAILED)
                .end(GrantState.DOCUMENT_CREDIT_FAILED)
                .end(GrantState.GRANT_FAILED)
                .end(GrantState.GRANT_SUCCESS);
    }

    /**
     * 状态扭转器配置，配置工作流通过 event（触发事件）把状态从
     * source status（源状态) 转到 target status (目标状态)
     * 可根据上一步建档授信结果转换成不同的 target status (目标状态)
     * 具体配置情况如下
     */
    @Override
    public void configure(TransitionConfigurer<GrantState, GrantEvent> transitions) {
        transitions
                /**  1、等待创建二类户  **/
                //标准转换器，不具备结果判断，事件触发后只能从X状态转为Y状态
                .standardTransition()
                .source(GrantState.WAIT_CREATE_CARDII)
                .target(GrantState.WAIT_DOCUMENT_CREDIT)
                .event(GrantEvent.CREATE_CARDII)
                .action(createCardIIAction, createCardIIAction.errorAction(s -> log.info("创建二类户异常")))
                .and()
                /**  2、等待建档授信步骤  **/
                //具备选择结果的转换器，可根据当前事件执行结果扭转到不同状态
                .choiceTransition()
                //原状态为等待建档授信
                .source(GrantState.WAIT_DOCUMENT_CREDIT)
                //first相当于if，如果建档授信状态返回DOCUMENT_CREDIT_SUCCESS则转换成WAIT_GRANT等待放款
                .first(GrantState.WAIT_GRANT, DefaultGuard.condition(DOCUMENT_CREDIT_STATUS, DOCUMENT_CREDIT_SUCCESS))
                //then相当于elseif，如果建档授信状态返回WAIT_DOCUMENT_CREDIT_CALLBACK则转换成等待建档授信回调
                .then(GrantState.WAIT_DOCUMENT_CREDIT_CALLBACK, DefaultGuard.condition(DOCUMENT_CREDIT_STATUS, WAIT_DOCUMENT_CREDIT_CALLBACK))
                //last相当于else，如果都不是则返回建档授信失败
                .last(GrantState.DOCUMENT_CREDIT_FAILED)
                //触发事件
                .event(GrantEvent.DOCUMENT_CREDIT)
                //事件执行的action
                .action(documentCreditAction, documentCreditAction.errorAction())
                .and()
                /**  3、等待建档授信回调步骤  **/
                .choiceTransition()
                .source(GrantState.WAIT_DOCUMENT_CREDIT_CALLBACK)
                .first(GrantState.WAIT_GRANT, DefaultGuard.condition(DOCUMENT_CREDIT_STATUS, DOCUMENT_CREDIT_SUCCESS))
                .last(GrantState.DOCUMENT_CREDIT_FAILED)
                .event(GrantEvent.DOCUMENT_CREDIT_CALLBACK)
                .and()
                /**  4、等待放款流程 **/
                .choiceTransition()
                .source(GrantState.WAIT_GRANT)
                .first(GrantState.GRANT_TASK_SAVE, DefaultGuard.condition(GRANT_STATUS, GRANT_SUCCESS))
                .last(GrantState.WAIT_GRANT_CHECK)
                .event(GrantEvent.GRANTED)
                .action(grantAction)
                .and()
                /** 5、放款检查流程，如果上一步操作超时 **/
                .choiceTransition()
                .source(GrantState.WAIT_GRANT_CHECK)
                .first(GrantState.GRANT_TASK_SAVE, DefaultGuard.condition(GRANT_STATUS, GRANT_SUCCESS))
                .last(GrantState.GRANT_FAILED)
                .event(GrantEvent.GRANT_CHECKED)
                .and()
                /** 6、最后完成的流程 **/
                .standardTransition()
                .source(GrantState.GRANT_TASK_SAVE).target(GrantState.GRANT_SUCCESS)
                .event(GrantEvent.FINISHED)
                .action(finishAction);
    }

    /**
     * 注册拦截器
     */
    @Override
    public void configure(StateMachineInterceptorConfigurer<GrantState, GrantEvent> interceptors) {
        //状态改变持久化到数据库拦截器
        interceptors.register(persistStateMachineInterceptor);
    }

    /**
     * 状态机名称
     */
    @Override
    public String getName() {
        return "SF";
    }

    @Autowired
    private PersistStateMachineInterceptor<GrantState, GrantEvent> persistStateMachineInterceptor;
    @Autowired
    private CreateCardIIAction createCardIIAction;
    @Autowired
    private DocumentCreditAction documentCreditAction;
    @Autowired
    private GrantAction grantAction;
    @Autowired
    private FinishAction finishAction;

}
