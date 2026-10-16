package com.oaixdog.loginserver.service;

import com.oaixdog.loginserver.config.ApplicationProperties;
import com.oaixdog.loginserver.pojo.ServerNode;
import com.zfoo.net.NetContext;
import com.zfoo.protocol.util.JsonUtils;
import com.zfoo.scheduler.manager.SchedulerBus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDiscoveryService {

    private static final long REFRESH_SECONDS = 10;

    private final ApplicationProperties properties;
    private final Map<Integer, ServerNode> logicNodes = new ConcurrentHashMap<>();
    private ServerNode loginNode;
    private String loginPath;

    public void start() {
        String rootPath = "/danmu/" + properties.getAppKey();
        String logicPath = rootPath + "/logic";
        NetContext.getConfigManager().getRegistry().addListener(logicPath, (path, data) -> updateLogic(logicPath, path, data), path -> removeLogic(logicPath, path));

        loginNode = new ServerNode();
        loginNode.setHost(properties.getPublicHost());
        loginNode.setPort(properties.getPort());
        loginNode.setInnerHost(properties.getInnerHost());
        loginPath = rootPath + "/login/" + properties.getInnerHost() + ":" + properties.getPort();
        refreshLoginNode();
        SchedulerBus.scheduleAtFixedRate(this::refreshLoginNode, REFRESH_SECONDS, TimeUnit.SECONDS);
    }

    public ServerNode getLogic(int zoneId) {
        return logicNodes.get(zoneId);
    }

    /**
     * addData 在节点不存在时创建；与 ZooKeeper 断开、临时节点丢失后由下一次刷新补回。
     */
    private void refreshLoginNode() {
        try {
            byte[] data = JsonUtils.object2String(loginNode).getBytes(StandardCharsets.UTF_8);
            NetContext.getConfigManager().getRegistry().addData(loginPath, data, CreateMode.EPHEMERAL);
        } catch (Exception e) {
            log.error("刷新登录服节点失败，path={}", loginPath, e);
        }
    }

    private void updateLogic(String logicPath, String path, byte[] data) {
        Integer zoneId = parseZoneId(logicPath, path);
        if (zoneId == null) {
            return;
        }
        try {
            ServerNode node = JsonUtils.string2Object(new String(data, StandardCharsets.UTF_8), ServerNode.class);
            logicNodes.put(zoneId, node);
        } catch (Exception e) {
            log.error("游戏服节点解析失败，path={}", path, e);
        }
    }

    private void removeLogic(String logicPath, String path) {
        Integer zoneId = parseZoneId(logicPath, path);
        if (zoneId != null && logicNodes.remove(zoneId) != null) {
            log.info("游戏服下线，zoneId={}", zoneId);
        }
    }

    private Integer parseZoneId(String logicPath, String path) {
        if (!path.startsWith(logicPath + "/")) {
            return null;
        }
        String name = path.substring(logicPath.length() + 1);
        try {
            return Integer.valueOf(name);
        } catch (NumberFormatException e) {
            log.warn("忽略无法识别的游戏服节点，path={}", path);
            return null;
        }
    }
}
