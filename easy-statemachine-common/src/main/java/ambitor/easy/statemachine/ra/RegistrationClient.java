package ambitor.easy.statemachine.ra;

import ambitor.easy.statemachine.ra.vo.StateMachineClient;

import java.util.List;

/**
 * 注册客户端
 * Created by Ambitor on 2019/6/26
 */
public interface RegistrationClient {
    /**
     * 向注册中心注册
     * @param client 客户端
     * @return 仅当注册失败了返回false 否则true
     */
    boolean register(StateMachineClient client);

    /**
     * 获取活跃的客户端
     * @return 没有的话返回null
     */
    List<StateMachineClient> getAliveClient();
}
