package ambitor.easy.statemachine.messaging.coder;

import ambitor.easy.statemachine.messaging.vo.Header;
import ambitor.easy.statemachine.messaging.vo.NettyMessage;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description： 私有协议解码
 * Created by Ambitor on 2017/4/26.
 */
public class NettyMessageDecoder extends LengthFieldBasedFrameDecoder {

    private MarshallingDecoder marshallingDecoder;

    public NettyMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength) throws Exception {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
        marshallingDecoder = new MarshallingDecoder();
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        ByteBuf frame = (ByteBuf) super.decode(ctx, in);
        //这种情况一般是半包的时候
        if (frame == null) return null;

        NettyMessage message = new NettyMessage();
        Header header = new Header();
        header.setCrcCode(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());
        header.setPriority(frame.readByte());

        int attachmentSize = frame.readInt();
        Map<String, Object> attachment = new HashMap<>();
        for (int i = 0; i < attachmentSize; i++) {
            int keySize = frame.readInt();
            byte[] keyByte = new byte[keySize];
            frame.readBytes(keyByte);
            String key = new String(keyByte, Charset.forName("UTF-8"));
            Object value = marshallingDecoder.decode(frame);
            attachment.put(key, value);
        }
        header.setAttachment(attachment);
        message.setHeader(header);
        Object body = marshallingDecoder.decode(frame);
        message.setBody(body);
        return message;
    }
}
