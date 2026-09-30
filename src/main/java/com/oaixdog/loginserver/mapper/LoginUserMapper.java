package com.oaixdog.loginserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oaixdog.loginserver.entity.LoginUser;
import org.apache.ibatis.annotations.Param;

public interface LoginUserMapper extends BaseMapper<LoginUser> {

    LoginUser upsert(@Param("appId") String appId, @Param("anchorOpenId") String anchorOpenId, @Param("nickname") String nickname, @Param("avatarUrl") String avatarUrl, @Param("loginIp") String loginIp);

    LoginUser upsertLocal(@Param("appId") String appId, @Param("account") String account, @Param("loginIp") String loginIp);
}
