package com.oaixdog.loginserver.protocol;

import com.zfoo.protocol.anno.Protocol;
import com.zfoo.protocol.util.JsonUtils;
import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.StandardCharsets;

@Protocol(id = 1000)
public class LoginRequest {

    private String account;
    private String password;

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
