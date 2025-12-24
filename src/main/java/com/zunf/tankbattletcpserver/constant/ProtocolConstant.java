package com.zunf.tankbattletcpserver.constant;

/**
 * 自定义协议常量
 * 协议格式：协议头(15字节) + 协议体(N字节)
 * 协议头：操作类型(2B) + 版本号(1B) + 请求ID(4B) + 协议体长度(4B) + 校验码(4B)
 *
 * @author zunf
 */
public interface ProtocolConstant {

    /**
     * 协议头中各个字段的 bit 长度
     */
    int OPERATION_TYPE_FIELD_LENGTH = 2;
    int VERSION_FIELD_LENGTH = 1;
    int REQUEST_ID_FIELD_LENGTH = 4;
    int BODY_LENGTH_FIELD_LENGTH = 4;
    int CRC32_FIELD_LENGTH = 4;

    /**
     * 协议头总长度
     */
    int HEADER_TOTAL_LENGTH = OPERATION_TYPE_FIELD_LENGTH + VERSION_FIELD_LENGTH + REQUEST_ID_FIELD_LENGTH + BODY_LENGTH_FIELD_LENGTH + CRC32_FIELD_LENGTH;

    /**
     * 各个字段协议头中的偏移量
     */
    int REQUEST_ID_FIELD_OFFSET = OPERATION_TYPE_FIELD_LENGTH + VERSION_FIELD_LENGTH;
    int BODY_LENGTH_FIELD_OFFSET = OPERATION_TYPE_FIELD_LENGTH + VERSION_FIELD_LENGTH + REQUEST_ID_FIELD_LENGTH;
    int CRC32_FIELD_OFFSET = OPERATION_TYPE_FIELD_LENGTH + VERSION_FIELD_LENGTH + REQUEST_ID_FIELD_LENGTH + BODY_LENGTH_FIELD_LENGTH;

    /**
     * 跳过的初始字节数：0（解码器从协议头开始解析，不跳过任何字节）
     */
    int INITIAL_BYTES_TO_STRIP = 0;
    /**
     * 最大帧长度：防止内存溢出（当前为 1024*1024=1MB）
     */
    int MAX_FRAME_LENGTH = 1024 * 1024;

    byte PROTOCOL_VERSION = 1;
}
