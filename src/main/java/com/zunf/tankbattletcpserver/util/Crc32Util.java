package com.zunf.tankbattletcpserver.util;

import io.netty.buffer.ByteBuf;

import java.util.zip.CRC32;

/**
 * CRC32 工具类
 *
 * @author zunf
 */
public class Crc32Util {

    /**
     * 对 ByteBuf 的两段区间连续计算 CRC32（同一个 CRC32 状态）
     * 用于“中间跳过某些字段”的场景
     */
    public static int crc32TwoParts(ByteBuf buf,
                                    int headerStart, int headerPartLen,
                                    int bodyStart, int bodyPartLen) {
        CRC32 crc32 = new CRC32();
        for (int i = 0; i < headerPartLen; i++) {
            crc32.update(buf.getByte(headerStart + i));
        }
        for (int i = 0; i < bodyPartLen; i++) {
            crc32.update(buf.getByte(bodyStart + i));
        }
        return (int) crc32.getValue();
    }
}
