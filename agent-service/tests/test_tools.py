from app.tools import tools_for


def test_tools_for_only_returns_signed_allow_list() -> None:
    tools = tools_for(["get_class_dashboard", "unknown", "search_questions"])

    assert [tool["function"]["name"] for tool in tools] == [
        "get_class_dashboard",
        "search_questions",
    ]


def test_tools_for_includes_python_rag_tool_when_enabled() -> None:
    tools = tools_for(["get_class_dashboard"], include_local=True)

    assert [tool["function"]["name"] for tool in tools] == [
        "retrieve_teaching_knowledge",
        "get_class_dashboard",
    ]
