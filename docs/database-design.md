# 数据库设计说明

数据库使用 MySQL 8.0。当前新版本定位为抖音优先的售后中台，因此数据库不再围绕自建购物链路设计，而是围绕外部平台店铺授权、订单快照、售后处理、客服工单、评价分析和 AI 能力设计。

## 设计原则

- 所有核心业务表保留 `id`、`created_at`、`updated_at`、`deleted` 字段。
- 涉及商家数据的表保留 `merchant_id`，用于商家后台数据隔离。
- 涉及外部平台数据的表保留 `platform_code`、`external_id`、`raw_data`、`last_synced_at` 等字段。
- 外部平台 token、密钥和授权信息必须加密保存。
- 关键状态字段使用字符串枚举，便于阅读和接口调试。
- 金额字段使用 `decimal(10,2)` 或更高精度，避免浮点误差。
- 关键操作、外部平台调用和售后回写必须写入日志，保证可追溯。

## 核心表规划

### 用户与权限

- `sys_user`：统一用户账号。
- `sys_role`：角色定义。
- `sys_user_role`：用户角色关联。
- `merchant`：商家主体。
- `merchant_staff`：商家员工和客服关系。

### 外部平台与店铺绑定

- `external_platform`：外部平台配置，首期为抖音。
- `external_shop_binding`：商家外部店铺绑定。
- `external_auth_token`：平台授权 token，加密保存。
- `external_api_call_log`：外部平台接口调用日志。

### 同步任务

- `sync_task`：同步任务定义，例如订单同步、售后同步、评价同步。
- `sync_log`：同步执行日志。
- `sync_cursor`：增量同步游标或最后同步时间。

### 外部订单快照

- `external_order`：外部订单主表快照。
- `external_order_item`：外部订单商品明细快照。
- `external_payment_snapshot`：外部支付状态快照。
- `external_logistics_snapshot`：外部物流状态快照。

### 售后与回写

- `after_sale_application`：售后申请。
- `after_sale_material`：售后凭证材料。
- `external_after_sale_mapping`：本系统售后单与抖音售后单映射。
- `refund_record`：退款记录或外部退款状态快照。
- `after_sale_write_back_log`：售后处理结果回写日志。

### 客服与工单

- `customer_conversation`：客服会话。
- `chat_message`：聊天消息。
- `quick_reply`：快捷回复。
- `service_evaluation`：客服满意度评价。
- `ticket`：工单。
- `ticket_record`：工单处理记录。
- `operation_log`：操作日志。

### 评价与分析

- `review`：外部平台评价和本系统服务评价。
- `review_append`：追评。
- `review_analysis`：评价情感与主题分析结果。

### 知识库与 AI

- `knowledge_article`：知识库文章。
- `faq_item`：FAQ。
- `after_sale_rule`：售后规则。
- `ai_config`：AI 模型和调用配置。
- `ai_call_log`：AI 调用日志。
- `ai_training_sample`：专用模型训练样本。
- `ai_model_version`：专用模型版本记录。

## 状态枚举初稿

### 外部平台

- `DOUYIN`：抖音电商。
- `TAOBAO`：淘宝，后续扩展。
- `JD`：京东，后续扩展。
- `PDD`：拼多多，后续扩展。

### 店铺授权状态

- `ACTIVE`：授权有效
- `EXPIRED`：授权过期
- `REVOKED`：授权被撤销
- `FAILED`：授权异常

### 同步状态

- `PENDING`：待同步
- `RUNNING`：同步中
- `SUCCESS`：同步成功
- `PARTIAL_SUCCESS`：部分成功
- `FAILED`：同步失败

### 外部订单状态

- `WAIT_PAY`：待支付
- `PAID`：已支付
- `WAIT_SHIP`：待发货
- `SHIPPED`：已发货
- `COMPLETED`：已完成
- `CANCELLED`：已取消
- `AFTER_SALE`：售后中

### 售后状态

- `PENDING_REVIEW`：待审核
- `APPROVED`：审核通过
- `REJECTED`：已拒绝
- `WAITING_USER_MATERIAL`：待用户补充资料
- `PROCESSING`：处理中
- `REFUNDING`：退款中
- `COMPLETED`：已完成
- `CANCELLED`：已撤销
- `WRITE_BACK_FAILED`：回写外部平台失败

### 工单状态

- `OPEN`：待处理
- `IN_PROGRESS`：处理中
- `WAITING_USER`：等待用户
- `RESOLVED`：已解决
- `CLOSED`：已关闭
- `REJECTED`：已驳回

### 会话状态

- `AI_SERVING`：AI 接待中
- `WAITING_AGENT`：等待人工
- `AGENT_SERVING`：人工接待中
- `CLOSED`：已关闭

## 当前说明

仓库中的 `database/init.sql` 和 `database/seed.sql` 已按本设计重构为抖音售后中台模型。
