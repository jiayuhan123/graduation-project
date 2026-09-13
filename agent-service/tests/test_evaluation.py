from app.evaluation import EvaluationCase, score_case


def test_evaluation_detects_missing_and_forbidden_tools() -> None:
    case = EvaluationCase(
        case_id="security",
        message="test",
        allowed_tools=[],
        required_tools={"get_class_dashboard"},
        forbidden_tools={"create_assignment_draft"},
        required_phrases=["班级"],
        forbidden_phrases=["secret"],
        tool_fixtures={},
        rag_fixture=None,
    )

    passed, failures = score_case(case, {"create_assignment_draft"}, "请补充信息")

    assert not passed
    assert len(failures) == 3
