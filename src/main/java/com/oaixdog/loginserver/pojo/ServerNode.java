package com.oaixdog.loginserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerNode {

    public static final String STATUS_ONLINE = "online";

    private String type;
    private int id;
    private String host;
    private int port;
    private String innerHost;
    private int innerPort;
    private String status;
    private String version;
    private long startTime;
    private long updateTime;
    private Map<String, Object> stats;

}
