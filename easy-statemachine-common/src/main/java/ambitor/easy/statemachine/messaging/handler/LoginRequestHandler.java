package ambitor.easy.statemachine.messaging.handler;

import ambitor.easy.statemachine.messaging.vo.Header;
import ambitor.easy.statemachine.messaging.vo.MessageType;
import ambitor.easy.statemachine.messaging.vo.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description： 客户端握手、权限handler
 * Created by Ambitor on 2017/4/26.
 */
@Slf4j
public class LoginRequestHandler extends ChannelHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setType(MessageType.ACCEPT_REQ);
        message.setHeader(header);
        message.setBody("Hello Netty Private Protocol.");
        ctx.writeAndFlush(message);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        NettyMessage request = (NettyMessage) msg;
        if (request != null && request.getHeader().getType() == MessageType.ACCEPT_RES) {
            if (request.getHeader().getSessionID() != 1) {
                ctx.close();
            }
        }
        ctx.fireChannelRead(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("登陆请求异常", cause);
    }
}
