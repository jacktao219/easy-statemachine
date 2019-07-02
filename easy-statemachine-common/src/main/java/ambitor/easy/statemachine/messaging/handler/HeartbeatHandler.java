package ambitor.easy.statemachine.messaging.handler;

import ambitor.easy.statemachine.messaging.vo.Header;
import ambitor.easy.statemachine.messaging.vo.MessageType;
import ambitor.easy.statemachine.messaging.vo.NettyMessage;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.ScheduledFuture;

import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @Description： 心跳维持
 * Created by Ambitor on 2017/4/26.
 */
public class HeartbeatHandler extends ChannelHandlerAdapter {

    private ScheduledFuture<?> heartbeatScheduled;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        NettyMessage request = (NettyMessage) msg;
        if (request.getHeader() == null) {
            ctx.fireChannelRead(msg);
        }
        if (request.getHeader().getType() == MessageType.ACCEPT_RES) {
            heartbeatScheduled = ctx.executor().scheduleAtFixedRate(new HeartbeatTask(ctx), 0, 30, TimeUnit.SECONDS);

        } else if (request.getHeader().getType() == MessageType.HEARTBEAT_REQ) {
            NettyMessage response = new NettyMessage();
            response.setHeader(new Header().setType(MessageType.HEARTBEAT_RES));
            response.setBody("心跳回应信息.");
            ctx.writeAndFlush(response);
            System.out.println("收到客户端心跳包,即将返回pong消息..." + request.getBody() + new Date());
        } else if (request.getHeader().getType() == MessageType.HEARTBEAT_RES) {
            System.out.println("心跳正常..." + new Date());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        ctx.fireExceptionCaught(cause);
    }

    class HeartbeatTask implements Runnable {

        private ChannelHandlerContext ctx;

        public HeartbeatTask(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            NettyMessage heartbeatMsg = new NettyMessage();
            heartbeatMsg.setHeader(new Header().setType(MessageType.HEARTBEAT_REQ));
            heartbeatMsg.setBody("客户端心跳包...");
            ctx.writeAndFlush(heartbeatMsg);
            System.out.println("发送心跳包" + new Date());
        }
    }
}

