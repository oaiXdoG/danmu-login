package com.oaixdog.loginserver.controller;

import com.oaixdog.loginserver.exception.BusinessException;
import com.oaixdog.loginserver.protocol.ApiResponse;
import com.oaixdog.loginserver.protocol.LoginRequest;
import com.oaixdog.loginserver.protocol.LoginResponse;
import com.oaixdog.loginserver.service.LoginService;
import com.zfoo.net.NetContext;
import com.zfoo.net.anno.PacketReceiver;
import com.zfoo.net.router.attachment.HttpAttachment;
import com.zfoo.net.session.Session;
import com.zfoo.net.util.SessionUtils;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LoginController {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final LoginService loginService;

    @PacketReceiver
    public void atLoginRequest(Session session, LoginRequest request, HttpAttachment attachment) {
        try {
            if (request == null) {
                throw new BusinessException("登录请求不能为空");
            }
            String loginIp = SessionUtils.toIp(session);
            String platform = request.getPlatform();
            String account = request.getAccount();
            String token = request.getToken();
            String clientVersion = request.getClientVersion();
            String resourceVersion = request.getResourceVersion();
            LoginService.LoginResult result = loginService.login(platform, account, token, clientVersion, resourceVersion, loginIp);
            LoginResponse data = LoginResponse.valueOf(result.loginTicket(), result.host(), result.port());
            attachment.setHttpResponseStatus(HttpResponseStatus.OK);
            NetContext.getRouter().send(session, ApiResponse.success(data), attachment);
        } catch (BusinessException exception) {
            log.warn("登录业务失败: {}", exception.getMessage());
            attachment.setHttpResponseStatus(HttpResponseStatus.OK);
            NetContext.getRouter().send(session, ApiResponse.businessError(exception.getCode(), exception.getMessage()), attachment);
        } catch (Exception e) {
            log.error("登录接口执行失败", e);
            attachment.setHttpResponseStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            NetContext.getRouter().send(session, ApiResponse.systemError(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "服务器内部错误"), attachment);
        }
    }
}
