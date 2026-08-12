package com.oaixdog.loginserver;

import com.oaixdog.loginserver.config.ApplicationProperties;
import com.oaixdog.loginserver.protocol.LoginRequest;
import com.oaixdog.loginserver.protocol.VersionRequest;
import com.zfoo.net.core.HostAndPort;
import com.zfoo.net.core.http.HttpServer;
import com.zfoo.net.packet.DecodedPacketInfo;
import com.zfoo.net.packet.common.Message;
import com.zfoo.net.router.attachment.HttpAttachment;
import com.zfoo.protocol.exception.RunException;
import com.zfoo.protocol.util.JsonUtils;
import com.zfoo.protocol.util.ThreadUtils;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.QueryStringDecoder;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public class Application {
    public static void main(String[] args) {
        var context = new ClassPathXmlApplicationContext("config.xml");

        var properties = context.getBean(ApplicationProperties.class);
        context.registerShutdownHook();

        Function<FullHttpRequest, DecodedPacketInfo> requestResolver = rawRequest -> {
            var attachment = HttpAttachment.valueOf(rawRequest, HttpResponseStatus.OK);
            var uri = new QueryStringDecoder(rawRequest.uri());
            var route = rawRequest.method().name() + " " + uri.path();

            Object request = switch (route) {
                case "POST /api/login" -> parseJsonBody(rawRequest, LoginRequest.class);
                case "GET /api/version" -> parseJsonBody(rawRequest, VersionRequest.class);
                default -> new RuntimeException("Unknown route: " + route);
            };

            return DecodedPacketInfo.valueOf(request, attachment);
        };

        HttpServer server = new HttpServer(HostAndPort.valueOf(properties.getHost(), properties.getPort()), requestResolver);

        server.start();
        ThreadUtils.sleep(Long.MAX_VALUE);
    }


    private static <T> T parseJsonBody(FullHttpRequest rawRequest, Class<T> requestClass) {
        var body = rawRequest.content().toString(StandardCharsets.UTF_8);

        try {
            return JsonUtils.string2Object(body, requestClass);
        } catch (Exception exception) {
            throw new IllegalArgumentException("JSON格式错误", exception);
        }
    }
}
