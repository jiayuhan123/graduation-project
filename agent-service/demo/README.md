# 数学教学 Agent 核心骨架 Demo

这个目录用于学习当前项目的 **LangChain + LangGraph Agent 运转方式**，不是另一套业务实现，也不要求直接运行。

核心代码只有一个文件：

- [`teaching_agent_core.py`](./teaching_agent_core.py)：保留真实项目的 Agent 主链路，并用详细中文注释解释每一步。

## Demo 保留了哪些真实功能

它保留了项目 Agent 最重要的能力：

1. **教学知识库 RAG**：向量检索、MMR、LLM 相关性评分、无结果时查询改写。
2. **班级学情查询**：Agent 能读取薄弱知识点和班级趋势。
3. **固定题库搜索**：Agent 根据学情选择真实存在的题目。
4. **作业草稿生成**：Agent 只创建待教师确认的草稿，不直接发布。
5. **主 Agent 工具循环**：模型判断调用什么工具，工具结果再返回模型，直到形成最终回答。
6. **短期会话状态**：用 LangGraph checkpoint 和 `thread_id` 延续多轮会话。
7. **长期语义记忆时机**：运行前召回，成功回答后写回；具体数据库实现被省略。
8. **流式过程**：区分模型文本、工具事件和最终完整状态。

## Demo 删除了什么

为了让注意力集中在 Agent，本 Demo 删除或抽象掉了：

- FastAPI 路由和 SSE 字符串编码；
- 身份认证、动态权限和令牌；
- 限流、并发控制、监控、审计日志；
- 环境变量与 `Settings`；
- PostgreSQL 建表、迁移、连接池和 HNSW 建索引；
- Java HTTP 请求的具体实现；
- 完整异常处理和重试中间件。

这些代码在生产系统中很重要，但不决定 LangChain/LangGraph 的核心运行原理。

## 建议阅读顺序

按文件中的编号阅读：

1. `RagState`：先理解“状态”是图中所有节点共享的数据。
2. `KnowledgeService._build_rag_graph()`：看节点、固定边和条件边如何组成 RAG 图。
3. `_retrieve_node()`、`_grade_node()`、`_rewrite_node()`：看每个节点怎样读取状态并返回局部更新。
4. 四个 `@tool` 函数：看普通业务能力怎样变成模型可调用工具。
5. `AgentContext`：理解模型参数和服务端可信依赖为什么必须分开。
6. `build_primary_agent()`：理解 `create_agent()` 已经替你创建了模型—工具循环图。
7. `run_agent_once()`：看一轮请求怎样注入消息、`thread_id`、context，并消费流式事件。

## 两张图如何协作

```text
用户：根据班级薄弱点设计分层练习
                  │
                  ▼
┌──────────────── 主 Agent 图 ────────────────┐
│ model                                      │
│   ├─> get_class_dashboard                  │
│   ├─> retrieve_teaching_knowledge ─────────┼──┐
│   ├─> search_questions                     │  │
│   ├─> create_assignment_draft              │  │
│   └─> 汇总工具结果，形成回答                  │  │
└────────────────────────────────────────────┘  │
                                                ▼
                                  ┌──────── RAG 子图 ────────┐
                                  │ retrieve：MMR 检索       │
                                  │    ↓                     │
                                  │ grade：LLM 相关性评分     │
                                  │    ├─ 命中 → END          │
                                  │    └─ 未命中 → rewrite    │
                                  │                  ↓        │
                                  │              再次检索     │
                                  └───────────────────────────┘
```

RAG 图不是直接嵌在主图里的节点。它先被包装成 `retrieve_teaching_knowledge` 工具；主 Agent 决定调用该工具时，工具内部才执行 RAG 图。

## 对照真实源码

| Demo 位置 | 真实项目位置 | 对应作用 |
|---|---|---|
| `KnowledgeService` | `agent-service/app/rag.py` | Agentic RAG 子图 |
| 四个 `@tool` | `agent-service/app/tools.py` | 模型可调用的工具 |
| `AgentContext` | `agent-service/app/tools.py` | 向工具注入可信运行时依赖 |
| `build_primary_agent()` | `agent-service/app/agent.py` 的 `AgentFactory` | 编译主 Agent 图 |
| `run_agent_once()` | `agent-service/app/agent.py` 的 `TeachingAgent.run()` | 驱动图、消费流式事件 |
| `InMemorySaver` | `AsyncPostgresSaver` | 短期会话 checkpoint |
| `LongTermMemoryPort` | `agent-service/app/memory.py` + `AsyncPostgresStore` | 跨会话语义记忆 |

## 最应该抓住的三个概念

### 1. LangChain 提供“零件”

模型、Embedding、`Document`、工具装饰器、结构化输出、向量库适配器都来自 LangChain。

### 2. LangGraph 负责“状态与流程”

`StateGraph` 明确描述 RAG 节点怎样跳转；`create_agent()` 的返回值本身也是一个预构建的 LangGraph 图。

### 3. Agent 不是一次模型调用

一次真正的 Agent 运行可能是：

```text
模型思考
  → 调班级工具
  → 模型读取班级结果
  → 调 RAG 工具（内部再跑 RAG 图）
  → 模型读取资料
  → 调题库工具
  → 模型选择题目
  → 调草稿工具
  → 模型生成最终说明
```

模型负责决策，工具负责取得真实数据或执行受约束动作，LangGraph 负责保存状态并让整个循环可靠地继续。
