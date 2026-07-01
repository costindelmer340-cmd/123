from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Ecommerce After-Sale AI Service"
    app_version: str = "1.0.0"

    model_provider: str = "deepseek"
    model_max_tokens: int = 800
    llm_timeout: int = 30

    deepseek_api_key: str = ""
    deepseek_base_url: str = "https://api.deepseek.com/v1"
    deepseek_model: str = "deepseek-chat"

    qwen_api_key: str = ""
    qwen_base_url: str = "https://dashscope.aliyuncs.com/compatible-mode/v1"
    qwen_model: str = "qwen-plus"

    glm_api_key: str = ""
    glm_base_url: str = "https://open.bigmodel.cn/api/paas/v4"
    glm_model: str = "glm-4"

    openai_api_key: str = ""
    openai_base_url: str = "https://api.openai.com/v1"
    openai_model: str = "gpt-4o-mini"

    db_host: str = "localhost"
    db_port: int = 3306
    db_user: str = "root"
    db_password: str = "cy20040910"
    db_name: str = "ecommerce_after_sale"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")


settings = Settings()
