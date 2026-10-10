"""本地开发启动入口，负责在导入 FastAPI 应用前准备运行环境。

Windows 默认 Proactor 事件循环与 psycopg 异步连接不兼容，因此本文件既切换全局策略，
也向 Uvicorn 显式提供 Selector loop factory，再让它导入 `app.main:app`。Linux 容器不需要
这个入口，可直接执行 Uvicorn。
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


def selector_event_loop_factory() -> asyncio.AbstractEventLoop:
    """为新版 Uvicorn 显式创建 psycopg 在 Windows 上兼容的事件循环。"""

    return asyncio.SelectorEventLoop()


def main() -> None:
    """按环境变量启动 Uvicorn，并加载 `app.main` 中的 ASGI 应用对象。"""

    uvicorn.run(
        "app.main:app",
        host=os.getenv("AI_HOST", "0.0.0.0"),
        port=int(os.getenv("AI_AGENT_PORT", "8100")),
        reload=os.getenv("AI_RELOAD", "false").lower() in {"1", "true", "yes", "on"},
        # Uvicorn 0.52+ 在 Windows 单进程模式下会直接选择 ProactorEventLoop，忽略上面的
        # policy。传入可导入的 factory，确保普通启动和 reload 子进程都使用 Selector。
        loop="app.server:selector_event_loop_factory" if sys.platform == "win32" else "auto",
    )


if __name__ == "__main__":
    # `python -m app.server` 时启动；被测试或其他模块导入时不会自动占用端口。
    main()
