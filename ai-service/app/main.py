from fastapi import FastAPI

from app.api.routes import router

app = FastAPI(
    title="After-sale AI Service",
    description="Rule-based AI service for after-sale intent, sentiment, topic, ticket and reply tasks.",
    version="0.1.0",
)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "UP"}


app.include_router(router, prefix="/api/ai")
