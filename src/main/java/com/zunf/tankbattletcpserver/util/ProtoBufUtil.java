package com.zunf.tankbattletcpserver.util;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;
import com.zunf.tankbattletcpserver.enums.ErrorCode;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtoBufUtil {
    
    public static <T extends MessageLite> T parseRespBody(CommonProto.BaseResponse baseResponse, Class<T> clazz) {
        if (baseResponse.getCode() != 0) {
            log.warn("Failed to request: {}", baseResponse.getMessage());
            return null;
        }
        try {
            return (T) clazz.getMethod("parseFrom", ByteString.class).invoke(null, baseResponse.getPayloadBytes());
        } catch (Exception e) {
            log.error("Failed to parse protobuf message {}", clazz.getSimpleName(), e);
            return null;
        }
    }

    public static byte[] successResp(ByteString body) {
        return CommonProto.BaseResponse.newBuilder().setCode(ErrorCode.OK.getCode()).setMessage(ErrorCode.OK.getMsg()).setPayloadBytes(body).build().toByteArray();
    }

    public static byte[] failResp(ErrorCode errorCode) {
        return CommonProto.BaseResponse.newBuilder().setCode(errorCode.getCode()).setMessage(errorCode.getMsg()).build().toByteArray();
    }
}
