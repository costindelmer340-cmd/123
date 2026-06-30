from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    APP_NAME: str = "Ecommerce After-Sale AI Engine"
    APP_VERSION: str = "1.0.0"
    
    MYSQL_HOST: str = "localhost"
    MYSQL_PORT: int = 3306
    MYSQL_USER: str = "root"
    MYSQL_PASSWORD: str = "cy20040910"
    MYSQL_DATABASE: str = "ecommerce_after_sale"
    MYSQL_CHARSET: str = "utf8mb4"
    
    LLM_API_KEY: str = ""
    LLM_API_BASE: str = ""
    MODEL_PROVIDER: str = "deepseek"
    DEEPSEEK_API_KEY: str = ""
    DEEPSEEK_BASE_URL: str = "https://api.deepseek.com/v1"
    DEEPSEEK_MODEL: str = "deepseek-chat"
    
    MODEL_TEMPERATURE: float = 0.7
    MODEL_MAX_TOKENS: int = 1024
    LLM_TIMEOUT: int = 30
    
    model_config = SettingsConfigDict(env_file=".env", extra="allow")

settings = Settings()
