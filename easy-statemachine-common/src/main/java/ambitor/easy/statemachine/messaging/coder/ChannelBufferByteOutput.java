package ambitor.easy.statemachine.messaging.coder;

import io.netty.buffer.ByteBuf;
import org.jboss.marshalling.ByteOutput;

import java.io.IOException;

/**
 * netty源码，因为引入不了所以直接复制出来
 * {@link ByteOutput} implementation which writes the data to a {@link ByteBuf}
 */
class ChannelBufferByteOutput implements ByteOutput {

    private final ByteBuf buffer;

    /**
     * Create a new instance which use the given {@link ByteBuf}
     */
    ChannelBufferByteOutput(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public void close() {
        // Nothing to do
    }

    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public void write(int b) {
        buffer.writeByte(b);
    }

    @Override
    public void write(byte[] bytes) {
        buffer.writeBytes(bytes);
    }

    @Override
    public void write(byte[] bytes, int srcIndex, int length) {
        buffer.writeBytes(bytes, srcIndex, length);
    }

    /**
     * Return the {@link ByteBuf} which contains the written content
     *
     */
    ByteBuf getBuffer() {
        return buffer;
    }
}
