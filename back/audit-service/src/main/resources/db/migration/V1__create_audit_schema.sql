-- 审计日志由 MQ 消费异步写入，业务服务不跨库调用审计数据库。
CREATE TABLE IF NOT EXISTS audit_log (
    id          UUID         PRIMARY KEY,
    event_id    UUID         NOT NULL,
    operator_id BIGINT       NOT NULL,
    source      VARCHAR(64)  NOT NULL,
    action      VARCHAR(128) NOT NULL,
    target_type VARCHAR(64)  NOT NULL,
    target_id   VARCHAR(128) NOT NULL,
    detail      VARCHAR(1000),
    occurred_at TIMESTAMP    NOT NULL,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_audit_log_event UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_log_operator_occurred
    ON audit_log (operator_id, occurred_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_action_occurred
    ON audit_log (action, occurred_at DESC);
