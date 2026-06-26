# 部署说明

## 本地开发服务

计划中的本地端口如下：

- MySQL：`3306`
- Redis：`6379`
- Spring Boot 后端：`8080`
- FastAPI AI 服务：`8000`
- 商家客服后台：`5173`
- 平台管理员后台：`5175`

## 环境变量规划

后端：

```text
DB_URL=jdbc:mysql://localhost:3306/ecommerce_after_sale?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8
DB_USERNAME=root
DB_PASSWORD=你的MySQL密码
REDIS_HOST=localhost
REDIS_PORT=6379
APP_AUTH_JWT_SECRET=change-me-change-me-change-me-change-me
APP_AI_BASE_URL=http://localhost:9000
```

如果本机 MySQL 的 `root` 密码不是 `123456`，启动后端时需要指定 `DB_PASSWORD`。
Windows PowerShell 示例：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="你的MySQL密码"
java -jar target\mall-backend-0.0.1-SNAPSHOT.jar
```

抖音平台接入：

```text
DOUYIN_APP_KEY=
DOUYIN_APP_SECRET=
DOUYIN_REDIRECT_URI=http://localhost:8080/api/external/douyin/oauth/callback
DOUYIN_API_BASE_URL=
EXTERNAL_TOKEN_ENCRYPT_KEY=change-me
```

AI 服务：

```text
AI_PROVIDER=local
AI_MODEL_PATH=
AI_RULE_MODE=true
```

## Docker Compose 规划

后续阶段会在 `deploy/` 中补充：

- `docker-compose.yml`
- `nginx/nginx.conf`
- `mysql/init.sql`
- 后端 Dockerfile
- AI 服务 Dockerfile
- 前端构建与静态资源部署配置

## 生产化注意事项

- 不在 Git 中提交真实密钥、数据库密码、平台 app secret 和 token。
- 抖音平台 token 必须加密保存。
- 外部平台接口调用必须设置超时、重试和调用日志。
- 售后回写必须支持幂等、防重复提交和人工兜底。
- WebSocket 服务需要考虑鉴权和断线重连。
- MySQL 数据需要定期备份。
- Nginx 需要统一配置 HTTPS、静态资源缓存和接口反向代理。
