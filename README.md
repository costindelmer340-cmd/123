# 20-第20组  电商售后客服与用户评价分析系统

目前只完成了极为简单的基本框架，正在进行初步开发
林世烨：接口和数据库
蔡宇：前端


本项目面向电商平台的售后服务、客服接待、工单处理和用户评价分析场景，建设一个包含消费者端、商家客服端、平台管理员端和 AI 智能服务的售后中台系统。

系统目标是作为商家统一处理外部平台订单、售后、客服、评价和知识库的工作台：支持店铺绑定、订单同步、售后申请、实时客服、工单流转、评价分析、知识库管理、规则配置和 AI 辅助回复。

## 技术栈

- 消费者微信小程序：微信小程序原生框架
- 商家客服后台：Vue 3、TypeScript、Element Plus
- 平台管理员后台：Vue 3、TypeScript、Element Plus
- 后端服务：Java、Spring Boot、MyBatis-Plus、Spring Security、JWT、Spring WebSocket/STOMP
- 外部平台接入：抖音电商开放平台 API，后续预留其他平台适配层
- 数据存储：MySQL 8.0、Redis
- AI 服务：Python、FastAPI
- 部署工具：Docker、Docker Compose、Nginx

## 项目结构

```text
ecommerce-after-sale-system/
├─ frontend/
│  ├─ consumer-miniapp/
│  ├─ merchant-web/
│  └─ admin-web/
├─ backend/
├─ ai-service/
├─ database/
├─ docs/
├─ deploy/
├─ README.md
└─ .gitignore
```

## 角色划分

- `CONSUMER`：消费者，使用微信小程序查询外部订单、发起售后、咨询和评价。
- `MERCHANT_ADMIN`：商家管理员，管理店铺授权、外部订单、售后和客服。
- `CUSTOMER_SERVICE`：商家客服，处理实时咨询、售后工单和服务记录。
- `PLATFORM_ADMIN`：平台管理员，管理平台用户、商家、店铺绑定、知识库、售后规则、评价分析和 AI 配置。

## 核心业务

1. 抖音店铺绑定与授权管理。
2. 外部订单同步、售后同步、评价同步和状态回写。
3. 售后闭环：退换货申请、资料补充、客服审核、退款/换货/维修处理、进度查看。
4. 客服闭环：实时会话、AI 辅助回复、转人工、会话记录、满意度评价。
5. 工单闭环：自动生成工单、AI 分类、客服处理、状态流转、操作日志。
6. 评价分析闭环：用户评价、情感分析、主题归类、商家和平台统计。
7. 知识库闭环：FAQ、售后规则、品牌口径、AI 回复依据管理。


