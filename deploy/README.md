# 部署说明

本项目已提供本地联调用 Docker 部署骨架，包含 MySQL、Redis、Spring Boot 后端、FastAPI AI 服务、商家后台和管理员后台。消费者用户端只保留微信小程序。

## 文件说明

- `../docker-compose.yml`：统一编排入口。
- `../.env.example`：本地端口和 MySQL 密码示例。
- `nginx/spa.conf`：前端静态资源、后端 `/api` 和 WebSocket `/ws` 反向代理配置。
- `../backend/Dockerfile`：后端服务镜像构建。
- `../ai-service/Dockerfile`：AI 服务镜像构建。
- `../frontend/merchant-web/Dockerfile`：商家后台镜像构建。
- `../frontend/admin-web/Dockerfile`：管理员后台镜像构建。

## 启动步骤

在项目根目录执行：

```bash
copy .env.example .env
docker compose up -d --build
```

如果希望启动后自动检查关键服务，可以执行：

```powershell
.\scripts\dev-check.ps1 -StartCompose
```

如果暂时无法从 Docker Hub 拉取镜像，也可以用本机开发模式启动：

```powershell
.\scripts\dev-start.ps1
Start-Sleep -Seconds 8
.\scripts\dev-check.ps1
```

如果你的 MySQL root 密码不是 `123456`，可以这样传入：

```powershell
.\scripts\dev-start.ps1 -DbUsername root -DbPassword 你的MySQL密码
```

启动后访问：

- 商家客服后台：http://localhost:5173
- 平台管理员后台：http://localhost:5175
- 后端接口健康检查：http://localhost:8080/api/health
- Swagger 文档：http://localhost:8080/swagger-ui.html
- AI 服务健康检查：http://localhost:9000/health

## 数据库初始化

首次启动 MySQL 容器时会自动执行：

- `database/init.sql`
- `database/seed.sql`

如果已经启动过容器并生成了 `mysql-data` 卷，修改 SQL 后不会自动重新初始化。需要重建演示数据库时，可以执行：

```bash
docker compose down -v
docker compose up -d --build
```

这会删除容器卷中的 MySQL 数据，请只在演示或开发环境使用。

## 常用命令

```bash
docker compose ps
docker compose logs -f backend
docker compose logs -f ai-service
docker compose restart backend
docker compose down
```

也可以只执行联调检查：

```powershell
.\scripts\dev-check.ps1
```

停止本机开发模式服务：

```powershell
.\scripts\dev-stop.ps1
```

检查脚本会验证 Docker、端口、Compose 状态、后端健康检查、Swagger、AI 服务、三个 Web 前端、AI 情感分析样例和后端登录样例。如果后端健康检查正常但登录失败，通常说明 MySQL 账号密码、数据库初始化或后端 `DB_URL/DB_USERNAME/DB_PASSWORD` 配置存在问题。

## Docker Hub 拉取失败处理

如果启动时出现 `failed to resolve reference "docker.io/library/mysql:8.0"`、`registry-1.docker.io` 连接超时等错误，说明 Docker Desktop 当前无法访问 Docker Hub。可以按下面顺序处理：

1. 确认 Docker Desktop 已完全启动，并且 `docker version` 能显示 Server 信息。
2. 在 Docker Desktop 的 Settings 中配置可用代理或镜像源。
3. 先单独拉取基础镜像：

```bash
docker pull mysql:8.0
docker pull redis:7.2-alpine
docker pull maven:3.9-eclipse-temurin-17
docker pull eclipse-temurin:17-jre
docker pull python:3.12-slim
docker pull node:20-alpine
docker pull nginx:1.27-alpine
```

4. 如果目标环境完全离线，可以在能联网的机器上使用 `docker save` 导出镜像，再在本机使用 `docker load` 导入。

## 说明

当前部署配置面向本地开发和演示。生产环境还需要补充 HTTPS、域名、真实密钥管理、数据库备份、日志采集、监控告警和开放平台回调公网地址。
