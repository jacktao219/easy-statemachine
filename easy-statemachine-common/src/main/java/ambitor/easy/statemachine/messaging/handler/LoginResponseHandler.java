package ambitor.easy.statemachine.messaging.handler;

import ambitor.easy.statemachine.messaging.vo.Header;
import ambitor.easy.statemachine.messaging.vo.MessageType;
import ambitor.easy.statemachine.messaging.vo.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description： 服务器登录返回handler
 * Created by Ambitor on 2017/4/27.
 */
public class LoginResponseHandler extends ChannelHandlerAdapter {

    private static ConcurrentHashMap<String, String> onlineNode = new ConcurrentHashMap<>();
    private static final String value = "online";
    private static Set<String> whiteHost = new HashSet<String>() {
        {
            //加载白名单
            add("127.0.0.1");
        }
    };

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        NettyMessage request = (NettyMessage) msg;
        if (request != null && request.getHeader().getType() == MessageType.ACCEPT_REQ) {
            if (request.getBody() == null || !request.getBody().equals("Hello Netty Private Protocol.")) {
                ctx.close();
            } else {
                System.out.println("登录成功,进行白名单、是否重复连接校验.");
                String address = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
                if (!whiteHost.contains(address)) {
                    System.out.println("该IP禁止访问服务器，断开链接.");
                    ctx.close();
                }
                if (onlineNode.containsKey(address)) {
                    System.out.println("该IP重复登录服务器，断开链接.");
                    ctx.close();
                }
                onlineNode.put(address, value);
                NettyMessage response = new NettyMessage();
                Header header = new Header();
                header.setType(MessageType.ACCEPT_RES);
                header.setSessionID(1);
                response.setHeader(header);
                response.setBody("登录成功，返回信息.");
                ctx.writeAndFlush(response);
                //TODO 待验证 这个地方不需要再调用ctx.fireChannelRead往下走的原因是此消息已经被判断为连接请求，并且已经处理返回了，所以不需要往下走了
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        onlineNode.remove(((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress());
        System.out.println("关闭链接，释放资源，删除已登录信息防止客户端登录不了.");
        ctx.close();
        ctx.fireExceptionCaught(cause);
    }
}