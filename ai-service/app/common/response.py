from typing import Any, Optional, Generic, TypeVar
from pydantic import BaseModel

T = TypeVar('T')

class APIResponse(BaseModel, Generic[T]):
    code: int = 200
    message: str = "success"
    data: Optional[T] = None
    
    @classmethod
    def success(cls, data: Optional[T] = None, message: str = "success") -> 'APIResponse[T]':
        return cls(code=200, message=message, data=data)
    
    @classmethod
    def error(cls, code: int, message: str) -> 'APIResponse[T]':
        return cls(code=code, message=message, data=None)
    
    @classmethod
    def not_found(cls, message: str = "资源不存在") -> 'APIResponse[T]':
        return cls(code=404, message=message, data=None)
    
    @classmethod
    def bad_request(cls, message: str = "请求参数错误") -> 'APIResponse[T]':
        return cls(code=400, message=message, data=None)
    
    @classmethod
    def unauthorized(cls, message: str = "未授权") -> 'APIResponse[T]':
        return cls(code=401, message=message, data=None)