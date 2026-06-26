# 接口规范

## 基础约定

- 后端接口统一前缀：`/api`
- 消费者端接口使用：`/api/consumer`
- 商家后台接口使用：`/api/merchant`
- 平台管理员接口使用：`/api/admin`
- 外部平台接入接口使用：`/api/external`
- AI 服务接口由后端封装调用，外部默认不直接暴露给前端。
- 请求和响应数据使用 JSON。
- 时间字段使用 ISO 8601 或 `yyyy-MM-dd HH:mm:ss`，后端统一处理时区。

## 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {},
  "traceId": "202606250001"
}
```

## 分页响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [],
    "pageNum": 1,
    "pageSize": 10,
    "total": 0,
    "pages": 0
  },
  "traceId": "202606250001"
}
```

## 常用状态码

- `200`：成功
- `400`：请求参数错误
- `401`：未登录或令牌无效
- `403`：无访问权限
- `404`：资源不存在
- `409`：业务状态冲突
- `500`：服务器内部错误
- `502`：外部平台接口调用失败

## 认证方式

登录成功后返回 JWT，前端请求业务接口时放入请求头：

```http
Authorization: Bearer <token>
```

## 角色权限

- `CONSUMER`：消费者端接口。
- `MERCHANT_ADMIN`：商家管理、店铺绑定、订单同步、售后审核接口。
- `CUSTOMER_SERVICE`：客服工作台、实时会话、工单处理接口。
- `PLATFORM_ADMIN`：平台管理、外部平台配置、同步监控、AI 配置接口。

## 外部平台接口约定

抖音接入接口由后端统一封装，不允许前端直接保存或传递平台密钥。

示例路径：

```text
GET  /api/merchant/platforms/douyin/auth-url
GET  /api/external/douyin/oauth/callback
POST /api/merchant/platforms/douyin/sync-orders
POST /api/merchant/platforms/douyin/sync-after-sales
POST /api/merchant/platforms/douyin/write-back-after-sale
GET  /api/admin/external-api-logs
```

外部平台调用必须记录：

- 平台编码
- 接口名称
- 业务类型
- 业务 ID
- 请求摘要
- 响应摘要
- 成功状态
- 错误信息
- 耗时

## WebSocket 约定

- 后端采用 Spring WebSocket + STOMP。
- 连接地址：`/ws`
- 消息发送目的地示例：`/app/chat.send`
- 用户订阅目的地示例：`/user/queue/messages`
- 会话订阅目的地示例：`/topic/conversations/{conversationId}`

## 命名约定

- URL 使用小写短横线，例如 `/api/merchant/after-sale-tickets`。
- JSON 字段使用 camelCase，例如 `orderId`、`createdAt`。
- 数据库字段使用 snake_case，例如 `order_id`、`created_at`。

