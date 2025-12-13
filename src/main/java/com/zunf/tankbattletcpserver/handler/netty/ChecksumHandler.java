package com.zunf.tankbattletcpserver.handler.netty;

import com.zunf.tankbattletcpserver.util.Crc32Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.zunf.tankbattletcpserver.constant.ProtocolConstant.*;


/**
 * CRC32 校验
 *
 * @author zunf
 */
@Slf4j
@Component
@ChannelHandler.Sharable
public class ChecksumHandler extends ChannelDuplexHandler {

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            // 非 ByteBuf 的消息直接透传
            super.channelRead(ctx, msg);
            return;
        }

        ByteBuf frame = (ByteBuf) msg;
        try {
            if (frame.readableBytes() < HEADER_TOTAL_LENGTH) {
                // 理论上不该发生（FrameDecoder 已保证完整帧），防御性处理
                log.warn("Frame is shorter then the total length of the header, ignore.");
                ReferenceCountUtil.release(frame);
                return;
            }

            // 记录起始位置（一般是 0，但不要假设）
            int startIndex = frame.readerIndex();

            // 读取头部字段（注意：这里用 readXXX 会移动 readerIndex）
            byte type = frame.readByte();
            byte version = frame.readByte();
            int requestId = frame.readInt();
            int bodyLength = frame.readInt();
            int crc32FromHeader = frame.readInt();

            // 检查 body 是否完整
            if (frame.readableBytes() < bodyLength) {
                // 数据不完整，协议错误，直接丢弃
                log.warn("Frame body is too short, ignore.");
                ReferenceCountUtil.release(frame);
                return;
            }

            // 计算 CRC32：范围 = type + version + length + body
            int computedCrc32 = computeCrc32(frame, startIndex, bodyLength);

            if (computedCrc32 != crc32FromHeader) {
                log.warn("CRC32 check failed, remote=" + ctx.channel().remoteAddress() + ", expected=" + crc32FromHeader + ", actual=" + computedCrc32);
                ReferenceCountUtil.release(frame);
                return;
            }

            // 校验通过：重置 readerIndex，让下游从头按自己的方式解析
            frame.readerIndex(startIndex);
            super.channelRead(ctx, frame.retain());
        } finally {
            ReferenceCountUtil.release(frame);
        }
    }

    private static int computeCrc32(ByteBuf frame, int startIndex, int bodyLength) {
        int headerPartLen = startIndex + CRC32_FIELD_OFFSET;
        int bodyStart = startIndex + HEADER_TOTAL_LENGTH;

        return Crc32Util.crc32TwoParts(frame, startIndex, headerPartLen, bodyStart, bodyLength);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (!(msg instanceof ByteBuf buf)) {
            super.write(ctx, msg, promise);
            log.warn("Frame is not ByteBuf, ignore.");
            return;
        }

        try {
            if (buf.readableBytes() < HEADER_TOTAL_LENGTH) {
                // 协议错误：至少要有头部
                log.warn("Frame is shorter then the total length of the header, ignore.");
                ReferenceCountUtil.release(buf);
                return;
            }

            int startIndex = buf.readerIndex();

            // 读取头部字段（用 getXXX，不移动 readerIndex）
            int bodyLength = buf.getInt(startIndex + BODY_LENGTH_FIELD_OFFSET);

            // 检查 body 是否完整（可选，通常 GameMessageEncoder 已保证）
            int totalLength = HEADER_TOTAL_LENGTH + bodyLength;
            if (buf.readableBytes() < totalLength) {
                // 数据不完整，说明上游编码有问题
                log.warn("Frame body is too short, ignore.");
                ReferenceCountUtil.release(buf);
                return;
            }

            // 计算 CRC32：范围 = type + version + length + body
            int crc32 = computeCrc32(buf, startIndex, bodyLength);

            // 把 CRC32 写回头部的校验码字段位置（大端序）
            int crcFieldIndex = startIndex + CRC32_FIELD_OFFSET;
            buf.setInt(crcFieldIndex, crc32);

            // 写出
            super.write(ctx, buf.retain(), promise);
        } finally {
            ReferenceCountUtil.release(buf);
        }
    }
}
