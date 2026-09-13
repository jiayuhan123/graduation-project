from __future__ import annotations

from typing import Any

TOOL_DEFINITIONS: dict[str, dict[str, Any]] = {
    "retrieve_teaching_knowledge": {
        "type": "function",
        "function": {
            "name": "retrieve_teaching_knowledge",
            "description": "从已入库教材、教案和教学规范中进行语义检索，返回可引用来源。",
            "parameters": {
                "type": "object",
                "properties": {
                    "query": {"type": "string", "minLength": 2, "maxLength": 500},
                    "topK": {"type": "integer", "minimum": 1, "maximum": 12},
                    "metadataFilter": {"type": "object"},
                },
                "required": ["query"],
                "additionalProperties": False,
            },
        },
    },
    "get_class_dashboard": {
        "type": "function",
        "function": {
            "name": "get_class_dashboard",
            "description": "读取当前教师有权查看的班级学情统计、薄弱知识点、趋势和学生预警。",
            "parameters": {
                "type": "object",
                "properties": {
                    "classId": {"type": "integer", "description": "班级 ID"},
                    "days": {
                        "type": "integer",
                        "minimum": 7,
                        "maximum": 90,
                        "default": 30,
                        "description": "统计最近多少天",
                    },
                },
                "required": ["classId"],
                "additionalProperties": False,
            },
        },
    },
    "search_questions": {
        "type": "function",
        "function": {
            "name": "search_questions",
            "description": "从已审核启用、具有标准答案的固定题库检索作业候选题。",
            "parameters": {
                "type": "object",
                "properties": {
                    "keyword": {"type": "string"},
                    "knowledgePointId": {"type": "integer"},
                    "questionType": {
                        "type": "string",
                        "enum": ["ARITHMETIC", "FILL_BLANK", "VERTICAL", "CHOICE", "JUDGEMENT"],
                    },
                    "difficulty": {"type": "integer", "minimum": 1, "maximum": 5},
                    "size": {"type": "integer", "minimum": 1, "maximum": 30, "default": 10},
                },
                "additionalProperties": False,
            },
        },
    },
    "create_assignment_draft": {
        "type": "function",
        "function": {
            "name": "create_assignment_draft",
            "description": (
                "生成一个等待教师确认的作业草稿。该工具不会写入作业；用户必须在界面中明确确认。"
            ),
            "parameters": {
                "type": "object",
                "properties": {
                    "classId": {"type": "integer"},
                    "title": {"type": "string", "maxLength": 128},
                    "description": {"type": "string", "maxLength": 1000},
                    "assignmentType": {"type": "string", "enum": ["UNIFIED", "LAYERED"]},
                    "startAt": {"type": "string", "format": "date-time"},
                    "dueAt": {"type": "string", "format": "date-time"},
                    "settings": {
                        "type": ["string", "null"],
                        "description": "可选的 JSON 字符串配置",
                    },
                    "questions": {
                        "type": "array",
                        "minItems": 1,
                        "maxItems": 100,
                        "items": {
                            "type": "object",
                            "properties": {
                                "questionId": {"type": "integer"},
                                "layerCode": {"type": "string"},
                                "sequenceNo": {"type": "integer", "minimum": 1},
                                "score": {"type": "number", "exclusiveMinimum": 0},
                            },
                            "required": ["questionId", "layerCode", "sequenceNo", "score"],
                            "additionalProperties": False,
                        },
                    },
                },
                "required": [
                    "classId",
                    "title",
                    "assignmentType",
                    "startAt",
                    "dueAt",
                    "questions",
                ],
                "additionalProperties": False,
            },
        },
    },
}


LOCAL_TOOLS = ("retrieve_teaching_knowledge",)


def tools_for(allowed_tools: list[str], include_local: bool = False) -> list[dict[str, Any]]:
    """Return only the tools authorized and signed by the Java service."""
    names = [name for name in allowed_tools if name in TOOL_DEFINITIONS and name not in LOCAL_TOOLS]
    if include_local:
        names = [*LOCAL_TOOLS, *names]
    return [TOOL_DEFINITIONS[name] for name in names]
