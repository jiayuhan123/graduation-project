from app.storage import redact_sensitive_text, redact_trace_data


def test_trace_redaction_removes_tokens_and_student_identity() -> None:
    value = {
        "confirmationToken": "signed-action",
        "alerts": [{"studentId": 8, "displayName": "小明", "severity": "HIGH"}],
    }

    redacted = redact_trace_data(value)

    assert redacted["confirmationToken"] == "[REDACTED]"
    assert redacted["alerts"][0]["studentId"] == "[PERSONAL_DATA]"
    assert redacted["alerts"][0]["displayName"] == "[PERSONAL_DATA]"
    assert redacted["alerts"][0]["severity"] == "HIGH"


def test_sensitive_text_redaction_covers_common_credential_formats() -> None:
    value = "api_key=abc123456789 password: hunter2 Bearer abcdefghijklmnop sk-live_secret123"

    redacted = redact_sensitive_text(value)

    assert "abc123456789" not in redacted
    assert "hunter2" not in redacted
    assert "abcdefghijklmnop" not in redacted
    assert "sk-live_secret123" not in redacted
    assert redacted.count("[REDACTED]") == 4
