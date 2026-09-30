package com.oaixdog.loginserver.controller;

import com.oaixdog.loginserver.exception.BusinessException;
import com.oaixdog.loginserver.protocol.ApiResponse;
import com.oaixdog.loginserver.protocol.VersionRequest;
import com.oaixdog.loginserver.protocol.VersionResponse;
import com.oaixdog.loginserver.service.VersionService;
import com.zfoo.net.NetContext;
import com.zfoo.net.anno.PacketReceiver;
import com.zfoo.net.router.attachment.HttpAttachment;
import com.zfoo.net.session.Session;
import io.netty.handler.codec.http.HttpResponseStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VersionController {

    private static final Logger log = LoggerFactory.getLogger(VersionController.class);

    private final VersionService versionService;

    @PacketReceiver
    public void atVersionRequest(Session session, VersionRequest request, HttpAttachment attachment) {
        try {
            if (request == null) {
                throw new BusinessException("版本请求不能为空");
            }
            VersionService.VersionCheckResult result = versionService.checkVersion(request.getClientVersion(), request.getResourceVersion());
            VersionResponse data = VersionResponse.valueOf(result.serverClientVersion(), result.serverResourceVersion(), result.needClientUpdate(), result.needResourceUpdate());
            attachment.setHttpResponseStatus(HttpResponseStatus.OK);
            NetContext.getRouter().send(session, ApiResponse.success(data), attachment);
        } catch (BusinessException exception) {
            log.warn("版本业务失败: {}", exception.getMessage());
            attachment.setHttpResponseStatus(HttpResponseStatus.OK);
            NetContext.getRouter().send(session, ApiResponse.businessError(exception.getCode(), exception.getMessage()), attachment);
        } catch (Exception exception) {
            log.error("版本接口执行失败", exception);
            attachment.setHttpResponseStatus(HttpResponseStatus.INTERNAL_SERVER_ERROR);
            NetContext.getRouter().send(session, ApiResponse.systemError(HttpResponseStatus.INTERNAL_SERVER_ERROR.code(), "服务器内部错误"), attachment);
        }
    }
}
