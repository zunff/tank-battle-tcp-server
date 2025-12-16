package com.zunf.tankbattletcpserver.manager.grpc;

import com.zunf.tankbattletcpserver.grpc.CommonProto;
import com.zunf.tankbattletcpserver.grpc.game.auth.AuthClientProto;
import com.zunf.tankbattletcpserver.grpc.server.auth.AuthProto;
import com.zunf.tankbattletcpserver.grpc.server.auth.AuthServiceGrpc;
import com.zunf.tankbattletcpserver.util.ProtoBufUtil;
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
    public AuthClientProto.LoginResponse checkToken(String token) {
        AuthProto.CheckTokenRequest request = AuthProto.CheckTokenRequest.newBuilder().setToken(token).build();
        CommonProto.BaseResponse baseResponse = authService.checkToken(request);
        AuthProto.CheckTokenResponse checkTokenResponse = ProtoBufUtil.parseRespBody(baseResponse, AuthProto.CheckTokenResponse.parser());
        if (checkTokenResponse == null || checkTokenResponse.getPlayerId() <= 0) {
            return null;
        }
        return AuthClientProto.LoginResponse.newBuilder().
                setPlayerId(checkTokenResponse.getPlayerId()).setPlayerAccount(checkTokenResponse.getPlayerAccount()).setPlayerName(checkTokenResponse.getPlayerName()).build();
    }
}