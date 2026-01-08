package com.zunf.tankbattletcpserver.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.Parser;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoBufUtil {
    
    public static <T extends MessageLite> T parseRespBody(CommonProto.BaseResponse baseResponse, Parser<T> parser) {
        if (baseResponse.getCode() != ErrorCode.OK.getCode()) {
            log.warn("Failed to request: {}", baseResponse.getMessage());
            return null;
        }
        try {
            return parser.parseFrom(baseResponse.getPayloadBytes());
        } catch (Exception e) {
            log.error("Failed to parse protobuf message {}", parser.getClass().getSimpleName(), e);
            return null;
        }
    }

    public static <T extends MessageLite> T parseBytes(byte[] bytes, Parser<T> parser) {
        if (bytes == null) {
            return null;
        }
        try {
            return parser.parseFrom(bytes);
        } catch (Exception e) {
            log.error("Failed to parse protobuf message {}", parser.getClass().getSimpleName(), e);
            return null;
        }
    }

    public static byte[] successResp(ByteString body) {
        return CommonProto.BaseResponse.newBuilder().setCode(ErrorCode.OK.getCode()).setMessage(ErrorCode.OK.getMsg()).setPayloadBytes(body).build().toByteArray();
    }

    public static byte[] failResp(ErrorCode errorCode) {
        return CommonProto.BaseResponse.newBuilder().setCode(errorCode.getCode()).setMessage(errorCode.getMsg()).build().toByteArray();
    }

    // 字段映射器：定义“ServerResp → ClientBuilder”的映射规则（函数式接口）
    @FunctionalInterface
    public interface FieldMapper<T extends Message, B extends Message.Builder> {
        void map(T serverResp, B clientBuilder);
    }

    /**
     * 通用方法：解析 Server 响应 → 转换为 Client 响应 → 构建最终 byte[] 响应
     * @param baseResp        gRPC 服务端基础响应
     * @param serverParser    Server 侧 Proto 解析器
     * @param clientBuilder   Client 侧 Proto Builder（用于字段映射）
     * @param fieldMapper     业务字段映射器（ServerResp → ClientBuilder）
     * @return 最终返回给 TCP 客户端的 byte[] 响应
     */
    public static <T extends Message, B extends Message.Builder> byte[] convertAndBuildResp(
            CommonProto.BaseResponse baseResp,
            Parser<T> serverParser,
            B clientBuilder,
            FieldMapper<T, B> fieldMapper
    ) {
        // 1. 空值兜底：baseResp 为空 → 返回失败响应
        if (baseResp == null) {
            return failResp(ErrorCode.UNKNOWN_ERROR);
        }
        // 没有响应体的情况
        if (serverParser ==  null || clientBuilder == null) {
            // 直接返回
            return baseResp.toByteArray();
        }

        // 2. 解析 Server 侧业务响应（捕获解析异常）
        T serverResp;
        try {
            serverResp = parseRespBody(baseResp, serverParser);
        } catch (Exception e) {
            log.error("解析 Proto 响应失败", e);
            return failResp(ErrorCode.PROTO_PARSE_ERROR);
        }

        // 3. Server 响应为空 → 返回失败响应
        if (serverResp == null) {
            return failResp(ErrorCode.PROTO_PARSE_ERROR);
        }

        // 4. 映射业务字段（由具体业务指定）
        fieldMapper.map(serverResp, clientBuilder);

        // 5. 构建 Client 响应并转换为最终 byte[]
        Message clientResp = clientBuilder.build();
        return successResp(clientResp.toByteString());
    }
}
