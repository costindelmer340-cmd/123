# AI 服务

AI 服务使用 Python + FastAPI 开发。当前阶段先提供轻量规则版 AI 能力，后续再接入专用模型训练和推理。

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
