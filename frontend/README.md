# 前端目录

本目录包含三个前端应用：

- `consumer-miniapp`：消费者端微信小程序，使用微信小程序原生框架。
- `merchant-web`：商家客服后台，使用 Vue 3 + TypeScript + Element Plus。
- `admin-web`：平台管理员后台，使用 Vue 3 + TypeScript + Element Plus。

三个前端共用后端接口和数据库，用户权限由后端 JWT 与角色体系控制。消费者用户端只保留微信小程序。
