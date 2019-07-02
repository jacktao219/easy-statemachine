package ambitor.easy.statemachine.messaging.vo;

/**
 * @Description： ${todo}(描述完成的功能)
 * Created by Ambitor on 2017/4/26.
 */
public class MessageType {

    public final static byte SERVICE_REQ = 0;
    public final static byte SERVICE_RES = 1;
    public final static byte SERVICE_ONE_WAY = 2;
    public final static byte ACCEPT_REQ = 3;
    public final static byte ACCEPT_RES = 4;
    public final static byte HEARTBEAT_REQ = 5;
    public final static byte HEARTBEAT_RES = 6;

}
