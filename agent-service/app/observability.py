"""定义教学 Agent 的 Prometheus 运行指标。

指标与数据库审计互补：Prometheus 适合实时监控、趋势和告警，PostgreSQL 审计适合追查
某一次具体运行及其工具调用。使用独立 Registry 可避免测试或重复导入时污染全局指标。
"""

from __future__ import annotations

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram, generate_latest


class AgentMetrics:
    """集中创建并持有服务使用的 Counter、Histogram 和 Gauge。"""

    def __init__(self) -> None:
        """注册运行、耗时、工具、RAG、反馈和在途请求指标。"""

        self.registry = CollectorRegistry()
        self.runs = Counter(
            "ai_agent_runs_total",
            "Agent runs by outcome",
            ["status", "role"],
            registry=self.registry,
        )
        self.run_duration = Histogram(
            "ai_agent_run_duration_seconds",
            "Agent run latency",
            ["status"],
            registry=self.registry,
        )
        self.tool_calls = Counter(
            "ai_agent_tool_calls_total",
            "Agent tool calls",
            ["tool", "status"],
            registry=self.registry,
        )
        self.rag_hits = Histogram(
            "ai_agent_rag_hits",
            "RAG result count per retrieval",
            registry=self.registry,
        )
        self.feedback = Counter(
            "ai_agent_feedback_total",
            "Explicit user feedback",
            ["rating"],
            registry=self.registry,
        )
        self.inflight = Gauge(
            "ai_agent_inflight_runs",
            "Currently running Agent requests",
            registry=self.registry,
        )

    def render(self) -> bytes:
        """按 Prometheus 文本暴露格式序列化当前 Registry。"""

        return generate_latest(self.registry)
