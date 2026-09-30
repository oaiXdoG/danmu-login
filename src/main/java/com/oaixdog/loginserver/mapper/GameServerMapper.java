package com.oaixdog.loginserver.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.oaixdog.loginserver.entity.GameServer;

public interface GameServerMapper extends BaseMapper<GameServer> {

    GameServer selectFirstOnline();
}
