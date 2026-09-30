# 版本接口文档

## 1. 获取版本信息

- 请求方式：`POST`
- 请求地址：`/api/version`
- Content-Type：`application/json`

### 请求字段

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `clientVersion` | String | 是 | 客户端程序版本 |
| `resourceVersion` | String | 是 | 客户端资源版本 |

两个版本字段的格式均为三段数字：`主版本.次版本.修订版本`，例如 `1.0.0`。

### 请求示例

```json
{
  "clientVersion": "1.0.0",
  "resourceVersion": "1.2.0"
}
```

### 成功响应

HTTP 状态码为 `200`。

响应外层字段遵循 [HTTP 统一响应格式](api-response.md)，版本业务数据位于 `data`。

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "serverClientVersion": "1.1.0",
    "serverResourceVersion": "1.2.0",
    "needClientUpdate": true,
    "needResourceUpdate": false
  }
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `data.serverClientVersion` | String | 服务器要求的客户端程序版本 |
| `data.serverResourceVersion` | String | 服务器要求的客户端资源版本 |
| `data.needClientUpdate` | boolean | 是否需要更新客户端程序 |
| `data.needResourceUpdate` | boolean | 是否需要更新客户端资源 |

### 判断规则

- 客户端版本低于对应服务器版本时，更新字段返回 `true`。
- 客户端版本等于或高于对应服务器版本时，更新字段返回 `false`。
- 客户端程序版本与客户端资源版本分别比较，互不影响。

### 失败响应

版本参数为空或格式错误属于业务失败，HTTP 状态码仍为 `200`，例如：

```json
{
  "code": 100001,
  "message": "客户端版本不能为空",
  "data": null
}
```

系统或传输层异常使用实际 HTTP 状态码，响应体格式见 [HTTP 统一响应格式](api-response.md)。
