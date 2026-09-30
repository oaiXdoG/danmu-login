# 抖音 getAccessToken 接口

## 1. 文档信息

- 官方文档：[getAccessToken](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/interface-request-credential/get-access-token)
- 相关文档：[服务端 API 介绍](https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/server-api-introduction)
- 查阅日期：2026-08-12

## 2. 接口能力

`getAccessToken` 用于开发者服务器通过应用的 `appid` 和 `AppSecret` 获取服务端 OpenAPI 调用凭证 `access_token`。

`access_token` 是应用级凭证，不是玩家级凭证：

- 同一个小程序或小游戏共用一份 `access_token`，不能为每个玩家单独获取或分配。
- 它用于调用需要应用身份认证的抖音服务端 API。
- 它不能单独完成玩家登录，也不会直接返回玩家资料、直播间 `room_id` 或弹幕数据。
- 玩家登录时由客户端提交的 SDK token 或 code，属于另一类玩家身份凭证，不能与该 `access_token` 混用。

## 3. 请求定义

### 3.1 请求地址

```http
POST https://developer.toutiao.com/api/apps/v2/token
Content-Type: application/json
```

### 3.2 请求参数

| 参数 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `appid` | string | 是 | 小程序或小游戏 ID |
| `secret` | string | 是 | 应用的 AppSecret，可在开发者后台获取 |
| `grant_type` | string | 是 | 固定填写 `client_credential` |

请求示例：

```json
{
  "appid": "ttabc*****",
  "secret": "d428***********",
  "grant_type": "client_credential"
}
```

## 4. 响应定义

### 4.1 成功响应

```json
{
  "err_no": 0,
  "err_tips": "success",
  "data": {
    "access_token": "0801121***********",
    "expires_in": 7200
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `err_no` | int64 | 业务错误码，`0` 表示成功 |
| `err_tips` | string | 业务结果说明 |
| `data.access_token` | string | 服务端 OpenAPI 调用凭证 |
| `data.expires_in` | int64 | 凭证有效时间，单位为秒 |

### 4.2 错误响应

```json
{
  "err_no": 40017,
  "err_tips": "bad secret",
  "data": {
    "access_token": "",
    "expires_in": 0
  }
}
```

官方文档列出的业务错误码：

| `err_no` | 说明 |
| --- | --- |
| `0` | 请求成功 |
| `-1` | 系统错误 |
| `40015` | `appid` 错误 |
| `40017` | `secret` 错误 |
| `40020` | `grant_type` 不是 `client_credential` |
| 其他 | 可能存在请求参数为空等错误，具体原因以 `err_tips` 为准 |

调用方必须同时检查 HTTP 请求结果和响应中的 `err_no`，不能只依赖 HTTP 状态码判断业务是否成功。

## 5. 有效期与刷新规则

- `access_token` 最长有效期为 2 小时，实际有效时间以 `expires_in` 为准。
- 官方建议每小时更新一次。
- 重复获取会影响上一枚 `access_token`：新旧 token 仅保留约 5 分钟的平滑过渡时间。
- 不应在每次玩家登录时重新请求 `access_token`。

## 6. 在登录服中的定位

该接口是登录服访问抖音服务端 OpenAPI 的基础凭证能力，建议链路如下：

```text
appid + AppSecret
        |
        v
getAccessToken
        |
        v
缓存应用级 access_token
        |
        v
调用需要应用凭证的抖音服务端 API
```

玩家登录链路应区分两类凭证：

```text
客户端 SDK token/code
        |
        v
登录服读取缓存的应用 access_token
        |
        v
调用对应的抖音玩家身份接口
        |
        v
获取玩家标识或资料
        |
        v
查询或创建游戏用户，签发游戏会话凭证
```

具体使用哪个接口将客户端 SDK token 或 code 换取玩家身份，需要以对应的抖音登录或用户接口文档为准，不属于 `getAccessToken` 本身的能力。

## 7. 实现建议

### 7.1 缓存与刷新

- 登录服应提供单例的 token 管理模块，缓存 `access_token`、获取时间和过期时间。
- 刷新时间必须以平台返回的 `expires_in` 计算，并预留安全时间，不能写死为恰好 7200 秒。
- 可以按官方建议每小时主动刷新，或者在接近过期时刷新。
- 下游接口明确返回 token 失效时，可以触发一次受控刷新并重试一次，避免无限重试。
- 多实例部署时应共享 token 状态，并通过分布式锁或等效机制限制同一时间只有一个实例刷新。

### 7.2 安全要求

- `AppSecret` 只能保存在服务端配置或密钥管理系统中，禁止下发到客户端。
- 禁止将 `AppSecret`、完整 `access_token` 写入日志、异常信息或接口响应。
- 配置文件中不要提交真实密钥，生产环境应使用环境变量或密钥管理服务注入。
- 对抖音接口设置合理的连接超时、读取超时和有限重试；只对网络错误、超时或可恢复的平台错误进行重试。

## 8. 与本项目其他功能的关系

| 登录服功能 | 是否由本接口直接完成 | 说明 |
| --- | --- | --- |
| 客户端登录 | 否 | 本接口只获取应用凭证，玩家身份需要调用其他接口验证或兑换 |
| 游戏版本检查 | 否 | 与抖音 `access_token` 无关 |
| 开启直播玩法推送 | 否 | 本接口可为需要应用认证的后续接口提供凭证，但不会自行开启推送 |
| 退出直播玩法推送 | 否 | 退出逻辑由对应推送接口和本项目会话管理实现 |
