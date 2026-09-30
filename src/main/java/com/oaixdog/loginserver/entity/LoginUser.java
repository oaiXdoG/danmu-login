package com.oaixdog.loginserver.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Setter
@Getter
@TableName("login_user")
public class LoginUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String account;
    private String appId;
    private String anchorOpenId;
    private String nickname;
    private String avatarUrl;
    private String loginIp;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
