# 轻量级状态机工作流框架

### Demo场景介绍

信贷业务主要流程为`用户注册->风控系统授信->风控出额度->用户申请放款->用户还清贷款`，其中放款功能的简易流程如下`开立二类户->建档授信->签署合同->放款->出金`，其中出金还包括复杂子流程，流程图如下：

![](https://oscimg.oschina.net/oscnet/dd5c192581ab6610ea1a64521b28636ff3f.jpg)

                                                   （放款流程图）

![](https://oscimg.oschina.net/oscnet/f30365910938757d659f6a6a1f7dbc0657b.jpg)

                                                    （出金流程图）

此类信贷业务是一个典型的工作流业务，并且针对不同渠道/流量方的接入放款的流程不同，需要重新定义流程，如下：

-   A渠道：`开立二类户->建档授信->等待建档授信回调->签署合同->放款->出金`
-   B渠道：`创建客户号->建档授信->等待建档授信回调->放款->出金`
-   C渠道：`开立二类户->建档授信->等待建档授信回调->放款->出金`
-   others...

### 问题

Q1.流程节点间代码完全耦合，无法适应易变的流程

A1.流程中开二类户节点的下一个节点完全在代码中用硬代码`if else`写死，如果新渠道放款流程为`创建客户号->放款->出金`，改造成本太高，后期维护工作量大，无法适应节点顺序随意变化，伪代码：

```
class 二类户处理类 {
    if( 开二类户成功 ) {
        创建建档授信task任务
    } else{
        创建放款失败回调task任务
    }
}

```

Q2.放款时间长，生成Task任务多

A2.由于XXL-JOB调度器是X秒调度一次，一次执行Y条，因为节点耦合每次调度一次只能执行一个节点的Task，执行完后生成的下一个节点Task只能下一次调度的时候才能触发，目前一个放款有15条Task，大致需要5~10分钟，随着量越来越大，task越来越多，放款时间将越来越久。

Q3.放款流程当前在哪个节点状态不明确

A3.由于分成15条Task去做，资产表状态只有父级状态`放款中、还款中、结清...`，不能很好区分当前资产处在放款中的子状态，比如`开二类户中、建档授信...`，不仅不利于问题排查，也不利于实时统计的细化(看需求)

Q4.流程运行时节点之间的Session数据共享问题

A4.流程节点之间往往可能需要数据共享，比如放款人信息在开二类户已经查出来，下一个建档授信节点应该不用再去查询，原Task实现方式只能把信息保存在下一个建档授信Task的request_data字段中，然后再查出来，但很多数据共享的场景本身更适合在内存中共享而不是数据库

Q5.新人学习成本很大

A5.以前分成15条Task实现的方式，没有地方写明每条Task的依赖关系，先后顺序等，导致新人来了之后除了看viso流程图，在代码中必须切入每个Task源码才能熟悉流程，刚开始就切入整个细节实现，学习成本高也非常繁琐，而工作流框架在状态机实现类中清晰定义了每个流程节点以及转换关系，通过后续简单的扩展可以支持XML、HTML配置工作流

6.Others...

### 工作流框架实现

上面提到了诸如`代码解耦、高效率开发/维护、节省开发成本`等一系列好处，下面谈谈工作流的实现，整个工作流是基于状态机实现的，介绍工作流之前先介绍下状态机的概念

#### yml配置方式
```
    #状态机名称
    name: sf
    
    #状态配置
    states:
      init: WAIT_CREATE_CARDII          #等待开二类户
      suspend:
        - WAIT_DOCUMENT_CREDIT_CALLBACK #等待建档授信回调
        - WAIT_GRANT_CHECK              #放款检查
      end:
        - CREATE_CARDII_FAILED          #开二类户失败
        - DOCUMENT_CREDIT_FAILED        #建档授信失败
        - GRANT_FAILED                  #放款失败
        - GRANT_SUCCESS                 #结束流程
      other:
        - WAIT_DOCUMENT_CREDIT          #建档授信
        - WAIT_GRANT                    #放款
        - WAIT_GRANT_CHECK              #等待放款校验
        - GRANT_TASK_SAVE               #主流程完成
    
    #事件配置
    events:
        - CREATE_CARDII                 #开二类户
        - DOCUMENT_CREDIT               #建档授信
        - DOCUMENT_CREDIT_CALLBACK      #建档授信回调
        - GRANTED                       #放款
        - GRANT_CHECKED                 #放款校验
        - FINISHED                      #结束
    
    #转换器配置
    transitions:
        - type: standard                        #类型： 标准转换器
          source: WAIT_CREATE_CARDII            #源状态：等待创建二类户
          target: WAIT_DOCUMENT_CREDIT          #目标状态：等待建档授信
          event:  CREATE_CARDII                 #事件：  创建二类户
          action: SFCreateCardIIAction.class    #转换操作：创建二类户业务实现类
          errorAction:
    
        - type: choice                          #类型：选择转换器
          source: WAIT_DOCUMENT_CREDIT          #源状态：等待建档授信
          event:  DOCUMENT_CREDIT               #事件：建档授信
          action: SFDocumentCreditAction.class  #转换操作：建档授信业务实现类
          errorAction:
          if: {status: DOCUMENT_CREDIT_STATUS,equals: DOCUMENT_CREDIT_SUCCESS,target: WAIT_GRANT}
          elseif: {status: DOCUMENT_CREDIT_STATUS,equals: WAIT_DOCUMENT_CREDIT_CALLBACK,target: WAIT_DOCUMENT_CREDIT_CALLBACK}
          else: {target: DOCUMENT_CREDIT_FAILED}
    
        - type: choice
          source: WAIT_DOCUMENT_CREDIT_CALLBACK  #源状态：等待建档授信回调
          event:  DOCUMENT_CREDIT_CALLBACK       #事件： 建档授信回调
          if: {key: DOCUMENT_CREDIT_STATUS,equals: DOCUMENT_CREDIT_SUCCESS,target: WAIT_GRANT}
          else: {target: DOCUMENT_CREDIT_FAILED}
    
        - type: choice
          source: WAIT_GRANT                    #源状态：等待放款
          event:  GRANTED                       #事件：放款
          action: SFGrantAction.class           #转换操作：放款业务实现类
          if: {status: GRANT_STATUS,equals: GRANT_SUCCESS,target: GRANT_TASK_SAVE}
          else: {target: WAIT_GRANT_CHECK}
    
        - type: choice
          source: WAIT_GRANT_CHECK              #源状态：等待放款
          event:  GRANT_CHECKED                 #事件：放款
          action: SFGrantAction.class           #转换操作：放款业务实现类
          if: {status: GRANT_STATUS,equals: GRANT_SUCCESS,target: GRANT_TASK_SAVE}
          else: {target: GRANT_FAILED}
    
        - type: standard
          source: GRANT_TASK_SAVE               #源状态：放款任务保存
          target: GRANT_SUCCESS                 #目标状态：放款成功
          event:  FINISHED                      #事件：  放款结束
          action: SFFinishAction.class          #转换操作：放款结束保存任务业务实现类

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
