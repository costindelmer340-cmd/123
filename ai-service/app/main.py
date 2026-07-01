from fastapi import FastAPI, Response
from fastapi.middleware.cors import CORSMiddleware
from starlette.middleware.base import BaseHTTPMiddleware

from app.api.routes import router

app = FastAPI(
    title="After-sale AI Service",
    description="Rule-based AI service for after-sale intent, sentiment, topic, ticket and reply tasks.",
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

class UTF8Middleware(BaseHTTPMiddleware):
    async def dispatch(self, request, call_next):
        response = await call_next(request)
        response.headers["Content-Type"] = "application/json; charset=utf-8"
        return response

app.add_middleware(UTF8Middleware)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


app.include_router(router, prefix="/api/ai")
