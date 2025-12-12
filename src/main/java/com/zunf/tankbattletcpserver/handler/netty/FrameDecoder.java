package com.zunf.tankbattletcpserver.handler.netty;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

import static com.zunf.tankbattletcpserver.constant.ProtocolConstant.*;

/**
 * 自定义协议帧解码器（解决粘包拆包）
 *
 * @author zunf
 */
public class FrameDecoder extends LengthFieldBasedFrameDecoder {


    public FrameDecoder() {
        /*
         * 父类构造器参数说明：
         * 1. maxFrameLength：最大帧长度（超过则抛异常）
         * 2. lengthFieldOffset：长度字段的偏移量（协议体长度字段在协议头的第2字节开始）
         * 3. lengthFieldLength：长度字段的字节数（4字节int）
         * 4. lengthAdjustment：长度修正值。
         *    Netty 计算整帧长度公式： frameLength = lengthFieldValue + lengthFieldOffset + lengthFieldLength + lengthAdjustment
         *    本协议 length 字段表示 bodyLen（仅协议体长度），
         *    而 length 字段后面还有 crc32(4B) + body(bodyLen)，
         *    所以 lengthAdjustment = 4（CRC32字段长度）。
         * 5. initialBytesToStrip：解码后跳过的字节数（0=保留协议头，后续解析用）
         * 6. failFast：true=长度超过maxFrameLength时立即抛异常，false=读完所有数据再抛
         */
        super(MAX_FRAME_LENGTH, BODY_LENGTH_FIELD_OFFSET, BODY_LENGTH_FIELD_LENGTH, CRC32_FIELD_LENGTH, INITIAL_BYTES_TO_STRIP, true);
    }
}
