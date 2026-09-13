from __future__ import annotations

from prometheus_client import CollectorRegistry, Counter, Gauge, Histogram, generate_latest


class AgentMetrics:
    def __init__(self) -> None:
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
        return generate_latest(self.registry)
