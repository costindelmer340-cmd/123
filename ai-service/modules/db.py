"""
AI引擎 - 数据库连接模块
实现MySQL数据库连接和基本操作
"""
import aiomysql
from typing import Optional, List, Dict, Any
from datetime import datetime
from config import settings


class Database:
    """数据库连接池管理"""
    pool: Optional[aiomysql.Pool] = None

    async def init_pool(self):
        """初始化连接池"""
        if self.pool is None:
            self.pool = await aiomysql.create_pool(
                host=settings.MYSQL_HOST,
                port=settings.MYSQL_PORT,
                user=settings.MYSQL_USER,
                password=settings.MYSQL_PASSWORD,
                db=settings.MYSQL_DATABASE,
                charset=settings.MYSQL_CHARSET,
                autocommit=True,
                minsize=1,
                maxsize=10,
                loop=None
            )
        return self.pool

    async def execute(self, sql: str, params: tuple = ()) -> int:
        """执行SQL语句，返回影响行数"""
        pool = await self.init_pool()
        async with pool.acquire() as conn:
            await conn.execute("SET NAMES utf8mb4")
            await conn.execute("SET CHARACTER SET utf8mb4")
            async with conn.cursor() as cur:
                await cur.execute(sql, params)
                return cur.rowcount

    async def close_pool(self):
        """关闭连接池"""
        if self.pool:
            self.pool.close()
            await self.pool.wait_closed()
            self.pool = None

    async def execute(self, sql: str, params: tuple = ()) -> int:
        """执行SQL语句，返回影响行数"""
        pool = await self.init_pool()
        async with pool.acquire() as conn:
            async with conn.cursor() as cur:
                await cur.execute(sql, params)
                return cur.rowcount

    async def execute_many(self, sql: str, params_list: List[tuple]) -> int:
        """批量执行SQL语句"""
        pool = await self.init_pool()
        async with pool.acquire() as conn:
            async with conn.cursor() as cur:
                await cur.executemany(sql, params_list)
                return cur.rowcount

    async def fetch_one(self, sql: str, params: tuple = ()) -> Optional[Dict[str, Any]]:
        """查询单条记录"""
        pool = await self.init_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                await cur.execute(sql, params)
                return await cur.fetchone()

    async def fetch_all(self, sql: str, params: tuple = ()) -> List[Dict[str, Any]]:
        """查询多条记录"""
        pool = await self.init_pool()
        async with pool.acquire() as conn:
            async with conn.cursor(aiomysql.DictCursor) as cur:
                await cur.execute(sql, params)
                return await cur.fetchall()

    async def insert_and_get_id(self, sql: str, params: tuple = ()) -> int:
        """插入记录并返回自增ID"""
        pool = await self.init_pool()
        async with pool.acquire() as conn:
            async with conn.cursor() as cur:
                await cur.execute(sql, params)
                return cur.lastrowid


# 全局数据库实例
db = Database()


async def init_db():
    """初始化数据库连接"""
    await db.init_pool()
    print("MySQL database pool initialized")


async def close_db():
    """关闭数据库连接"""
    await db.close_pool()
    print("MySQL database pool closed")