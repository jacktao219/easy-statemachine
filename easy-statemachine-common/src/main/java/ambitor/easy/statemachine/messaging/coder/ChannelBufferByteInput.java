package ambitor.easy.statemachine.messaging.coder;


import io.netty.buffer.ByteBuf;
import org.jboss.marshalling.ByteInput;

import java.io.IOException;

/**
 * @Description： netty源码，因为引入不了所以直接复制出来
 * Created by Ambitor on 2017/4/26.
 */
class ChannelBufferByteInput implements ByteInput {
    private final ByteBuf buffer;

    ChannelBufferByteInput(ByteBuf buffer) {
        this.buffer = buffer;
    }

    @Override
    public void close() {
    }

    @Override
    public int available() {
        return this.buffer.readableBytes();
    }

    @Override
    public int read() {
        return this.buffer.isReadable() ? this.buffer.readByte() & 255 : -1;
    }

    @Override
    public int read(byte[] array) throws IOException {
        return this.read(array, 0, array.length);
    }

    @Override
    public int read(byte[] dst, int dstIndex, int length) throws IOException {
        int available = this.available();
        if (available == 0) {
            return -1;
        } else {
            length = Math.min(available, length);
            this.buffer.readBytes(dst, dstIndex, length);
            return length;
        }
    }

    @Override
    public long skip(long bytes) {
        int readable = this.buffer.readableBytes();
        if ((long) readable < bytes) {
            bytes = (long) readable;
        }

        this.buffer.readerIndex((int) ((long) this.buffer.readerIndex() + bytes));
        return bytes;
    }
}
