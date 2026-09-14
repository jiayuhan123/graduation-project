#!/usr/bin/env python3
"""Update the server .env for the containerized Java/FastAPI application.

The model API key is read from standard input so it never appears in the
process command line. Existing infrastructure credentials are preserved.
"""

from __future__ import annotations

import os
from pathlib import Path
import secrets
import shutil
import sys
from datetime import datetime


ENV_PATH = Path(sys.argv[1] if len(sys.argv) > 1 else ".env").resolve()


def new_secret() -> str:
    return secrets.token_urlsafe(48)


def read_entries(lines: list[str]) -> dict[str, str]:
    result: dict[str, str] = {}
    for line in lines:
        stripped = line.strip()
        if not stripped or stripped.startswith("#") or "=" not in stripped:
            continue
        key, value = stripped.split("=", 1)
        result[key.strip()] = value.strip().strip('"').strip("'")
    return result


def main() -> None:
    api_key = sys.stdin.read().strip()
    if not api_key.startswith("sk-") or len(api_key) < 32 or any(char.isspace() for char in api_key):
        raise SystemExit("Invalid model API key supplied on standard input")

    original_lines = ENV_PATH.read_text(encoding="utf-8").splitlines() if ENV_PATH.exists() else []
    current = read_entries(original_lines)

    def keep_or_create(name: str) -> str:
        existing = current.get(name, "")
        return existing if len(existing) >= 32 else new_secret()

    updates = {
        "APP_COMPOSE_PROJECT_NAME": "primary-math-platform-app",
        "INFRA_NETWORK_NAME": "primary-math-platform_local-net",
        "SERVER_PORT": "10000",
        "AI_AGENT_PORT": "8100",
        "INTERNAL_API_ENABLED": "true",
        "INTERNAL_API_TOKEN": keep_or_create("INTERNAL_API_TOKEN"),
        "AI_ENABLED": "true",
        "AI_AGENT_BRIDGE_TOKEN": keep_or_create("AI_AGENT_BRIDGE_TOKEN"),
        "AI_TOOL_SIGNING_SECRET": keep_or_create("AI_TOOL_SIGNING_SECRET"),
        "AI_MODEL_BASE_URL": "https://ws-jdiax7uwwausz7gi.cn-beijing.maas.aliyuncs.com/compatible-mode/v1",
        "AI_MODEL_API_KEY": api_key,
        "AI_MODEL_NAME": "qwen3.7-plus",
        "AI_EMBEDDING_MODEL": "qwen3.7-text-embedding",
        "AI_EMBEDDING_DIMENSIONS": "1024",
        "AI_EMBEDDING_BATCH_SIZE": "20",
        "AI_MODEL_TIMEOUT_SECONDS": "60",
        "AI_MAX_TOOL_ROUNDS": "4",
        "AI_MAX_CONCURRENT_RUNS": "12",
        "AI_DOCS_ENABLED": "false",
        "JAVA_OPTS": "-Xms256m -Xmx640m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError",
        # Non-empty placeholders let the demo start without an OSS account.
        # Replace these five values before demonstrating file uploads.
        "OSS_REGION": current.get("OSS_REGION") or "cn-beijing",
        "OSS_ENDPOINT": current.get("OSS_ENDPOINT") or "https://oss-cn-beijing.aliyuncs.com",
        "OSS_ACCESS_KEY_ID": current.get("OSS_ACCESS_KEY_ID") or "demo-disabled",
        "OSS_ACCESS_KEY_SECRET": current.get("OSS_ACCESS_KEY_SECRET") or "demo-disabled",
        "OSS_BUCKET": current.get("OSS_BUCKET") or "demo-disabled",
    }

    if ENV_PATH.exists():
        timestamp = datetime.now().strftime("%Y%m%d-%H%M%S")
        backup_path = ENV_PATH.with_name(f"{ENV_PATH.name}.backup-{timestamp}")
        shutil.copy2(ENV_PATH, backup_path)
        os.chmod(backup_path, 0o600)
    else:
        backup_path = None

    output_lines: list[str] = []
    replaced: set[str] = set()
    for line in original_lines:
        stripped = line.strip()
        if stripped and not stripped.startswith("#") and "=" in stripped:
            key = stripped.split("=", 1)[0].strip()
            if key in updates:
                output_lines.append(f"{key}={updates[key]}")
                replaced.add(key)
                continue
        output_lines.append(line)

    missing = [key for key in updates if key not in replaced]
    if missing:
        if output_lines and output_lines[-1].strip():
            output_lines.append("")
        output_lines.append("# Java + FastAPI AI application")
        output_lines.extend(f"{key}={updates[key]}" for key in missing)

    temp_path = ENV_PATH.with_name(f".{ENV_PATH.name}.tmp-{os.getpid()}")
    temp_path.write_text("\n".join(output_lines).rstrip() + "\n", encoding="utf-8")
    os.chmod(temp_path, 0o600)
    os.replace(temp_path, ENV_PATH)
    os.chmod(ENV_PATH, 0o600)

    print(f"Updated {len(updates)} application settings in {ENV_PATH}")
    if backup_path is not None:
        print(f"Backup created at {backup_path}")
    print("Secret values were not printed")


if __name__ == "__main__":
    main()
