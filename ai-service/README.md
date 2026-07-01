# AI 服务

AI 服务使用 Python + FastAPI 开发。当前阶段保留意图识别、情感分析、主题归类等轻量能力；对话回复通过真实大模型服务生成，不再使用固定规则话术冒充 AI 回复。

## 当前接口

- `GET /health`
- `POST /api/ai/intent`：意图识别
- `POST /api/ai/sentiment`：情感分析
- `POST /api/ai/topic`：主题归类
- `POST /api/ai/ticket/classify`：工单分类
- `POST /api/ai/reply`：AI 辅助回复

## 启动方式

```bash
pip install -r requirements.txt
uvicorn app.main:app --reload --host 0.0.0.0 --port 9000
```

后续按售后语料训练专用小模型，优先覆盖意图识别、工单分类、情感分析和主题归类。回复生成层预留开源模型微调和 RAG 知识库检索能力。

## 大模型配置

默认使用 DeepSeek 兼容接口，可在 `ai-service/.env` 中配置：

```env
MODEL_PROVIDER=deepseek
DEEPSEEK_API_KEY=你的 API Key
DEEPSEEK_BASE_URL=https://api.deepseek.com/v1
DEEPSEEK_MODEL=deepseek-chat
```

也可切换 `MODEL_PROVIDER=qwen`、`glm` 或 `openai`，并配置对应的 API Key、Base URL 和模型名。未配置 API Key 时，`/api/ai/reply` 会返回“AI 服务暂不可用，建议转人工客服”，不会再返回旧规则回复。
