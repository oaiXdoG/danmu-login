# 抖音直播数据推送任务

## 接口能力

直播数据按照 `roomid + appid + msg_type` 分别启动任务。登录服需要开启以下四种任务：

| `msg_type` | 数据类型 |
| --- | --- |
| `live_comment` | 评论 |
| `live_gift` | 礼物 |
| `live_like` | 点赞 |
| `live_fansclub` | 粉丝团 |

请求头统一携带应用级凭证：

```http
access-token: <应用级 access_token>
Content-Type: application/json
```

## 启动任务

官方文档：https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/live-room-scope/data-open/start-task

```http
POST https://webcast.bytedance.com/api/live_data/task/start
```

```json
{
  "roomid": "123456",
  "appid": "12312",
  "msg_type": "live_comment"
}
```

成功时 `err_no` 为 `0`，并返回 `data.task_id`。单个 `app_id` 调用上限为 10 次/秒。

## 停止任务

官方文档：https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/live-room-scope/data-open/stop-task

```http
POST https://webcast.bytedance.com/api/live_data/task/stop
```

请求体字段与启动任务一致，成功时 `err_no` 为 `0`。

## 查询任务状态

官方文档：https://developer.open-douyin.com/docs/resource/zh-CN/interaction/develop/server/live-room-scope/data-open/query-task-status

```http
GET https://webcast.bytedance.com/api/live_data/task/get?roomid=123456&appid=12312&msg_type=live_comment
```

状态值：

| `status` | 含义 |
| --- | --- |
| `1` | 任务不存在 |
| `2` | 任务未启动 |
| `3` | 任务运行中 |

## 登录流程规则

登录服取得有效 `room_id` 后立即依次开启四种推送任务。单个任务首次启动失败后再重试 3 次；仍失败时停止该房间的四种推送任务，并终止本次登录。
