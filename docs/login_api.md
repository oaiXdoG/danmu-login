# 统一登录入口接口文档

## 接口说明

该接口是客户端的统一登录入口，根据请求中的 `account` 是否为空，选择本地测试登录或平台登录：

- `account` 非空：进入本地测试登录，不调用抖音开放平台接口。
- `account` 为空：根据 `platform` 执行平台登录，目前支持 `douyin`。

- 请求方式：`POST`
- 请求地址：`/api/login`
- Content-Type：`application/json`

## 请求参数

| 字段 | 类型 | 必填条件 | 说明 |
| --- | --- | --- | --- |
| `platform` | String | `account` 为空时必填 | 平台标识，目前支持 `douyin` |
| `account` | String | 本地测试时必填 | 非空时进入本地测试登录，不校验密码，并忽略 `platform` 和 `token` |
| `token` | String | 抖音登录时必填 | 抖音玩法启动 token |
| `clientVersion` | String | 必填 | 客户端程序版本，格式为 `1.0.0` |
| `resourceVersion` | String | 必填 | 客户端资源版本，格式为 `1.0.0` |

### 本地测试登录

```json
{
  "platform": "",
  "account": "local-test-1",
  "token": "",
  "clientVersion": "1.0.0",
  "resourceVersion": "1.0.0"
}
```

本地账号存在时直接获取，不存在时自动创建；不校验密码。

### 抖音平台登录

```json
{
  "platform": "douyin",
  "account": "",
  "token": "抖音玩法启动 token",
  "clientVersion": "1.0.0",
  "resourceVersion": "1.0.0"
}
```

抖音登录会使用启动 token 获取直播间信息，并开启评论、礼物、点赞和粉丝团推送任务。任一推送任务连续重试后仍失败，登录失败。

## 成功响应

HTTP 状态码为 `200`。

响应外层字段遵循 [HTTP 统一响应格式](api-response.md)，登录业务数据位于 `data`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "loginTicket": "AES-GCM 加密票据",
    "host": "127.0.0.1",
    "port": 9020
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data.loginTicket` | String | 登录服加密的完整登录 JSON；游戏服解密后取得认证信息，详见 [登录票据规则](login-ticket.md) |
| `data.host` | String | 游戏服务器连接地址，仅用于客户端建立连接 |
| `data.port` | int | 游戏服务器连接端口，仅用于客户端建立连接 |

`userId`、`anchorOpenId`、区服标识及其他认证信息不在响应外层返回，统一封装在 `loginTicket` 中。当前票据明文包含 `userId`、`anchorOpenId` 和 `zoneId`。

## 登录失败

客户端程序版本或资源版本低于服务器版本时，登录失败并返回对应的更新提示。业务失败仍使用 HTTP `200`，响应格式如下：

```json
{
  "code": 100001,
  "message": "客户端版本过低，请更新至 1.1.0",
  "data": null
}
```

目标区游戏服不存在或处于维护中时，登录失败，`message` 为「服务器维护中」。

请求参数缺失等可预期业务错误同样使用 HTTP `200`。系统或传输层异常使用实际 HTTP 状态码，响应体格式见 [HTTP 统一响应格式](api-response.md)。
