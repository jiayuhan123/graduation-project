# 教学 Agent FastAPI 服务

FastAPI 只负责 HTTP/SSE 边界；Agent 编排、工具循环、Checkpoint 与 Agentic RAG 使用 LangChain/LangGraph。Java `monolith-app` 仍是业务数据与权限的唯一可信入口，负责动态工具授权、工具签名以及作业草稿的人工确认。

## 结构

```text
app/
├── main.py          # FastAPI 路由、依赖生命周期、就绪检查
├── agent.py         # LangChain create_agent 与 SSE 事件适配
├── tools.py         # @tool、参数模型、动态授权与工具审计
├── rag.py           # PGVectorStore 与 retrieve/grade/rewrite 子图
├── memory.py        # LangGraph PostgreSQL Checkpoint/Store
├── storage.py       # 业务元数据、运行轨迹和版本化迁移
├── clients.py       # ChatOpenAI、OpenAIEmbeddings、Java 工具客户端
└── migrations/      # Agent 自有数据库迁移
```

主链路如下：

```text
Java 身份与动态授权 → FastAPI → LangGraph Agent → LangChain Tools → Java 业务接口
                                      ├─ PostgreSQL Checkpoint
                                      ├─ PostgreSQL 长期语义记忆
                                      └─ Agentic RAG → PGVectorStore/pgvector
```

RAG 使用中文递归分块和 MMR：先召回与问题相似的候选，再降低候选之间的重复度。候选会经过模型相关性评估；没有有效资料时，子图最多改写问题一次并重新检索。返回结果保留 `[资料n]`、文档 ID、来源 URI 和片段 ID。

Redis 不再保存对话内容，只用于用户限流和并发控制。多轮短期上下文由 LangGraph Checkpoint 保存，长期记忆由 LangGraph Store 进行向量召回。

## 本地启动

需要 Python 3.12、带 pgvector 的 PostgreSQL、Redis，以及 OpenAI-compatible 的对话和 Embedding 接口。

```powershell
cd agent-service
python -m venv .venv
.\.venv\Scripts\python -m pip install --constraint requirements.lock -e ".[dev]"
$env:AI_RELOAD="true"
.\.venv\Scripts\python -m app.server
```

`app.server` 会自动读取仓库根目录 `.env`；当前进程中已设置的环境变量优先，便于临时覆盖配置。Windows 下请使用该入口，以保留 psycopg 所需的 Selector 事件循环策略。

关键配置：

- `AI_AGENT_BRIDGE_TOKEN`、`INTERNAL_API_TOKEN`：Java/Python 服务凭证；
- `AI_MODEL_BASE_URL`、`AI_MODEL_API_KEY`、`AI_MODEL_NAME`：模型配置；
- `AI_EMBEDDING_MODEL`、`AI_EMBEDDING_DIMENSIONS`：Embedding 配置；
- `AI_DATABASE_URL` 或 `AI_DB_*`：PostgreSQL/pgvector；
- `AI_REDIS_URL` 或 `AI_REDIS_*`：Redis；
- `JAVA_AI_TOOL_BASE_URL`：Java 内部工具地址；
- `AI_RAG_CHUNK_SIZE`、`AI_RAG_CHUNK_OVERLAP`：中文分块；
- `AI_RAG_TOP_K`、`AI_RAG_CANDIDATE_K`、`AI_RAG_MMR_LAMBDA`：MMR 检索；
- `AI_RAG_MAX_REWRITES`：无相关资料时的最大问题改写次数。

首次运行新版迁移会按项目决定直接删除旧的手写 RAG/记忆表和其中数据，再由 LangChain/LangGraph 创建新表；不会迁移或备份旧 AI 数据。业务表与 Agent 运行审计表不受影响。

## 验证

```powershell
.\.venv\Scripts\python -m ruff check app tests
.\.venv\Scripts\python -m ruff format --check app tests
.\.venv\Scripts\python -m pytest -q
curl.exe -fsS http://127.0.0.1:8100/health
curl.exe -fsS http://127.0.0.1:8100/ready
```

离线 Agent 回归仍可运行：

```powershell
.\.venv\Scripts\python -m app.evaluation evals/teaching-agent.jsonl --threshold 0.8
```

`/health` 只表示进程存活；`/ready` 会检查 PostgreSQL、Redis、模型、Embedding 与服务凭证。知识库、反馈、遗忘和质量接口只能由 Java 携带桥接令牌调用。
