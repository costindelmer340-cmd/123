from fastapi import FastAPI, HTTPException, Depends
from sqlalchemy.orm import Session
from fastapi.middleware.cors import CORSMiddleware
from app.config import settings
from app.db.database import engine, get_db
from app.modules.ai.router import router as ai_router
from app.modules.rule_engine.router import router as rule_engine_router
from app.modules.ticket.router import router as ticket_router
from app.common.response import APIResponse
from modules.llm_client import llm_client

app = FastAPI(title=settings.APP_NAME, version=settings.APP_VERSION)

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.middleware("http")
async def set_encoding_header(request, call_next):
    response = await call_next(request)
    response.headers["Content-Type"] = "application/json; charset=utf-8"
    return response

app.include_router(ai_router)
app.include_router(rule_engine_router)
app.include_router(ticket_router)

@app.get("/", response_model=APIResponse)
async def root():
    return APIResponse.success({
        "message": f"Welcome to {settings.APP_NAME} API", 
        "version": settings.APP_VERSION,
        "llm_configured": llm_client.is_configured()
    })

@app.get("/health", response_model=APIResponse)
async def health_check():
    return APIResponse.success({
        "status": "healthy", 
        "timestamp": __import__('datetime').datetime.now().isoformat(),
        "llm_configured": llm_client.is_configured()
    })

@app.exception_handler(HTTPException)
async def http_exception_handler(request, exc):
    return APIResponse.error(exc.status_code, exc.detail).dict()