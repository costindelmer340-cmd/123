# 后端服务

后端使用 Java + Spring Boot 开发，包名为 `com.example.mall`。

计划集成：

- Spring Web
- Spring Validation
- Spring Security
- JWT
- MyBatis-Plus
- MySQL
- Redis
- Spring WebSocket + STOMP
- Swagger / OpenAPI

模块规划：

- `auth`：认证授权
- `user`：用户
- `merchant`：商家与店铺绑定
- `platform`：外部平台配置
- `sync`：同步任务与日志
- `order`：外部订单管理
- `aftersale`：售后
- `customer`：在线客服
- `ticket`：工单
- `review`：评价
- `knowledge`：知识库
- `ai`：AI 服务调用
- `statistics`：统计分析

首期优先接入抖音店铺，后续通过平台适配层扩展淘宝、京东、拼多多或自建商城。
