package com.oaixdog.loginserver.controller;

import com.oaixdog.loginserver.protocol.LoginRequest;
import com.zfoo.net.NetContext;
import com.zfoo.net.anno.PacketReceiver;
import com.zfoo.net.router.attachment.HttpAttachment;
import com.zfoo.net.session.Session;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @PacketReceiver
    public void atLoginRequest(Session session, LoginRequest request, HttpAttachment attachment) {
        NetContext.getRouter().send(session, request, attachment);
    }
}
