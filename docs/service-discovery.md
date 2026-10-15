# 服务注册与发现

本文档约定同一个应用下登录服（login）、游戏服（logic）、回调服（callback）如何在 ZooKeeper 上注册自己、如何发现彼此。三类服务都必须遵守。

状态：登录服（监听 logic、注册自己）、游戏服 daoshinvgui（注册 logic 节点）已实现；回调服未实现。

## 1. 概念

| 名称 | 说明 |
| --- | --- |
| 应用 | 一款弹幕游戏，用管理后台项目的 App Key 标识。一个应用部署自己的一套登录服、游戏服、回调服，互不影响 |
| logic | 游戏服。一个区就是一个游戏服进程，区 ID 就是它的标识；进程部署在哪台机器由运维自行决定 |
| login | 登录服，整个应用共用，不分区 |
| callback | 回调服，整个应用共用，不分区 |
| 区 ID | 游戏区编号，从 1 开始，当前只有 1 区 |
| 实例 ID | callback 的实例编号，同类型内唯一。只部署一个实例时为 1 |
| 实例地址 | login 不配置编号，用自己的 `内网地址:端口` 作为节点名 |

游戏服之间不做负载均衡：主播属于哪个区，就连那个区的游戏服。

## 2. 节点路径

```text
/danmu/<appKey>/logic/<区ID>
/danmu/<appKey>/login/<内网地址:端口>
/danmu/<appKey>/callback/<实例ID>
```

示例：

```text
/danmu/<appKey>/logic/1
/danmu/<appKey>/login/10.0.0.8:9030
/danmu/<appKey>/callback/1
```

- 叶子节点（区 ID、实例 ID、实例地址那一层）是服务节点，使用临时节点（`EPHEMERAL`），由服务自己创建。
- 中间目录节点是持久节点，不存数据，由第一次写入或监听时自动创建。
- 多个应用可以共用一个 ZooKeeper，通过 `<appKey>` 隔离。

## 3. 节点内容

服务节点的数据为 UTF-8 JSON，三类服务格式相同：

```json
{
  "type": "logic",
  "id": 1,
  "host": "111.230.143.165",
  "port": 9020,
  "innerHost": "10.0.0.8",
  "innerPort": 0,
  "status": "online",
  "version": "1.0.0",
  "startTime": 1760500000000,
  "updateTime": 1760500010000,
  "stats": {
    "anchors": 12,
    "rooms": 10
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `type` | string | `logic`、`login`、`callback` |
| `id` | number | logic 为区 ID，callback 为实例 ID，与路径最后一段一致；login 固定为 0 |
| `host` / `port` | string / number | 对外地址，客户端连接用；没有对外端口的服务填 `port=0` |
| `innerHost` / `innerPort` | string / number | 内网地址，服务器之间互相调用用；不提供内部接口时填 `innerPort=0` |
| `status` | string | `online` 正常；`maintenance` 维护中（不再接收新的主播，已有的继续服务） |
| `version` | string | 程序版本 |
| `startTime` | number | 启动时间，毫秒 |
| `updateTime` | number | 最近一次刷新时间，毫秒 |
| `stats` | object | 各类服务自己的运行数据，见下表 |

`stats` 约定：

| 类型 | 字段 |
| --- | --- |
| logic | `anchors` 在线主播数，`rooms` 进行中的房间数（只用于展示） |
| login | 暂无 |
| callback | 暂无 |

字段只增不改：新增字段时读取方要能忽略不认识的字段，缺少字段按默认值处理。

## 4. 配置

每个服务在 `application.properties` 中配置：

```properties
zookeeper.address=127.0.0.1:2181
app.key=<管理后台项目的 App Key>
# logic 填区 ID，callback 填实例 ID；login 不配置
server.id=1
# 对外地址，客户端连接用
server.public-host=111.230.143.165
# 内网地址，服务器之间调用用
server.inner-host=10.0.0.8
```

ZooKeeper 连接使用 zfoo 自带的注册中心（`IRegistry`，Curator 实现）。

## 5. 注册规则（三类服务相同）

1. **启动**：端口监听成功后，再用 `addData(路径, JSON, EPHEMERAL)` 创建服务节点。端口没准备好之前不能注册。
2. **刷新**：每 10 秒调用一次 `addData` 更新节点内容（`updateTime`、`stats`、`status`）。`addData` 在节点不存在时会创建，所以与 ZooKeeper 断开、临时节点丢失后，下一次刷新会自动补回。
3. **进入维护**：把 `status` 改为 `maintenance` 并立即刷新一次。恢复时改回 `online`。
4. **正常停服**：先 `removeData` 删除自己的节点，再关闭对外服务。
5. **进程崩溃**：不需要处理，ZooKeeper 会话超时后临时节点自动删除。
6. **ID 冲突**（logic、callback）：启动时如果节点已存在且属于另一个会话（另一个进程正在使用同一个区 ID 或实例 ID），启动失败并打印错误，不能覆盖。login 的节点名是自己的地址，不存在冲突。

## 6. 发现规则

需要发现其他服务的一方调用 zfoo 的 `addListener(目录, 更新回调, 删除回调)`。监听是递归的，会收到目录下所有层级的节点变化。

1. 只处理服务节点（目录下第 1 层），目录本身忽略。
2. **更新回调**：解析 JSON，以路径最后一段（区 ID 或实例 ID）为键放入内存表。解析失败时打印错误并忽略该节点。
3. **删除回调**：从内存表中移除。
4. 业务只读内存表，不在请求中访问 ZooKeeper。
5. 与 ZooKeeper 断开期间继续使用内存表中的数据，恢复后由监听自动纠正。

## 7. 登录服找游戏服

登录服监听 `/danmu/<appKey>/logic`。

1. 确定主播要进入的区，目前固定为 1 区。
2. 在内存表中取该区的游戏服：
   - 不存在：返回业务失败「服务器维护中」；
   - `status=maintenance`：返回业务失败「服务器维护中」；
   - `status=online`：返回它的对外地址 `host` / `port`。
3. 登录票据中写入 `zoneId`。游戏服校验票据时核对 `zoneId` 与自己的区 ID 一致，不一致则拒绝登录。

原来手工维护的 `game_server` 表在本规则实现后不再使用。

## 8. 实现说明（登录服）

- `config.xml` 的 `<net:registry>` 开启 zfoo 注册中心。登录服不提供也不消费 zfoo 服务，zfoo 只连接 ZooKeeper。
- zfoo 在解析 `config.xml` 时从 Spring Environment 读取 `${zookeeper.address}`，因此 `Application` 启动时先把 `application.properties` 加入 Environment，再刷新容器。
- HTTP 端口启动成功后由 `ServiceDiscoveryService.start()` 开始监听并注册自己。
- 停服时 zfoo 先关闭 ZooKeeper 连接（临时节点随之删除），再关闭 HTTP 服务，满足「先删节点再关服务」。
- gz 上部署 ZooKeeper（Docker），端口和访问限制部署时确定。
