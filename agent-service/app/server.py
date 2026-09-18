from __future__ import annotations

import asyncio
import os
import sys

import uvicorn

if sys.platform == "win32":
    asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())


def main() -> None:
    """Run Uvicorn with the event loop required by psycopg on Windows."""

    uvicorn.run(
        "app.main:app",
        host=os.getenv("AI_HOST", "0.0.0.0"),
        port=int(os.getenv("AI_AGENT_PORT", "8100")),
        reload=os.getenv("AI_RELOAD", "false").lower() in {"1", "true", "yes", "on"},
    )


if __name__ == "__main__":
    main()
