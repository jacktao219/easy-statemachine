# easy-statemachine
轻量级状态机工作流框架

### 项目介绍
项目目前主要份为3大块，未来会随着功能的丰富拆分更多的项目

- easy-statemachine-core 状态机核心项目，是对有限状态机核心原理实现

- easy-statemachine-wokflow 状态机工作流项目是基于easy-statemachine-core 实现的轻量级状态机工作流框架

- easy-statemachine-demo 可运行Demo

- easy-statemachine-common 分布式中的公共组件，如:通信，选举等(待开发)


#### 有限状态机定义

有限状态机，（英语：Finite-state machine, FSM），又称有限状态自动机，简称状态机，是表示有限个状态以及在这些状态之间的转移和动作等行为的数学模型。有限状态机体现了两点：首先是离散的，然后是有限的。以下是对状态机抽象定义

State（状态）：构成状态机的基本单位。 状态机在任何特定时间都可处于某一状态。从生命周期来看有`Initial State、End State、Suspend State(挂起状态)`

Event（事件）：导致转换发生的事件活动

Transitions（转换器）：两个状态之间的定向转换关系，状态机对发生的特定类型事件响应后当前状态由A转换到B。`标准转换、选择转、子流程转换`多种抽象实现

Actions（转换操作）：在执行某个转换时执行的具体操作。

Guards（检测器）：检测器出现的原因是为了转换操作执行后检测结果是否满足特定条件从一个状态切换到某一个状态

Interceptor（拦截器）：对当前状态改变前、后进行监听拦截。如：每个状态变更后插入日志等

![](https://oscimg.oschina.net/oscnet/9724f02b886173ed28f7a802e1f20e1f40c.jpg)

                                 状态机扭转图

#### 状态机实现  待补
状态机`代码解耦、高效率开发/维护、节省开发成本`等一系列好处....
#### 状态机使用
目前支持以下两种方式配置状态机：
- yml配置文件(推荐)：只需要编写少量的业务实现类和yml配置文件即可完成
- 代码方式配置：稍微复杂，但可以更加直观的了解状态机的配置
- web界面拖拽式配置(未来版本)

#### yml配置方式
```
#状态机名称
name: hxd

#拦截器配置
interceptor:
    - PersistStateMachineInterceptor.class

#状态配置
states:
  init: WAIT_CREATE_CARDII          #等待开二类户
  suspend:
  end:
    - GRANT_FAILED                  #放款失败
    - GRANT_SUCCESS                 #结束流程
  other:
    - WAIT_PRE_VALID                #放款前校验
    - WAIT_PRE_VALID_CHECK          #放款前校验检查
    - WAIT_SIGN_CONTRACT            #等待签署合同
    - WAIT_GRANT                    #放款
    - WAIT_GRANT_CHECK              #等待放款校验
    - GRANT_FAILED_HANDLER          #放款失败处理
    - GRANT_SUCCESS_HANDLER         #放款成功处理

#事件配置
events:
    - CREATE_CARDII                 #开二类户
    - GRANT_VALID                   #放款前落地校验
    - GRANT_VALID_CHECK             #放款前落地校验Check
    - SIGN_CONTRACT                 #签署合同
    - GRANTED                       #放款
    - GRANT_CHECKED                 #放款校验
    - GRANTED_RESULT_HANDLE         #放款结果处理

#转换器配置
transitions:
    - type: choice                            #类型： 选择转换器
      source: WAIT_CREATE_CARDII              #源状态：等待创建二类户
      event:  CREATE_CARDII                   #事件：  创建二类户
      first: {status: GRANT_STATUS,equals: CREATE_CARDII_SUCCESS,target: WAIT_PRE_VALID}
      last: {target: GRANT_FAILED_HANDLER}
      action: BaseCreateCardIIAction.class    #转换操作：创建二类户业务实现类
      errorAction: BaseCreateCardIIAction.errorAction

    - type: choice                            #类型： 选择转换器
      source: WAIT_PRE_VALID                  #源状态：等待放款前落地校验
      event:  GRANT_VALID                     #事件：  放款前落地校验
      first: {status: GRANT_STATUS,equals: GRANT_PRE_VALID_SUCCESS,target: WAIT_SIGN_CONTRACT}
      last: {target: WAIT_PRE_VALID_CHECK}
      action: BasePreValidAction.class        #转换操作：放款前落地校验
      errorAction:

    - type: choice                            #类型： 选择转换器
      source: WAIT_PRE_VALID_CHECK            #源状态：等待放款前落地校验Check
      event: GRANT_VALID_CHECK                #事件：  放款前落地校验Check
      first: {status: GRANT_STATUS,equals: GRANT_PRE_VALID_SUCCESS,target: WAIT_SIGN_CONTRACT}
      last: {target: GRANT_FAILED_HANDLER}
      action: BasePreValidCheckAction.class   #转换操作：放款前落地校验
      errorAction: BasePreValidCheckAction.errorAction

    - type: choice                            #类型： 选择转换器
      source: WAIT_SIGN_CONTRACT              #源状态：等待签署合同
      event: SIGN_CONTRACT                    #事件：  签署合同
      first: {status: GRANT_STATUS,equals: SIGN_CONTRACT_SUCCESS,target: WAIT_GRANT}
      last: {target: GRANT_FAILED_HANDLER}
      action: BaseContractAction.class        #转换操作：签合同Action
      errorAction: BaseContractAction.errorAction

    - type: choice                            #类型：选择转换器
      source: WAIT_GRANT                      #源状态：等待放款
      event:  GRANTED                         #事件：放款
      action: BaseGrantAction.class           #转换操作：放款实现类
      errorAction:
      first: {status: GRANT_STATUS,equals: GRANT_SUCCESS,target: GRANT_SUCCESS_HANDLER}
      last: {target: WAIT_GRANT_CHECK}

    - type: choice
      source: WAIT_GRANT_CHECK                #源状态：等待放款校验
      event:  GRANT_CHECKED                   #事件： 放款校验
      errorAction: BaseGrantCheckAction.errorAction
      first: {status: GRANT_STATUS,equals: GRANT_SUCCESS,target: GRANT_SUCCESS_HANDLER}
      last: {target: GRANT_FAILED_HANDLER}

    - type: standard                          #类型:    标准转换器
      source: GRANT_SUCCESS_HANDLER           #源状态:  放款成功处理
      target: GRANT_SUCCESS                   #目标状态:放款成功
      event:  GRANTED_RESULT_HANDLE           #事件:    放款结果处理
      action: BaseGrantSuccessAction.class #转换操作: 众安放款成功业务实现类

    - type: standard                          #类型:    标准转换器
      source: GRANT_FAILED_HANDLER            #源状态:  放款失败处理
      target: GRANT_FAILED                    #目标状态:放款失败
      event:  GRANTED_RESULT_HANDLE           #事件:    放款结果处理
      action: BaseGrantFailedAction.class

```

#### 代码方式配置：

```
    /**
     * 定义状态枚举
     */
    public enum SFGrantState {
        //等待开二类户
        WAIT_CREATE_CARDII,
        //开二类户失败
        CREATE_CARDII_FAILED,
        //建档授信
        WAIT_DOCUMENT_CREDIT,
        //等待建档授信回调
        WAIT_DOCUMENT_CREDIT_CALLBACK,
        //建档授信失败
        DOCUMENT_CREDIT_FAILED,
        //放款
        WAIT_GRANT,
        //放款失败
        GRANT_FAILED,
        //等待放款校验
        WAIT_GRANT_CHECK,
        //主流程完成
        GRANT_TASK_SAVE,
        //结束流程
        GRANT_SUCCESS
    }

    /**
     * 定义事件枚举
     */
    public enum SFGrantEvent {
        //开二类户
        CREATE_CARDII,
        //建档授信
        DOCUMENT_CREDIT,
        //建档授信回调
        DOCUMENT_CREDIT_CALLBACK,
        //放款
        GRANTED,
        //放款校验
        GRANT_CHECKED,
        //结束
        FINISHED
    }       

    /**
     * 放款工作流配置
     */
    @Slf4j
    @EnableWithStateMachine //集成Spring IOC 的注解
    public class SFGrantStateMachineConfig extends StateMachineConfigurerAdapter<SFGrantState, SFGrantEvent> {

        /**
         * 工作流所有节点状态配置
         */
        @Override
       public void configure(StateConfigurer<SFGrantState, SFGrantEvent> states) {
            states
                    .newStatesConfigurer()
                    // 定义初始状态
                    .initial(SFGrantState.WAIT_CREATE_CARDII)
                    // 定义挂起状态，遇到此状态后状态机自动挂起，直到再次手动触发事件(回调中)，状态机继续往下执行
                    .suspend(SFGrantState.WAIT_DOCUMENT_CREDIT_CALLBACK)
                    // 放款校验状态
                    .suspend(SFGrantState.WAIT_GRANT_CHECK)
                    // 定义其它所有状态集合
                    .states(EnumSet.allOf(SFGrantState.class))
                    //定义结束状态，开二类户失败、建档授信失败、放款失败、放款成功都是结束状态
                    .end(SFGrantState.CREATE_CARDII_FAILED)
                    .end(SFGrantState.DOCUMENT_CREDIT_FAILED)
                    .end(SFGrantState.GRANT_FAILED)
                    .end(SFGrantState.GRANT_SUCCESS);
       }

         /**
         * 状态扭转器配置，配置工作流通过 event（触发事件）把状态从
         * source status（源状态) 转到 target status (目标状态)
         * 可根据上一步建档授信结果转换成不同的 target status (目标状态)
         * 代码完全解耦，把之前在Task中创建下一节点的IF/ELSE抽象成配置信息
         * 具体配置情况如下，细看 2、等待建档授信步骤 
         */
        @Override
        public void configure(TransitionConfigurer<SFGrantState, SFGrantEvent> transitions) {
            transitions
                    /**  1、等待创建二类户  **/
                    .standardTransition()//标准转换器，不具备结果判断，事件触发后只能从X状态转为Y状态
                    .source(SFGrantState.WAIT_CREATE_CARDII)
                    .target(SFGrantState.WAIT_DOCUMENT_CREDIT)
                    .event(SFGrantEvent.CREATE_CARDII)
                    .action(sfCreateCardIIAction, sfCreateCardIIAction.errorAction())
                    .and()

                    /**  2、等待建档授信步骤  **/
                    .choiceTransition()//具备选择结果的转换器，可根据当前事件执行结果扭转到不同状态

                    //原状态为等待建档授信
                    .source(SFGrantState.WAIT_DOCUMENT_CREDIT)

                    //first相当于if，如果建档授信状态返回DOCUMENT_CREDIT_SUCCESS则转换成WAIT_GRANT等待放款
                    .first(SFGrantState.WAIT_GRANT, GrantGuard.condition(DOCUMENT_CREDIT_STATUS, DOCUMENT_CREDIT_SUCCESS))

                    //then相当于elseif，如果建档授信状态返回WAIT_DOCUMENT_CREDIT_CALLBACK则转换成等待建档授信回调
                    .then(SFGrantState.WAIT_DOCUMENT_CREDIT_CALLBACK, GrantGuard.condition(DOCUMENT_CREDIT_STATUS, WAIT_DOCUMENT_CREDIT_CALLBACK))

                    //last相当于else，如果都不是则返回建档授信失败
                    .last(SFGrantState.DOCUMENT_CREDIT_FAILED)

                    //触发事件
                    .event(SFGrantEvent.DOCUMENT_CREDIT)

                    //转换器执行的转换action
                    .action(sfDocumentCreditAction, sfDocumentCreditAction.errorAction())

                    //不同节点转换器的拼接，类似StringBuulder.append()
                    .and()

                    /**  3、等待建档授信回调步骤  **/
                    .choiceTransition()
                    .source(SFGrantState.WAIT_DOCUMENT_CREDIT_CALLBACK)
                    .first(SFGrantState.WAIT_GRANT, GrantGuard.condition(DOCUMENT_CREDIT_STATUS, DOCUMENT_CREDIT_SUCCESS))
                    .last(SFGrantState.DOCUMENT_CREDIT_FAILED)
                    .event(SFGrantEvent.DOCUMENT_CREDIT_CALLBACK)
                    .and()

                    /**  4、等待放款流程 **/
                    .choiceTransition()
                    .source(SFGrantState.WAIT_GRANT)
                    .first(SFGrantState.GRANT_TASK_SAVE, GrantGuard.condition(GRANT_STATUS, GRANT_SUCCESS))
                    .last(SFGrantState.WAIT_GRANT_CHECK)
                    .event(SFGrantEvent.GRANTED)
                    .action(sfGrantAction)
                    .and()

                    /** 5、放款检查流程，如果上一步操作超时 **/
                    .choiceTransition()
                    .source(SFGrantState.WAIT_GRANT_CHECK)
                    .first(SFGrantState.GRANT_TASK_SAVE, GrantGuard.condition(GRANT_STATUS, GRANT_SUCCESS))
                    .last(SFGrantState.GRANT_FAILED)
                    .event(SFGrantEvent.GRANT_CHECKED)
                    .and()

                    /** 6、最后完成的流程 **/
                    .standardTransition()
                    .source(SFGrantState.GRANT_TASK_SAVE).target(SFGrantState.GRANT_SUCCESS)
                    .event(SFGrantEvent.FINISHED)
                    .action(sfFinishAction);
        /**
         * 注册拦截器
         */
        @Override
        public void configure(StateMachineInterceptorConfigurer<SFGrantState, SFGrantEvent> interceptors) {
            //状态改变持久化到数据库拦截器
            interceptors.register(persistStateMachineInterceptor);
        }
    }

```

放款工作流调用代码

```
    //根据工作流名称获取配置信息
    StateMachineConfigurer<S, E> configurer = getByName(task.getMachineType());
    //根据工作流配置创建一个工作流实例
    StateMachine<S, E> stateMachine = StateMachineFactory.build(configurer);
    //开始运行，可传工作流需要参数
    stateMachine.start(params);

```

