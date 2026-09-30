# 抖音直播信息接口

## 1. 文档信息

- 官方文档：[直播信息](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/live-room-scope/live-info)
- 接口 Scope：`webcastmate_info`
- 查阅日期：2026-08-12

## 2. 接口能力

主播通过直播伴侣或移动端云启动玩法后，平台会向玩法客户端传入一个启动 `token`。客户端需要把该 `token` 传给登录服，登录服再调用直播信息接口获取当前直播间和主播信息。

接口返回的主要信息包括：

- 直播间 `room_id`
- 主播 `anchor_open_id`
- 主播昵称 `nick_name`
- 主播头像 `avatar_url`

客户端启动 `token` 的有效期为 30 分钟。

## 3. 调用流程

```text
直播伴侣或移动端云启动玩法
        |
        v
平台向玩法客户端传入启动 token
        |
        v
客户端将启动 token 发送给登录服
        |
        v
登录服获取应用级 access_token
        |
        v
登录服调用直播信息接口
        |
        v
获得 room_id 和主播信息
```

该流程涉及两种不同的 token：

| token | 来源 | 请求位置 | 用途 |
| --- | --- | --- | --- |
| 应用级 `access_token` | 使用 `appid + AppSecret` 调用 `getAccessToken` | 请求头 `x-token` | 证明调用方应用身份 |
| 玩法启动 `token` | 直播伴侣或移动端云启动玩法时传给客户端 | JSON Body 的 `token` | 标识本次玩法启动和对应直播间 |

## 4. 请求定义

### 4.1 请求地址

```http
POST https://webcast.bytedance.com/api/webcastmate/info
Content-Type: application/json
x-token: <应用级 access_token>
```

### 4.2 请求头

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `Content-Type` | string | 是 | 固定为 `application/json` |
| `x-token` | string | 是 | 通过 `getAccessToken` 获取的应用级凭证 |

### 4.3 请求体

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `token` | string | 是 | 客户端收到的玩法启动 token |

请求示例：

```json
{
  "token": "客户端收到的玩法启动 token"
}
```

## 5. 响应定义

正常响应示例：

```json
{
  "data": {
    "info": {
      "room_id": 7214015683695250235,
      "anchor_open_id": "_000oJIu6APhomK7KIBGqSYm5XYPxCJB_xxx",
      "avatar_url": "https://p11.douyinpic.com/example.jpeg",
      "nick_name": "主播昵称"
    }
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data.info.room_id` | int64 | 当前直播间 ID |
| `data.info.anchor_open_id` | string | 主播在当前应用下的 OpenID |
| `data.info.avatar_url` | string | 主播头像地址 |
| `data.info.nick_name` | string | 主播昵称 |

响应中的 `ack_cfg` 和 `linker_info` 是平台预留信息，当前登录服不使用。

## 6. 使用限制与错误码

- 单个 `app_id` 调用上限为 10 次/秒。
- 客户端启动 token 有效期为 30 分钟，登录服应在收到后尽快调用。

| HTTP 状态码 | 错误码 | 说明 |
| --- | --- | --- |
| `200` | `40001` | 请求参数有误 |
| `200` | `40002` | 当前小程序或小游戏没有该项能力 |
| `200` | `40007` | 调用频率过高 |
| `200` | `-1` | 平台内部异常 |

## 7. 项目实现

接口封装位于 `DouyinOpenApiService`：

```java
var liveRoomInfo = douyinOpenApiService.getLiveRoomInfo(startToken);

var roomId = liveRoomInfo.roomId();
var anchorOpenId = liveRoomInfo.anchorOpenId();
var avatarUrl = liveRoomInfo.avatarUrl();
var nickName = liveRoomInfo.nickName();
```

`getLiveRoomInfo` 会通过 `getAccessTokenWithCache` 获取应用级 token，并将客户端启动 token 放入请求体后调用官方直播信息接口。
