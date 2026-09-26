"""本地开发启动入口，负责在导入 FastAPI 应用前准备运行环境。

Windows 默认 Proactor 事件循环与 psycopg 异步连接不兼容，因此本文件必须先切换 Selector
策略，再让 Uvicorn 导入 `app.main:app`。Linux 容器不需要这个入口，可直接执行 Uvicorn。
"""

from __future__ import annotations

import asyncio
import os
import sys
from pathlib import Path

import uvicorn
from dotenv import load_dotenv

PROJECT_ROOT = Path(__file__).resolve().parents[2]
# 读取仓库根目录的 .env；override=False 保证终端临时设置的环境变量拥有更高优先级。
load_dotenv(PROJECT_ROOT / ".env", override=False)

if sys.platform == "win32":
    # 必须发生在 FastAPI lifespan 创建 psycopg 连接池之前。
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())


def main() -> None:
    """按环境变量启动 Uvicorn，并加载 `app.main` 中的 ASGI 应用对象。"""

    uvicorn.run(
        "app.main:app",
        host=os.getenv("AI_HOST", "0.0.0.0"),
        port=int(os.getenv("AI_AGENT_PORT", "8100")),
        reload=os.getenv("AI_RELOAD", "false").lower() in {"1", "true", "yes", "on"},
    )


if __name__ == "__main__":
    # `python -m app.server` 时启动；被测试或其他模块导入时不会自动占用端口。
    main()
