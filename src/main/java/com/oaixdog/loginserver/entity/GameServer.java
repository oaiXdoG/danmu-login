package com.oaixdog.loginserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@TableName("game_server")
public class GameServer {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private int serverId;
    private String serverName;
    private String serverIp;
    private Integer serverPort;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
