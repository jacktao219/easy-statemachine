package ambitor.easy.statemachine.messaging.coder;

import ambitor.easy.statemachine.messaging.vo.NettyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * @Description： 私有协议编码
 * Created by Ambitor on 2017/4/26.
 */
public class NettyMessageEncoder extends MessageToMessageEncoder<NettyMessage> {

    private MarshallingEncoder marshallingEncoder;

    public NettyMessageEncoder() throws Exception {
        marshallingEncoder = new MarshallingEncoder();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, NettyMessage nettyMessage, List<Object> list) throws Exception {
        if (nettyMessage == null || nettyMessage.getHeader() == null) {
            throw new Exception("nettyMessage or nettyMessage.getHeader 为空");
        }
        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeInt(nettyMessage.getHeader().getCrcCode());
        byteBuf.writeInt(nettyMessage.getHeader().getLength());
        byteBuf.writeLong(nettyMessage.getHeader().getSessionID());
        byteBuf.writeByte(nettyMessage.getHeader().getType());
        byteBuf.writeByte(nettyMessage.getHeader().getPriority());
        byteBuf.writeInt(nettyMessage.getHeader().getAttachment().size());
        for (Map.Entry<String, Object> entry : nettyMessage.getHeader().getAttachment().entrySet()) {
            String key = entry.getKey();
            Object attachment = entry.getValue();
            byte[] keyByte = key.getBytes(Charset.forName("UTF-8"));
            byteBuf.writeInt(keyByte.length);
            byteBuf.writeBytes(keyByte);
            marshallingEncoder.encode(attachment, byteBuf);
        }
        if (nettyMessage.getBody() != null) {
            marshallingEncoder.encode(nettyMessage.getBody(), byteBuf);
        } else {
            //body长度为0
            byteBuf.writeInt(0);
            //往第5个字节中写入消息的总长度
        }
        byteBuf.setInt(4, byteBuf.readableBytes() - 8);
        ctx.writeAndFlush(byteBuf);
    }
}
