package com.oaixdog.loginserver.controller;

import com.zfoo.net.NetContext;
import com.zfoo.net.anno.PacketReceiver;
import com.zfoo.net.packet.common.Message;
import com.zfoo.net.router.attachment.HttpAttachment;
import com.zfoo.net.session.Session;
import org.springframework.stereotype.Component;

@Component
public class CommonTroller {

    @PacketReceiver
    public void atMessage(Session session, Message message, HttpAttachment attachment) {
        NetContext.getRouter().send(session, message, attachment);
    }

}
