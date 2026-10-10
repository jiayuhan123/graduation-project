"""数学教学 Agent 的核心骨架（学习版，不要求直接运行）。

这个文件不是另写一个无关的示例，而是从真实 ``agent-service`` 中抽取出的最短主链路：

1. LangChain 把普通 Python 函数包装成模型可调用的工具；
2. LangGraph ``StateGraph`` 编排 RAG 的“检索 -> 评分 -> 必要时改写”流程；
3. LangChain ``create_agent`` 创建主 Agent 图，让模型自己决定是否以及何时调用工具；
4. ``agent.astream`` 驱动主图并输出模型文本、工具事件和最终状态；
5. checkpoint 用 ``thread_id`` 保存同一会话的短期上下文。

为突出 Agent 的运转方式，本 Demo 故意省略 FastAPI、鉴权、限流、审计、Prometheus、
环境变量、数据库建表、异常重试等外围工程代码。业务接口和向量库也只保留抽象方法。

两张图的关系：

    主 Agent 图（create_agent）
        模型 -> 决定调用工具 -> 工具执行 -> 结果返回模型 -> 最终回答
                              |
                              +-- retrieve_teaching_knowledge 工具
                                      |
                                      v
                                  RAG 子图（StateGraph）
                                  retrieve -> grade -> rewrite? -> retrieve
"""

from __future__ import annotations

from collections.abc import AsyncIterator
from dataclasses import dataclass
from typing import Any, TypedDict

from langchain.agents import create_agent
from langchain.tools import ToolRuntime, tool
from langchain_core.documents import Document
from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, AIMessageChunk, HumanMessage
from langgraph.checkpoint.memory import InMemorySaver
from langgraph.graph import END, START, StateGraph
from pydantic import BaseModel, Field

# ---------------------------------------------------------------------------
# 1. 主 Agent 的系统规则
# ---------------------------------------------------------------------------

SYSTEM_PROMPT = """你是小学数学个性化练习平台的教学决策 Agent。

工作规则：
1. 数学知识、教学方法、教材依据优先调用 retrieve_teaching_knowledge。
2. 制定作业前先调用 get_class_dashboard 了解班级薄弱点。
3. 再调用 search_questions，从固定题库选择真实存在的题目。
4. 最后只能调用 create_assignment_draft 创建草稿，不能声称已经发布作业。
5. 工具没有返回的数据不能猜测；缺少参数时应询问用户。
6. 最终回答要区分业务数据、知识库依据和模型建议。
"""


# ---------------------------------------------------------------------------
# 2. 被省略的基础设施：这里只定义 Agent 真正依赖的能力
# ---------------------------------------------------------------------------


class VectorStorePort:
    """向量库最小接口。

    真实项目中这里是 LangChain ``PGVectorStore``。入库、建表、HNSW 索引等代码不影响
    Agent 如何运转，所以 Demo 只保留 RAG 节点真正调用的 MMR 检索方法。
    """

    async def amax_marginal_relevance_search_with_score_by_vector(
        self,
        embedding: list[float],
        *,
        k: int,
        fetch_k: int,
        lambda_mult: float,
    ) -> list[tuple[Document, float]]: ...


class EmbeddingsPort:
    """Embedding 模型最小接口；真实项目使用 ``OpenAIEmbeddings``。"""

    async def aembed_query(self, text: str) -> list[float]: ...


class BusinessGateway:
    """Java 业务服务的最小接口。

    HTTP、令牌和权限校验全部省略，只保留 Agent 能调用的三个业务能力。
    """

    async def get_class_dashboard(self, class_id: int, days: int) -> dict[str, Any]: ...

    async def search_questions(
        self,
        *,
        keyword: str | None,
        difficulty: int | None,
        size: int,
    ) -> dict[str, Any]: ...

    async def create_assignment_draft(
        self,
        *,
        class_id: int,
        title: str,
        questions: list[dict[str, Any]],
    ) -> dict[str, Any]: ...


class LongTermMemoryPort:
    """长期语义记忆的最小接口。

    真实项目使用 LangGraph ``AsyncPostgresStore`` 做向量召回。这里不展示存储细节，只让
    你看到长期记忆进入问题、成功回答再写回的时机。
    """

    async def recall(self, user_id: int, query: str, top_k: int) -> list[str]: ...

    async def remember(self, user_id: int, question: str, answer: str) -> None: ...


# ---------------------------------------------------------------------------
# 3. RAG 子图：显式使用 LangGraph StateGraph
# ---------------------------------------------------------------------------


class RelevanceDecision(BaseModel):
    """让大模型以稳定结构返回相关文档的编号，而不是自由文本。"""

    relevant_indices: list[int] = Field(
        default_factory=list,
        description="能直接支撑回答的候选资料编号，从 1 开始",
    )


class RewrittenQuery(BaseModel):
    """查询改写节点的结构化输出。"""

    query: str = Field(description="保持原意、但更适合知识库检索的问题")


class RagState(TypedDict, total=False):
    """RAG 图的共享状态。

    LangGraph 会在节点之间传递这份字典。每个节点只返回自己要更新的字段，LangGraph 再
    把更新合并回状态。这就是“图的状态”与普通函数参数传递最明显的区别。
    """

    query: str
    search_query: str
    top_k: int
    candidate_docs: list[tuple[Document, float]]
    relevant_docs: list[tuple[Document, float]]
    rewrite_count: int


class KnowledgeService:
    """把 Embedding、向量库和 RAG 子图组合成一个可被工具调用的服务。"""

    def __init__(
        self,
        model: BaseChatModel,
        embeddings: EmbeddingsPort,
        vector_store: VectorStorePort,
    ) -> None:
        self.embeddings = embeddings
        self.vector_store = vector_store

        # with_structured_output 会要求模型按照 Pydantic 结构返回结果。
        # 它仍然是一次 LLM 调用，但调用方拿到的是 RelevanceDecision/RewrittenQuery 对象。
        self.grader = model.with_structured_output(RelevanceDecision)
        self.rewriter = model.with_structured_output(RewrittenQuery)

        # 图通常在服务启动时编译一次，后续请求反复复用。
        self.graph = self._build_rag_graph()

    def _build_rag_graph(self) -> Any:
        """定义节点和边，然后把图编译成可 ``ainvoke`` 的对象。"""

        builder = StateGraph(RagState)

        # 节点名称只是图中的标识；第二个参数才是真正执行的 Python 函数。
        builder.add_node("retrieve", self._retrieve_node)
        builder.add_node("grade", self._grade_node)
        builder.add_node("rewrite", self._rewrite_node)

        # 固定边：图开始后一定先检索，然后一定评分。
        builder.add_edge(START, "retrieve")
        builder.add_edge("retrieve", "grade")

        # 条件边：评分之后由 route 函数决定结束，还是改写查询再检索。
        builder.add_conditional_edges(
            "grade",
            self._route_after_grade,
            {
                "rewrite": "rewrite",
                "done": END,
            },
        )
        builder.add_edge("rewrite", "retrieve")

        return builder.compile()

    async def retrieve(self, query: str, top_k: int = 6) -> dict[str, Any]:
        """RAG 对外入口：准备初始状态，然后完整运行一次 RAG 图。"""

        final_state = await self.graph.ainvoke(
            {
                "query": query,
                "search_query": query,
                "top_k": top_k,
                "rewrite_count": 0,
            }
        )

        # 图内部使用 Document 对象，对工具输出则转换为模型容易理解的字典。
        sources = [
            {
                "citation": f"资料{index}",
                "content": document.page_content,
                "title": document.metadata.get("title", "未命名资料"),
                "distance": distance,
            }
            for index, (document, distance) in enumerate(
                final_state.get("relevant_docs", []), start=1
            )
        ]
        return {
            "originalQuery": query,
            "searchQuery": final_state.get("search_query", query),
            "hitCount": len(sources),
            "sources": sources,
        }

    async def _retrieve_node(self, state: RagState) -> dict[str, Any]:
        """节点一：Embedding + MMR 向量检索。"""

        embedding = await self.embeddings.aembed_query(state["search_query"])
        docs = await self.vector_store.amax_marginal_relevance_search_with_score_by_vector(
            embedding,
            k=state["top_k"],  # 最终希望留下多少条
            fetch_k=24,  # 先从向量库取更大的候选池
            lambda_mult=0.7,  # 相关性与多样性的平衡系数
        )
        return {"candidate_docs": docs}

    async def _grade_node(self, state: RagState) -> dict[str, Any]:
        """节点二：让 LLM 判断候选片段能否真正支撑原问题。"""

        candidates = state.get("candidate_docs", [])
        if not candidates:
            return {"relevant_docs": []}

        candidate_text = "\n\n".join(
            f"[{index}] {document.page_content}"
            for index, (document, _distance) in enumerate(candidates, start=1)
        )
        decision: RelevanceDecision = await self.grader.ainvoke(
            [
                ("system", "只保留能够直接回答问题的资料，返回资料编号。"),
                ("human", f"原问题：{state['query']}\n\n候选资料：\n{candidate_text}"),
            ]
        )

        selected = {
            index - 1 for index in decision.relevant_indices if 1 <= index <= len(candidates)
        }
        return {
            "relevant_docs": [item for index, item in enumerate(candidates) if index in selected]
        }

    @staticmethod
    def _route_after_grade(state: RagState) -> str:
        """节点二之后的路由：命中则结束；未命中时最多改写一次。"""

        if state.get("relevant_docs"):
            return "done"
        if state.get("rewrite_count", 0) >= 1:
            return "done"
        return "rewrite"

    async def _rewrite_node(self, state: RagState) -> dict[str, Any]:
        """节点三：让 LLM 把原问题改写成更适合语义检索的表达。"""

        result: RewrittenQuery = await self.rewriter.ainvoke(
            [
                ("system", "保持原意，把问题改写成适合中文小学数学知识库检索的查询。"),
                ("human", state["query"]),
            ]
        )
        return {
            "search_query": result.query,
            "rewrite_count": state.get("rewrite_count", 0) + 1,
        }


# ---------------------------------------------------------------------------
# 4. LangChain 工具：把项目能力暴露给主 Agent 图
# ---------------------------------------------------------------------------


@dataclass(slots=True)
class AgentContext:
    """服务端注入的可信依赖，不由大模型生成。

    模型只能生成工具的业务参数，例如 ``classId``；它不能生成或替换 knowledge、business
    和 user_id。``ToolRuntime`` 会在工具执行时把这个 context 取出来。
    """

    user_id: int
    knowledge: KnowledgeService
    business: BusinessGateway


class RetrieveKnowledgeArgs(BaseModel):
    query: str = Field(min_length=2, max_length=500)
    topK: int = Field(default=6, ge=1, le=12)


@tool(
    "retrieve_teaching_knowledge",
    args_schema=RetrieveKnowledgeArgs,
    description="从教材、教案和教学规范中检索资料，并返回可引用的内容。",
)
async def retrieve_teaching_knowledge(
    query: str,
    runtime: ToolRuntime[AgentContext],
    topK: int = 6,
) -> dict[str, Any]:
    """主 Agent 调用这个工具时，内部会再运行一次 RAG 子图。"""

    runtime.stream_writer({"event": "tool_start", "toolName": "retrieve_teaching_knowledge"})
    result = await runtime.context.knowledge.retrieve(query, topK)
    runtime.stream_writer(
        {
            "event": "tool_result",
            "toolName": "retrieve_teaching_knowledge",
            "result": result,
        }
    )
    return result


@tool("get_class_dashboard", description="读取班级薄弱知识点、趋势和学生预警。")
async def get_class_dashboard(
    classId: int,
    runtime: ToolRuntime[AgentContext],
    days: int = 30,
) -> dict[str, Any]:
    """业务传输细节省略；工具只负责把模型参数转交给业务服务。"""

    return await runtime.context.business.get_class_dashboard(classId, days)


@tool("search_questions", description="从已审核固定题库中搜索作业候选题。")
async def search_questions(
    runtime: ToolRuntime[AgentContext],
    keyword: str | None = None,
    difficulty: int | None = None,
    size: int = 10,
) -> dict[str, Any]:
    """模型根据班级薄弱点组织筛选条件，工具返回真实题目。"""

    return await runtime.context.business.search_questions(
        keyword=keyword,
        difficulty=difficulty,
        size=size,
    )


class AssignmentQuestion(BaseModel):
    questionId: int
    layerCode: str
    sequenceNo: int
    score: float


@tool(
    "create_assignment_draft",
    description="根据已选固定题生成等待教师确认的作业草稿；不会直接发布。",
)
async def create_assignment_draft(
    classId: int,
    title: str,
    questions: list[AssignmentQuestion],
    runtime: ToolRuntime[AgentContext],
) -> dict[str, Any]:
    """这是 Agent 规划链的最后一步，但仍只是草稿，不是最终写操作。"""

    return await runtime.context.business.create_assignment_draft(
        class_id=classId,
        title=title,
        questions=[question.model_dump() for question in questions],
    )


ALL_TOOLS = [
    retrieve_teaching_knowledge,
    get_class_dashboard,
    search_questions,
    create_assignment_draft,
]


# ---------------------------------------------------------------------------
# 5. 主 Agent 图：create_agent 帮我们组装模型节点与工具节点
# ---------------------------------------------------------------------------


def build_primary_agent(model: BaseChatModel) -> Any:
    """创建主 Agent 图。

    ``create_agent`` 返回的不是一个简单聊天客户端，而是一个已经编译好的 LangGraph 图。
    它内部的核心循环可以抽象为：

        START
          -> model（读取消息并生成 AIMessage）
          -> 如果 AIMessage 含 tool_calls：tools（并行/顺序执行工具）
          -> 把 ToolMessage 追加到 messages
          -> 回到 model
          -> 如果没有 tool_calls：END

    所以项目不需要手写 ``while model_wants_tool``；LangGraph 负责状态传递和循环。
    """

    return create_agent(
        model=model,
        tools=ALL_TOOLS,
        system_prompt=SYSTEM_PROMPT,
        context_schema=AgentContext,
        # Demo 用内存 checkpoint。真实项目替换成 AsyncPostgresSaver，调用方式不变。
        checkpointer=InMemorySaver(),
        name="math_teaching_agent_demo",
    )


# ---------------------------------------------------------------------------
# 6. 驱动主图：一轮用户请求到底怎样流过 Agent
# ---------------------------------------------------------------------------


async def run_agent_once(
    *,
    agent: Any,
    context: AgentContext,
    memory: LongTermMemoryPort,
    conversation_id: str,
    user_message: str,
) -> AsyncIterator[dict[str, Any]]:
    """运行一轮 Agent，并把关键过程转换成便于前端消费的事件。

    真实项目外层会把这些字典编码成 SSE；Demo 直接 yield 字典，避免 HTTP 代码遮挡重点。
    """

    # 长期记忆不是主图的 checkpoint。它会跨会话按语义召回，再显式拼入当前问题。
    memories = await memory.recall(context.user_id, user_message, top_k=4)
    memory_text = "\n".join(f"- {item}" for item in memories)
    full_input = (
        f"以下历史记忆仅作参考：\n{memory_text}\n\n用户请求：{user_message}"
        if memories
        else user_message
    )

    # 同一个 thread_id 会读取并延续同一份 checkpoint；换 ID 就是新会话。
    config = {
        "configurable": {"thread_id": conversation_id},
        "recursion_limit": 30,
    }

    final_state: dict[str, Any] = {}
    streamed_text: list[str] = []

    async for event in agent.astream(
        {"messages": [HumanMessage(content=full_input)]},
        config=config,
        context=context,
        # custom：工具主动写出的事件；messages：模型 token；values：完整状态快照。
        stream_mode=["custom", "messages", "values"],
        version="v2",
    ):
        event_type = event.get("type")
        data = event.get("data")

        if event_type == "custom":
            # 来自工具的 runtime.stream_writer(...)。
            yield {"type": "tool_event", "data": data}

        elif event_type == "messages" and isinstance(data, tuple):
            message = data[0]
            if (
                isinstance(message, AIMessageChunk)
                and isinstance(message.content, str)
                and message.content
            ):
                streamed_text.append(message.content)
                yield {"type": "text_delta", "content": message.content}

        elif event_type == "values" and isinstance(data, dict):
            # values 会多次出现，始终保存最新状态，结束时它就是最终状态。
            final_state = data

    answer = _extract_final_answer(final_state) or "".join(streamed_text).strip()

    # 成功回答后再写长期记忆。真实项目使用 AsyncPostgresStore，并带 namespace/TTL。
    if answer:
        await memory.remember(context.user_id, user_message, answer)

    yield {"type": "done", "answer": answer}


def _extract_final_answer(state: dict[str, Any]) -> str:
    """从主图最终 messages 中找到最后一条真正的 AI 回答。"""

    for message in reversed(state.get("messages", [])):
        if (
            isinstance(message, AIMessage)
            and not message.tool_calls
            and isinstance(message.content, str)
        ):
            return message.content.strip()
    return ""


# ---------------------------------------------------------------------------
# 7. 组装顺序（伪代码）：真实对象的创建细节不属于本 Demo
# ---------------------------------------------------------------------------

"""
# ① 创建 LangChain 模型和 Embedding
model = ChatOpenAI(...)
embeddings = OpenAIEmbeddings(...)

# ② 创建 PGVectorStore、JavaBusinessGateway、AsyncPostgresStore
vector_store = PGVectorStore(...)
business = JavaBusinessGateway(...)
memory = PostgresSemanticMemory(...)

# ③ 编译 RAG 子图，再编译主 Agent 图
knowledge = KnowledgeService(model, embeddings, vector_store)
agent = build_primary_agent(model)

# ④ 每次请求构造可信 context，并流式运行主图
context = AgentContext(user_id=1001, knowledge=knowledge, business=business)
async for event in run_agent_once(
    agent=agent,
    context=context,
    memory=memory,
    conversation_id="conversation-001",
    user_message="根据三年级一班最近的薄弱点设计一份分层练习",
):
    print(event)
"""
