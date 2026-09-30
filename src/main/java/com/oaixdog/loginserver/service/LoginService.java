package com.oaixdog.loginserver.service;

import com.oaixdog.loginserver.config.ApplicationProperties;
import com.oaixdog.loginserver.entity.GameServer;
import com.oaixdog.loginserver.entity.LoginUser;
import com.oaixdog.loginserver.exception.BusinessException;
import com.oaixdog.loginserver.mapper.GameServerMapper;
import com.oaixdog.loginserver.mapper.LoginUserMapper;
import com.oaixdog.loginserver.pojo.LoginTicketData;
import com.zfoo.protocol.util.JsonUtils;
import com.zfoo.protocol.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoginService {

    private static final int LIVE_DATA_TASK_RETRY_TIMES = 3;
    private static final String DOUYIN_PLATFORM = "douyin";
    private static final List<String> LIVE_DATA_MESSAGE_TYPES = List.of("live_comment", "live_gift", "live_like", "live_fansclub");

    private final ApplicationProperties properties;
    private final DouyinOpenApiService douyinOpenApiService;
    private final LoginUserMapper loginUserMapper;
    private final GameServerMapper gameServerMapper;
    private final LoginTicketService loginTicketService;
    private final VersionService versionService;

    public LoginResult login(String platform, String account, String token, String clientVersion, String resourceVersion, String loginIp) {
        boolean localLogin = StringUtils.isNotBlank(account);
        LoginUser user = null;
        DouyinOpenApiService.LiveRoomInfo liveRoomInfo = null;
        long roomId;

        if (localLogin) {
            String localAccount = account.trim();
            user = loginUserMapper.upsertLocal(properties.getDouyinAppId(), localAccount, loginIp);
            roomId = user.getId();
        } else {
            if (StringUtils.isBlank(platform)) {
                throw new BusinessException("登录平台不能为空");
            }
            if (!DOUYIN_PLATFORM.equalsIgnoreCase(platform.trim())) {
                throw new BusinessException("暂不支持登录平台 " + platform.trim());
            }
            if (StringUtils.isBlank(token)) {
                throw new BusinessException("抖音玩法启动 token 不能为空");
            }
            liveRoomInfo = douyinOpenApiService.getLiveRoomInfo(token);
            roomId = liveRoomInfo.roomId();
            if (roomId <= 0) {
                throw new IllegalStateException("抖音直播信息中没有直播间 ID");
            }
            startLiveDataTasks(roomId);
            if (StringUtils.isBlank(liveRoomInfo.anchorOpenId())) {
                throw new IllegalStateException("抖音直播信息中没有主播身份");
            }
        }

        VersionService.VersionCheckResult versionResult = versionService.checkVersion(clientVersion, resourceVersion);
        if (versionResult.needClientUpdate()) {
            throw new BusinessException("客户端版本过低，请更新至 " + versionResult.serverClientVersion());
        }
        if (versionResult.needResourceUpdate()) {
            throw new BusinessException("客户端资源版本过低，请更新至 " + versionResult.serverResourceVersion());
        }

        if (!localLogin) {
            String nickname = liveRoomInfo.nickName() == null ? "" : liveRoomInfo.nickName();
            String avatarUrl = liveRoomInfo.avatarUrl() == null ? "" : liveRoomInfo.avatarUrl();
            user = loginUserMapper.upsert(properties.getDouyinAppId(), liveRoomInfo.anchorOpenId(), nickname, avatarUrl, loginIp);
        }
        GameServer gameServer = gameServerMapper.selectFirstOnline();
        if (gameServer == null) {
            throw new IllegalStateException("当前没有可用的游戏服务器");
        }

        LoginTicketData ticketData = LoginTicketData.valueOf(user.getId(), user.getAnchorOpenId());
        String loginJson = JsonUtils.object2String(ticketData);
        String loginTicket = loginTicketService.encrypt(loginJson);

        return new LoginResult(loginTicket, gameServer.getServerIp(), gameServer.getServerPort());
    }

    private void startLiveDataTasks(long roomId) {
        for (String messageType : LIVE_DATA_MESSAGE_TYPES) {
            RuntimeException startException = null;
            for (int retry = 0; retry <= LIVE_DATA_TASK_RETRY_TIMES; retry++) {
                try {
                    douyinOpenApiService.startLiveDataTask(roomId, messageType);
                    startException = null;
                    break;
                } catch (RuntimeException exception) {
                    startException = exception;
                }
            }
            if (startException == null) {
                continue;
            }
            for (String taskMessageType : LIVE_DATA_MESSAGE_TYPES) {
                try {
                    douyinOpenApiService.stopLiveDataTask(roomId, taskMessageType);
                } catch (RuntimeException stopException) {
                    log.error("关闭抖音推送任务失败，roomId={}，messageType={}", roomId, taskMessageType, stopException);
                }
            }
            throw new IllegalStateException("开启抖音推送任务失败，messageType=" + messageType, startException);
        }
    }

    public record LoginResult(String loginTicket, String host, int port) {}
}
