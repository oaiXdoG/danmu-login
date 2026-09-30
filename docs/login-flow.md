# 登录流程

## 1. 流程目标

登录服负责完成以下工作：

1. 根据 `account` 是否为空选择本地测试登录或平台登录。
2. 本地测试登录使用唯一 `account` 创建或获取用户，不调用平台接口。
3. 抖音登录使用玩法启动 token 获取直播间信息并开启四种推送任务。
4. 完成身份处理后校验客户端程序版本和资源版本。
5. 在 PostgreSQL 中查询或创建应用级游戏用户。
6. 从服务器信息表选择一台可用游戏服。
7. 生成包含 `userId` 和 `anchorOpenId` 的 AES-GCM 登录票据，并返回游戏服连接地址。

本阶段不处理客户端与游戏服之间的 TCP 连接、游戏服注册接口或心跳接口。

## 2. 完整流程

```text
客户端提交 platform + account + token + 版本
        |
        v
account 是否为空
        |
        +-- 否：按唯一 account 创建或获取本地用户
        |       anchor_open_id 和 room_id 均使用用户数据库 ID
        |
        +-- 是：根据 platform 执行平台登录，目前支持 douyin
                使用 token 获取直播间和主播资料
                依次开启评论、礼物、点赞、粉丝团推送任务
                单项失败后重试 3 次，仍失败则全部停止并登录失败
        |
        v
校验客户端程序版本和资源版本
        |
        v
抖音用户按 app_id + anchor_open_id 查询或创建 login_user
        |
        v
从 game_server 选择第一台 ONLINE 服务器
        |
        v
使用登录服密钥生成 AES-GCM loginTicket
        |
        v
返回 loginTicket 和游戏服路由地址
```

## 3. 登录请求

```http
POST /api/login
Content-Type: application/json
```

```json
{
  "platform": "douyin",
  "account": "",
  "token": "直播伴侣或移动端云传给客户端的玩法启动 token",
  "clientVersion": "1.0.0",
  "resourceVersion": "1.0.0"
}
```

| 字段 | 说明 |
| --- | --- |
| `platform` | 平台标识；`account` 为空时必填，目前支持 `douyin` |
| `account` | 本地测试账号；非空时进入本地登录并忽略 `platform` 和 `token` |
| `token` | 抖音玩法启动 token；抖音登录时必填，有效期为 30 分钟 |
| `clientVersion` | 客户端程序版本，代码、SDK 或协议变化时更新 |
| `resourceVersion` | 客户端资源版本，图片、配置或音频等资源变化时更新 |

## 4. 版本校验

服务器当前客户端版本和资源版本保存在 `game_version` 表，并根据服务端配置的 `douyin.app-id` 查询对应记录。任一客户端版本低于对应的服务器版本时需要更新并停止登录；等于或高于服务器版本时不做处理。版本校验在本地用户处理或平台身份处理完成后执行。

版本查询接口：

```http
POST /api/version
Content-Type: application/json
```

```json
{
  "clientVersion": "1.0.0",
  "resourceVersion": "1.0.0"
}
```

```json
{
  "serverClientVersion": "1.1.0",
  "serverResourceVersion": "1.2.0",
  "needClientUpdate": true,
  "needResourceUpdate": true
}
```

版本固定为三段数字格式，并从左到右依次比较主版本、次版本和修订版本，因此 `1.10.0` 高于 `1.9.0`，`2.0.0` 高于 `1.99.99`。更新服务器版本：

```sql
UPDATE game_version
SET client_version = '1.1.0',
    resource_version = '1.2.0',
    updated_at = CURRENT_TIMESTAMP
WHERE app_id = 对应的数值型抖音 appId;
```

## 5. 用户身份与持久化

本地登录和平台登录共用 `login_user` 表：

- 本地登录使用唯一 `account` 创建或获取用户，不校验密码。首次创建后使用用户数据库 ID 的字符串值作为 `anchor_open_id`，`room_id` 也使用该数据库 ID。
- 抖音玩法启动 token 是短期凭证，不能作为用户唯一标识。抖音登录使用直播信息接口返回的 `anchor_open_id` 标识主播用户。

用户唯一约束：

```text
(app_id, anchor_open_id)
```

登录时通过 MyBatis-Plus Mapper 调用 PostgreSQL upsert，完成查询或创建，并同步登录信息。PostgreSQL 专用的 `ON CONFLICT ... RETURNING` 位于 Mapper XML 中，不嵌入 Java 业务代码。

`room_id` 属于当前直播会话，不写入用户表。

## 6. 游戏服选择

游戏服信息保存在 `game_server` 表。本阶段按照以下规则选择：

```sql
WHERE status = 'ONLINE'
ORDER BY id
LIMIT 1
```

`game_server.id` 是数据库自增主键；`server_id` 是手动指定的业务区服 ID，并具有唯一约束。新增游戏服时只需要指定 `server_id`，不需要填写 `id`。

首次测试前至少需要登记一台在线游戏服：

```sql
INSERT INTO game_server (
    server_id,
    server_name,
    server_ip,
    server_port,
    status
) VALUES (
    1,
    '游戏服 1',
    '127.0.0.1',
    9001,
    'ONLINE'
)
ON CONFLICT (server_id) DO UPDATE SET
    server_name = EXCLUDED.server_name,
    server_ip = EXCLUDED.server_ip,
    server_port = EXCLUDED.server_port,
    status = EXCLUDED.status,
    updated_at = CURRENT_TIMESTAMP;
```

后续实现滚服或用户绑定服务器时，只需要替换服务器选择策略。

## 7. 登录票据

登录票据字段名为 `loginTicket`。它不是 JWT，不是 MD5 签名字符串，也不写入登录服 Redis。

### 7.1 明文内容

当前业务层传入的 JSON 示例为：

```json
{
  "userId": 10001,
  "anchorOpenId": "主播唯一标识"
}
```

当前登录示例包含 `userId` 和 `anchorOpenId`；登录票据加密服务不固定或解析业务字段，后续可以直接扩展这段 JSON。

### 7.2 加密算法

代码使用：

```text
AES/GCM/NoPadding
```

配置项：

```properties
ticket-key=<Base64 编码的 AES 密钥>
```

当前配置使用 Base64 解码后 32 字节的 AES-256 密钥。密钥只应存在登录服和游戏服，客户端不能获取。

每次加密都会生成新的 12 字节随机 IV。AES-GCM 自动生成 16 字节认证 Tag，用于发现密文被篡改。

### 7.3 票据编码格式

加密结果按以下顺序拼接：

```text
12 字节 IV + AES-GCM 密文（末尾包含 16 字节 Tag）
```

拼接结果使用 Base64 URL 安全编码且不带填充，作为 `loginTicket` 返回。

### 7.4 游戏服解密

游戏服使用相同的 AES 密钥处理票据：

```text
Base64URL 解码
    -> 读取前 12 字节 IV
    -> 读取剩余密文和 Tag
    -> AES-GCM 校验 Tag 并解密
    -> 返回原始 JSON 字符串
```

密钥错误、票据格式错误或密文被修改时，Tag 校验失败，游戏服必须拒绝登录，不能使用解密失败时的 JSON 内容。解密服务只返回原始 JSON，业务层负责解析其中的字段。

### 7.5 当前边界

当前登录票据和统一登录响应都没有过期时间字段。因此票据本身暂时不能被游戏服独立判断 5 分钟过期；过期校验需要后续明确规则后再增加字段或机制。

当前也没有一次性消费规则，重复提交同一张未失效票据不会因为票据本身而自动拒绝。

## 8. 登录响应

```json
{
  "loginTicket": "AES-GCM 登录票据",
  "host": "127.0.0.1",
  "port": 9020
}
```

`host` 和 `port` 仅用于客户端连接游戏服；`userId`、`anchorOpenId` 及其他认证信息不在响应外层返回，统一封装在 `loginTicket` 中。

## 9. 数据库与持久层

`src/main/resources/db/schema.sql` 始终保存当前最新的完整表结构。项目启动时不自动执行该文件；表结构发生变化时直接覆盖这份 SQL，不保留版本迁移记录。

当前数据表：

| 表 | 用途 |
| --- | --- |
| `login_user` | 保存本地测试账号、平台用户身份与游戏内部用户的映射 |
| `game_server` | 保存区服 ID、服务器名称、客户端地址和状态 |
| `game_version` | 按应用 ID 保存服务器要求的客户端程序版本和资源版本 |

Java 持久层使用 MyBatis-Plus：

| 目录 | 用途 |
| --- | --- |
| `entity` | 数据库实体及表映射 |
| `mapper` | MyBatis-Plus Mapper 接口 |
| `resources/mapper` | PostgreSQL 专用查询和 upsert |
| `resources/db/schema.sql` | 当前最新的完整 PostgreSQL 表结构，需手工执行 |
