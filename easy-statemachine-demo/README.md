# 轻量级状态机工作流框架

### Demo场景介绍


#### yml配置方式
简单的配置一个yml配置文件，然后编写BuyFood.class、CookRice.class、CookFood.class、EatRice.class、WifeWash.class、 HusbandWash.class的业务逻辑实现类，为方便理解Demo状态和事件都以中文表示

```
#状态机名称
name: eat

#拦截器配置
interceptor:
    - PersistStateMachineInterceptor.class

#状态配置
states:
  init: 准备食材
  suspend:
#    - 不用
  end:
    - 休息中
  other:
    - 准备米饭
    - 准备炒菜
    - 准备吃饭
    - 老公准备洗碗            #-----谁洗碗由EatRice.classg根据逻辑返回
    - 老婆准备洗碗

#事件配置
events:
    - 买菜
    - 煮饭
    - 炒菜
    - 吃饭
    - 洗碗

#转换器配置
transitions:
    - type: standard
      source: 准备食材
      target: 准备米饭
      event:  买菜
      action: BuyFood.class
      errorAction:

    - type: standard
      source: 准备米饭
      target: 准备炒菜
      event:  煮饭
      action: CookRice.class
      errorAction:

    - type: standard
      source: 准备炒菜
      target: 准备吃饭
      event:  炒菜
      action: CookFood.class
      errorAction:

    - type: choice
      source: 准备吃饭
      event:  吃饭
      action: EatRice.class
      first: {status: Eat_Status,equals: Husband,target: 老公准备洗碗}
      last: {target: 老婆准备洗碗}

    - type: standard
      source: 老婆准备洗碗
      target: 休息中
      event:  洗碗
      action: WifeWash.class

    - type: standard
      source: 老公准备洗碗
      target: 休息中
      event:  洗碗
      action: HusbandWash.class

```
####代码调用
```
    public class DemoTest{
            
            @Autowired
            private ApplicationContext context
            
            public void runStateMachine(){
                //根据状态机name获取状态机配置Bean
                StateMachineConfigurer configurer = context.getBean("eat", StateMachineConfigurer.class);
                //构建状态机实例
                StateMachine<S, E> stateMachine = StateMachineFactory.build(configurer);
                //工作流上下文，整个流程中共享
                MessageHeaders headers = new MessageHeaders();
                headers.addHeader(TASK_HEADER, task);
                //设置工作流上下文，可为null
                stateMachine.start(headers);
            }
    }
```
