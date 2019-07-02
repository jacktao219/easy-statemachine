package ambitor.easy.statemachine.messaging.vo;


/**
 * @Description： ${todo}(描述完成的功能)
 * Created by Ambitor on 2017/4/26.
 */
public class NettyMessage {
    private Header header;
    private Object body;

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public Header getHeader() {
        return header;
    }

    public void setHeader(Header header) {
        this.header = header;
    }

    @Override
    public String toString() {
        return header.toString() + " body:" + body.toString();
    }
}
