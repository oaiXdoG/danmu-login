package com.oaixdog.loginserver.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.oaixdog.loginserver.config.ApplicationProperties;
import com.zfoo.protocol.util.JsonUtils;
import com.zfoo.protocol.util.StringUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
@RequiredArgsConstructor
public class DouyinOpenApiService {

    private static final URI ACCESS_TOKEN_URI = URI.create("https://developer.toutiao.com/api/apps/v2/token");
    private static final URI LIVE_ROOM_INFO_URI = URI.create("https://webcast.bytedance.com/api/webcastmate/info");
    private static final URI START_LIVE_DATA_TASK_URI = URI.create("https://webcast.bytedance.com/api/live_data/task/start");
    private static final URI STOP_LIVE_DATA_TASK_URI = URI.create("https://webcast.bytedance.com/api/live_data/task/stop");
    private static final String QUERY_LIVE_DATA_TASK_STATUS_URL = "https://webcast.bytedance.com/api/live_data/task/get";
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String JSON_CONTENT_TYPE = "application/json";
    private static final String ACCESS_TOKEN_HEADER = "x-token";
    private static final String LIVE_DATA_ACCESS_TOKEN_HEADER = "access-token";
    private static final String CLIENT_CREDENTIAL_GRANT_TYPE = "client_credential";
    private static final String TOKEN_KEY_PREFIX = "douyin:access_token:";
    private static final long TOKEN_EXPIRE_OFFSET_SECONDS = 60;
    private static final int HTTP_SUCCESS_MIN = 200;
    private static final int HTTP_SUCCESS_MAX = 300;

    private final ApplicationProperties properties;
    private final RedisService redisService;
    private final ReentrantReadWriteLock tokenLock = new ReentrantReadWriteLock();

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(HTTP_TIMEOUT)
            .build();

    public LiveRoomInfo getLiveRoomInfo(String startToken) {
        if (StringUtils.isBlank(startToken)) {
            throw new IllegalArgumentException("抖音玩法启动 token 不能为空");
        }

        AccessToken accessToken = getAccessTokenWithCache();
        String body = JsonUtils.object2String(new LiveRoomInfoRequest(startToken));
        HttpRequest request = HttpRequest.newBuilder(LIVE_ROOM_INFO_URI)
                .timeout(HTTP_TIMEOUT)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .header(ACCESS_TOKEN_HEADER, accessToken.value())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!isSuccessfulStatus(response.statusCode())) {
                throw new IllegalStateException("获取抖音直播信息失败，HTTP " + response.statusCode());
            }

            LiveRoomInfoResponse result = JsonUtils.string2Object(response.body(), LiveRoomInfoResponse.class);
            if (result == null || result.data() == null || result.data().info() == null) {
                throw new IllegalStateException("获取抖音直播信息失败，响应中没有直播间信息");
            }
            return result.data().info();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取抖音直播信息被中断", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("请求抖音直播信息接口失败", exception);
        }
    }

    public void startLiveDataTask(long roomId, String messageType) {
        AccessToken accessToken = getAccessTokenWithCache();
        String body = JsonUtils.object2String(new LiveDataTaskRequest(String.valueOf(roomId), properties.getDouyinAppId(), messageType));
        HttpRequest request = HttpRequest.newBuilder(START_LIVE_DATA_TASK_URI)
                .timeout(HTTP_TIMEOUT)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .header(LIVE_DATA_ACCESS_TOKEN_HEADER, accessToken.value())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!isSuccessfulStatus(response.statusCode())) {
                throw new IllegalStateException("开启抖音推送任务失败，HTTP " + response.statusCode());
            }
            LiveDataTaskResponse result = JsonUtils.string2Object(response.body(), LiveDataTaskResponse.class);
            if (result == null || result.errNo() != 0 || result.data() == null || StringUtils.isBlank(result.data().taskId())) {
                long errNo = result == null ? -1 : result.errNo();
                String errMsg = result == null ? "响应为空" : result.errMsg();
                throw new IllegalStateException("开启抖音推送任务失败，err_no=" + errNo + ", err_msg=" + errMsg);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("开启抖音推送任务被中断", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("请求抖音开启推送任务接口失败", exception);
        }
    }

    public void stopLiveDataTask(long roomId, String messageType) {
        AccessToken accessToken = getAccessTokenWithCache();
        String body = JsonUtils.object2String(new LiveDataTaskRequest(String.valueOf(roomId), properties.getDouyinAppId(), messageType));
        HttpRequest request = HttpRequest.newBuilder(STOP_LIVE_DATA_TASK_URI)
                .timeout(HTTP_TIMEOUT)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .header(LIVE_DATA_ACCESS_TOKEN_HEADER, accessToken.value())
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!isSuccessfulStatus(response.statusCode())) {
                throw new IllegalStateException("停止抖音推送任务失败，HTTP " + response.statusCode());
            }
            LiveDataTaskResponse result = JsonUtils.string2Object(response.body(), LiveDataTaskResponse.class);
            if (result == null || result.errNo() != 0) {
                long errNo = result == null ? -1 : result.errNo();
                String errMsg = result == null ? "响应为空" : result.errMsg();
                throw new IllegalStateException("停止抖音推送任务失败，err_no=" + errNo + ", err_msg=" + errMsg);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("停止抖音推送任务被中断", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("请求抖音停止推送任务接口失败", exception);
        }
    }

    public int queryLiveDataTaskStatus(long roomId, String messageType) {
        AccessToken accessToken = getAccessTokenWithCache();
        String roomIdParam = URLEncoder.encode(String.valueOf(roomId), StandardCharsets.UTF_8);
        String appIdParam = URLEncoder.encode(properties.getDouyinAppId(), StandardCharsets.UTF_8);
        String messageTypeParam = URLEncoder.encode(messageType, StandardCharsets.UTF_8);
        URI uri = URI.create(QUERY_LIVE_DATA_TASK_STATUS_URL + "?roomid=" + roomIdParam + "&appid=" + appIdParam + "&msg_type=" + messageTypeParam);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(HTTP_TIMEOUT)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .header(LIVE_DATA_ACCESS_TOKEN_HEADER, accessToken.value())
                .GET()
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!isSuccessfulStatus(response.statusCode())) {
                throw new IllegalStateException("查询抖音推送任务状态失败，HTTP " + response.statusCode());
            }
            LiveDataTaskResponse result = JsonUtils.string2Object(response.body(), LiveDataTaskResponse.class);
            if (result == null || result.errNo() != 0 || result.data() == null || result.data().status() == null) {
                long errNo = result == null ? -1 : result.errNo();
                String errMsg = result == null ? "响应为空" : result.errMsg();
                throw new IllegalStateException("查询抖音推送任务状态失败，err_no=" + errNo + ", err_msg=" + errMsg);
            }
            return result.data().status();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("查询抖音推送任务状态被中断", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("请求抖音查询推送任务状态接口失败", exception);
        }
    }

    public AccessToken getAccessTokenWithCache() {
        if (StringUtils.isBlank(properties.getDouyinAppId()) || StringUtils.isBlank(properties.getDouyinAppSecret())) {
            throw new IllegalStateException("未配置 DOUYIN_APP_ID 或 DOUYIN_APP_SECRET");
        }

        String tokenKey = TOKEN_KEY_PREFIX + properties.getDouyinAppId();
        Lock readLock = tokenLock.readLock();
        readLock.lock();
        try {
            AccessToken cachedToken = getCachedToken(tokenKey);
            if (cachedToken != null) {
                return cachedToken;
            }
        } finally {
            readLock.unlock();
        }

        Lock writeLock = tokenLock.writeLock();
        writeLock.lock();
        try {
            AccessToken cachedToken = getCachedToken(tokenKey);
            if (cachedToken != null) {
                return cachedToken;
            }

            AccessToken accessToken = getAccessToken();
            long redisExpireSeconds = accessToken.expiresIn() - TOKEN_EXPIRE_OFFSET_SECONDS;
            redisService.set(tokenKey, accessToken.value(), Duration.ofSeconds(redisExpireSeconds));
            return accessToken;
        } finally {
            writeLock.unlock();
        }
    }

    private AccessToken getCachedToken(String tokenKey) {
        String value = redisService.get(tokenKey);
        if (StringUtils.isBlank(value)) {
            return null;
        }

        long expiresIn = redisService.getExpireSeconds(tokenKey);
        return expiresIn > 0 ? new AccessToken(value, expiresIn) : null;
    }

    public AccessToken getAccessToken() {
        if (StringUtils.isBlank(properties.getDouyinAppId()) || StringUtils.isBlank(properties.getDouyinAppSecret())) {
            throw new IllegalStateException("未配置 DOUYIN_APP_ID 或 DOUYIN_APP_SECRET");
        }

        String body = JsonUtils.object2String(new AccessTokenRequest(
                properties.getDouyinAppId(),
                properties.getDouyinAppSecret(),
                CLIENT_CREDENTIAL_GRANT_TYPE));
        HttpRequest request = HttpRequest.newBuilder(ACCESS_TOKEN_URI)
                .timeout(HTTP_TIMEOUT)
                .header(CONTENT_TYPE_HEADER, JSON_CONTENT_TYPE)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (!isSuccessfulStatus(response.statusCode())) {
                throw new IllegalStateException("获取抖音 access_token 失败，HTTP " + response.statusCode());
            }

            AccessTokenResponse result = JsonUtils.string2Object(response.body(), AccessTokenResponse.class);
            if (result == null || result.errNo() != 0) {
                long errNo = result == null ? -1 : result.errNo();
                String errTips = result == null ? "响应为空" : result.errTips();
                throw new IllegalStateException(
                        "获取抖音 access_token 失败，err_no=" + errNo + ", err_tips=" + errTips);
            }
            if (result.data() == null || StringUtils.isBlank(result.data().accessToken())) {
                throw new IllegalStateException("获取抖音 access_token 失败，响应中没有 access_token");
            }

            return new AccessToken(result.data().accessToken(), result.data().expiresIn());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("获取抖音 access_token 被中断", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("请求抖音 getAccessToken 接口失败", exception);
        }
    }

    private boolean isSuccessfulStatus(int statusCode) {
        return statusCode >= HTTP_SUCCESS_MIN && statusCode < HTTP_SUCCESS_MAX;
    }

    public record AccessToken(String value, long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record LiveRoomInfo(
            @JsonProperty("room_id") long roomId,
            @JsonProperty("anchor_open_id") String anchorOpenId,
            @JsonProperty("avatar_url") String avatarUrl,
            @JsonProperty("nick_name") String nickName) {
    }

    private record LiveRoomInfoRequest(String token) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LiveRoomInfoResponse(LiveRoomData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LiveRoomData(LiveRoomInfo info) {
    }

    private record LiveDataTaskRequest(String roomid, String appid, @JsonProperty("msg_type") String messageType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LiveDataTaskResponse(@JsonProperty("err_no") long errNo, @JsonProperty("err_msg") String errMsg, LiveDataTaskData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LiveDataTaskData(@JsonProperty("task_id") String taskId, Integer status) {
    }

    private record AccessTokenRequest(
            @JsonProperty("appid") String appId,
            @JsonProperty("secret") String secret,
            @JsonProperty("grant_type") String grantType) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AccessTokenResponse(
            @JsonProperty("err_no") long errNo,
            @JsonProperty("err_tips") String errTips,
            AccessTokenData data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AccessTokenData(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("expires_in") long expiresIn) {
    }
}
