package com.zunf.tankbattletcpserver.handler;

import io.netty.channel.ChannelHandler;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import org.springframework.stereotype.Component;

import static com.zunf.tankbattletcpserver.constant.ProtocolConstant.*;

/**
 * 自定义协议帧解码器（解决粘包拆包）
 *
 * @author zunf
 */
@Component
@ChannelHandler.Sharable
public class FrameDecoder extends LengthFieldBasedFrameDecoder {


    public FrameDecoder() {
        /*
         * 父类构造器参数说明：
         * 1. maxFrameLength：最大帧长度（超过则抛异常）
         * 2. lengthFieldOffset：长度字段的偏移量（协议体长度字段在协议头的第2字节开始）
         * 3. lengthFieldLength：长度字段的字节数（4字节int）
         * 4. lengthAdjustment：长度调整值（完整消息长度=协议体长度+协议头长度）
         * 5. initialBytesToStrip：解码后跳过的字节数（0=保留协议头，后续解析用）
         * 6. failFast：true=长度超过maxFrameLength时立即抛异常，false=读完所有数据再抛
         */
        super(MAX_FRAME_LENGTH, BODY_LENGTH_FIELD_OFFSET, BODY_LENGTH_FIELD_LENGTH, LENGTH_ADJUSTMENT, INITIAL_BYTES_TO_STRIP, true);
    }
}
