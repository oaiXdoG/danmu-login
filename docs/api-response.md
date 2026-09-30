# HTTP 统一响应格式

所有 HTTP 接口都使用以下 JSON 外层结构：

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `code` | int | `0` 表示业务成功；业务失败使用项目自定义业务码；系统异常使用对应 HTTP 状态码 |
| `message` | String | 成功或失败的说明信息 |
| `data` | Object/null | 成功时的业务数据，失败时为 `null` |

## 状态规则

- 业务成功：HTTP 状态码为 `200`，`code` 为 `0`。
- 业务失败：HTTP 状态码仍为 `200`，`code` 使用自定义业务码，建议从 `100000` 开始分配。
- 系统或传输层异常：HTTP 状态码使用实际状态码，例如 `404`、`502`、`500`；响应体仍使用统一结构，`code` 等于 HTTP 状态码。
- 业务层通过 `BusinessException` 表达可预期的业务失败，由接口外层统一捕获并返回。

## 示例

业务成功：

```http
HTTP 200
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1
  }
}
```

业务失败：

```http
HTTP 200
```

```json
{
  "code": 100001,
  "message": "客户端版本过低，请更新至 1.1.0",
  "data": null
}
```

系统异常：

```http
HTTP 500
```

```json
{
  "code": 500,
  "message": "服务器内部错误",
  "data": null
}
```
