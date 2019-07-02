package ambitor.easy.statemachine.messaging;

import ambitor.easy.statemachine.messaging.coder.NettyMessageDecoder;
import ambitor.easy.statemachine.messaging.coder.NettyMessageEncoder;
import ambitor.easy.statemachine.messaging.handler.HeartbeatHandler;
import ambitor.easy.statemachine.messaging.handler.LoginResponseHandler;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

/**
 * 服务端
 * @Description： Created by Ambitor on 2017/4/26.
 */
public class ProtocolServer {

    public void bind(String host, int port) {

        EventLoopGroup acceptGroup = new NioEventLoopGroup();
        EventLoopGroup serverGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(acceptGroup, serverGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("NettyMessageDecoder", new NettyMessageDecoder(1024 * 1024 * 1, 4, 4));
                            pipeline.addLast("NettyMessageEncoder", new NettyMessageEncoder());
                            pipeline.addLast("LoginResponseHandler", new LoginResponseHandler());
                            pipeline.addLast("HeartbeatHandler", new HeartbeatHandler());
                        }
                    });
            ChannelFuture future = bootstrap.bind(host, port).sync();
            System.out.println("服务器启动成功.");
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            acceptGroup.shutdownGracefully();
            serverGroup.shutdownGracefully();
            System.out.println("服务器中止服务.");
        } finally {

        }
    }

    public static void main(String[] args) {
        new ProtocolServer().bind("127.0.0.1", 9080);
    }
}
