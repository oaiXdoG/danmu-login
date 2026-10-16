package com.oaixdog.loginserver.pojo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ZooKeeper 服务节点内容，格式见 docs/service-discovery.md。类型和 ID 由节点路径表示，节点存在即在线。
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ServerNode {

    private String host;
    private int port;
    private String innerHost;
    private int innerPort;

}
