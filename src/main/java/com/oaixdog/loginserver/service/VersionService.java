package com.oaixdog.loginserver.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.oaixdog.loginserver.config.ApplicationProperties;
import com.oaixdog.loginserver.entity.GameVersion;
import com.oaixdog.loginserver.exception.BusinessException;
import com.oaixdog.loginserver.mapper.GameVersionMapper;
import com.zfoo.protocol.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class VersionService {

    private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+\\.[0-9]+\\.[0-9]+");

    private final ApplicationProperties properties;
    private final GameVersionMapper gameVersionMapper;

    public VersionCheckResult checkVersion(String clientVersion, String resourceVersion) {
        if (StringUtils.isBlank(clientVersion)) {
            throw new BusinessException("客户端版本不能为空");
        }
        if (!VERSION_PATTERN.matcher(clientVersion.trim()).matches()) {
            throw new BusinessException("客户端版本格式错误");
        }

        if (StringUtils.isBlank(resourceVersion)) {
            throw new BusinessException("客户端资源版本不能为空");
        }
        if (!VERSION_PATTERN.matcher(resourceVersion.trim()).matches()) {
            throw new BusinessException("客户端资源版本格式错误");
        }
        String appId = properties.getDouyinAppId();
        GameVersion gameVersion = gameVersionMapper.selectOne(Wrappers.<GameVersion>lambdaQuery().eq(GameVersion::getAppId, appId));
        if (gameVersion == null) {
            throw new IllegalStateException("服务器版本未配置");
        }
        String serverClientVersion = gameVersion.getClientVersion();
        String serverResourceVersion = gameVersion.getResourceVersion();
        if (StringUtils.isBlank(serverClientVersion)) {
            throw new IllegalStateException("服务器客户端版本未配置");
        }
        if (!VERSION_PATTERN.matcher(serverClientVersion.trim()).matches()) {
            throw new IllegalStateException("服务器客户端版本格式错误");
        }
        if (StringUtils.isBlank(serverResourceVersion)) {
            throw new IllegalStateException("服务器客户端资源版本未配置");
        }
        if (!VERSION_PATTERN.matcher(serverResourceVersion.trim()).matches()) {
            throw new IllegalStateException("服务器客户端资源版本格式错误");
        }
        clientVersion = clientVersion.trim();
        resourceVersion = resourceVersion.trim();
        serverClientVersion = serverClientVersion.trim();
        serverResourceVersion = serverResourceVersion.trim();
        boolean needClientUpdate = compareVersions(clientVersion, serverClientVersion) < 0;
        boolean needResourceUpdate = compareVersions(resourceVersion, serverResourceVersion) < 0;
        return new VersionCheckResult(serverClientVersion, serverResourceVersion, needClientUpdate, needResourceUpdate);
    }

    private int compareVersions(String clientVersion, String serverVersion) {
        String[] clientParts = clientVersion.split("\\.");
        String[] serverParts = serverVersion.split("\\.");
        int maxLength = Math.max(clientParts.length, serverParts.length);

        for (int index = 0; index < maxLength; index++) {
            BigInteger clientPart = index < clientParts.length ? new BigInteger(clientParts[index]) : BigInteger.ZERO;
            BigInteger serverPart = index < serverParts.length ? new BigInteger(serverParts[index]) : BigInteger.ZERO;
            int result = clientPart.compareTo(serverPart);
            if (result != 0) {
                return result;
            }
        }
        return 0;
    }

    public record VersionCheckResult(String serverClientVersion, String serverResourceVersion, boolean needClientUpdate, boolean needResourceUpdate) {}
}
