package com.oaixdog.loginserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@TableName("game_version")
public class GameVersion {

    @TableId(type = IdType.AUTO)
    private Integer id;
    private String appId;
    private String clientVersion;
    private String resourceVersion;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

}
