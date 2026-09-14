# 教学 Agent FastAPI 服务

这是教学 Agent 的模型编排与 AI 数据面。Java `ai-service` 仍是唯一可信业务入口，负责登录身份、业务权限、工具签名和人工确认；FastAPI 负责多轮规划、RAG、记忆、运行轨迹、评测与流式输出。

## 已实现的生产闭环

- 多轮 Tool Calling：模型在最多 `AI_MAX_TOOL_ROUNDS` 轮内执行“规划—工具—观察—再规划”。
- 真实业务工具：班级学情、审核题库检索、带人工确认的作业草稿。
- RAG：文档切片、批量 Embedding、PostgreSQL/pgvector HNSW 检索、词面加权、最低相关度过滤和来源引用。
- 双层记忆：Redis 保存有 TTL 的短期上下文；PostgreSQL 保存按用户隔离、可语义召回且自动过期的长期情景记忆。
- 安全：服务间令牌、Java 侧动态工具白名单、HMAC 运行令牌、写操作二次确认、防重放、提示注入隔离、学生身份最小化和凭据文本脱敏。
- 治理：运行与工具轨迹、显式好评/差评、30 天质量指标、会话记忆删除、知识库软删除。
- 可靠性：模型与 Embedding 瞬时故障重试、Redis 用户限流、进程并发上限、依赖就绪探针和定期数据清理。
- 可观测性：`/metrics` 暴露运行、时延、工具、RAG、反馈和并发指标；Prometheus 已配置抓取。
- Grafana：仓库内置“教学 Agent 运行总览”，展示成功率、P95 时延、工具失败、RAG 命中和反馈趋势。
- 离线评测：`evals/teaching-agent.jsonl` 检查必需工具、禁止工具和答案约束，适合在模型升级前执行回归。

系统不会在线修改提示词或自动“学习”用户反馈。生产系统采用可审计的离线评测与人工发布流程，避免反馈投毒导致行为漂移。

## 本地启动

```powershell
cd agent-service
python -m venv .venv
.\.venv\Scripts\python -m pip install -e ".[dev]"
.\.venv\Scripts\python -m uvicorn app.main:app --reload --port 8100
```

核心配置：

- `AI_AGENT_BRIDGE_TOKEN`、`INTERNAL_API_TOKEN`：Java/Python 双向服务凭证；
- `AI_MODEL_BASE_URL`、`AI_MODEL_API_KEY`、`AI_MODEL_NAME`：OpenAI-compatible Chat Completions 接口；
- `AI_EMBEDDING_MODEL`、`AI_EMBEDDING_DIMENSIONS`：同一服务的 Embeddings 接口及固定向量维度；
- `AI_DATABASE_URL` 或 `AI_DB_*`：安装 pgvector 的 PostgreSQL 连接；
- `AI_REDIS_URL` 或 `AI_REDIS_*`：短期记忆和限流 Redis；
- `JAVA_AI_TOOL_BASE_URL`：Java 单体内部工具地址。

阿里云百炼北京地域推荐配置：`AI_MODEL_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1`、`AI_MODEL_NAME=qwen3.7-plus`、`AI_EMBEDDING_MODEL=qwen3.7-text-embedding`、`AI_EMBEDDING_DIMENSIONS=1024`、`AI_EMBEDDING_BATCH_SIZE=20`。向量维度必须在首次数据库迁移前确定，上线后不可直接修改。

首次启动会在 PostgreSQL advisory lock 下执行带校验和的版本化迁移。修改已执行迁移会让启动失败；后续结构变更必须新增 `Vxxx__*.sql`。
容器构建使用 `requirements.lock` 约束完整依赖树，更新依赖后应重新执行测试、Ruff 和离线评测再更新锁文件。

## 健康、指标与评测

```bash
curl -fsS http://127.0.0.1:8100/health
curl -fsS http://127.0.0.1:8100/ready
curl -fsS http://127.0.0.1:8100/metrics
python -m app.evaluation evals/teaching-agent.jsonl --threshold 0.8
```

`/health` 仅表示进程存活；`/ready` 会检查 PostgreSQL、Redis、对话模型和 Embedding 模型配置。知识库导入、反馈、遗忘和质量接口只允许 Java 携带桥接令牌调用，浏览器不能直连。

## 数据保留

- Redis 短期上下文默认保留 7 天且最多 20 条消息；
- 长期语义记忆默认保留 180 天；
- 运行、工具和反馈轨迹默认保留 90 天；
- 用户可在对话页清除当前会话的短期与长期记忆；
- 定时清理任务每 6 小时处理过期数据。

部署前应根据隐私政策调整保留期，知识库不得导入学生隐私、盗版教材或未授权内容。
