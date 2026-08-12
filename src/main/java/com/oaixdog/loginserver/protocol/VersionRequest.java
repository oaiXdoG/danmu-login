package com.oaixdog.loginserver.protocol;

import com.zfoo.protocol.anno.Protocol;

@Protocol(id = 1001)
public class VersionRequest {

    public String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

}
