"""
电商售后客服AI引擎 - 配置文件
"""
from pydantic_settings import BaseSettings
from typing import Optional


class Settings(BaseSettings):
    """应用配置"""
    # 应用配置
    APP_NAME: str = "Ecommerce After-Sale AI Engine"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    
    # 服务器配置
    HOST: str = "0.0.0.0"
    PORT: int = 8003
    
    # AI模型配置
    MODEL_PROVIDER: str = "deepseek"  # deepseek, qwen, glm, openai
    
    # DeepSeek配置
    DEEPSEEK_API_KEY: Optional[str] = None
    DEEPSEEK_BASE_URL: str = "https://api.deepseek.com"
    DEEPSEEK_MODEL: str = "deepseek-chat"
    
    # Qwen配置
    QWEN_API_KEY: Optional[str] = None
    QWEN_BASE_URL: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    QWEN_MODEL: str = "qwen-max"
    
    # GLM配置
    GLM_API_KEY: Optional[str] = None
    GLM_BASE_URL: str = "https://open.bigmodel.cn/api/paas/v4"
    GLM_MODEL: str = "glm-4-flash"
    
    # OpenAI配置
    OPENAI_API_KEY: Optional[str] = None
    OPENAI_BASE_URL: str = "https://api.openai.com/v1"
    OPENAI_MODEL: str = "gpt-4o-mini"
    
    MODEL_TEMPERATURE: float = 0.7
    MODEL_MAX_TOKENS: int = 2000
    
    # RAG配置
    RAG_TOP_K: int = 5
    RAG_SIMILARITY_THRESHOLD: float = 0.7
    
    # Redis配置（用于会话管理）
    REDIS_HOST: str = "localhost"
    REDIS_PORT: int = 6379
    REDIS_DB: int = 0
    REDIS_PASSWORD: Optional[str] = None
    
    # 向量数据库配置
    ES_HOST: str = "localhost"
    ES_PORT: int = 9200
    
    # 熔断配置
    CIRCUIT_BREAKER_FAILURE_THRESHOLD: int = 5
    CIRCUIT_BREAKER_RECOVERY_TIMEOUT: int = 30
    
    # 超时配置（秒）
    LLM_TIMEOUT: int = 3
    RETRIEVAL_TIMEOUT: float = 0.5
    TOTAL_TIMEOUT: int = 5
    
    # 会话配置
    SESSION_EXPIRE_SECONDS: int = 3600
    MAX_CONTEXT_MESSAGES: int = 10
    
    # CORS配置
    CORS_ORIGINS: list = ["*"]

    # MySQL数据库配置
    MYSQL_HOST: str = "localhost"
    MYSQL_PORT: int = 3306
    MYSQL_USER: str = "root"
    MYSQL_PASSWORD: str = "cy20040910"
    MYSQL_DATABASE: str = "ecommerce_after_sale"
    MYSQL_CHARSET: str = "utf8mb4"

    class Config:
        env_file = ".env"
        case_sensitive = True


settings = Settings()
