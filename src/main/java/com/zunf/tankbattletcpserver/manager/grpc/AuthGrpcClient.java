package com.zunf.tankbattletcpserver.manager.grpc;

import com.google.protobuf.InvalidProtocolBufferException;
import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.server.auth.AuthProto;
import com.zunf.tankbattletcpserver.grpc.server.auth.AuthServiceGrpc;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AuthGrpcClient {

    @GrpcClient("tank-battle-backend")
    private AuthServiceGrpc.AuthServiceBlockingStub authService;

    /**
     * 调用 Token 校验接口
     */
    public Long checkToken(String token) {
        AuthProto.CheckTokenRequest request = AuthProto.CheckTokenRequest.newBuilder()
                .setToken(token)
                .build();
        CommonProto.BaseResponse baseResponse = authService.checkToken(request);
        try {
            return AuthProto.CheckTokenResponse.parseFrom(baseResponse.getPayloadBytes()).getPlayerId();
        } catch (InvalidProtocolBufferException e) {
            log.error("解析 gRPC 返回数据失败",  e);
            return null;
        } catch (io.grpc.StatusRuntimeException e) {
            // 补充捕获 gRPC 异常，定位具体原因
            log.error("gRPC 调用失败：{}，错误码：{}", e.getMessage(), e.getStatus().getCode());
            return null;
        }
    }
}