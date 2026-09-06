-- audit_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE audit_log (
id          UUID          CONSTRAINT pk_audit_log PRIMARY KEY,
event_id    UUID          NOT NULL,
operator_id BIGINT        NOT NULL,
source      VARCHAR(64)   NOT NULL,
action      VARCHAR(128)  NOT NULL,
target_type VARCHAR(64)   NOT NULL,
target_id   VARCHAR(128)  NOT NULL,
detail      VARCHAR(1000),
occurred_at TIMESTAMPTZ     NOT NULL,
created_at  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_audit_log_event UNIQUE (event_id)
);
CREATE INDEX idx_audit_log_operator_occurred
ON audit_log (operator_id, occurred_at DESC);
CREATE INDEX idx_audit_log_action_occurred
ON audit_log (action, occurred_at DESC);
CREATE INDEX idx_audit_log_target_occurred
ON audit_log (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_log_source_occurred
ON audit_log (source, occurred_at DESC);
COMMENT ON TABLE audit_log IS '跨服务不可变操作审计日志';
COMMENT ON COLUMN audit_log.id IS '审计记录编号';
COMMENT ON COLUMN audit_log.event_id IS '来源事件，保证消息消费幂等';
COMMENT ON COLUMN audit_log.operator_id IS '操作人编号；系统任务可约定为 0';
COMMENT ON COLUMN audit_log.source IS '来源微服务';
COMMENT ON COLUMN audit_log.action IS '操作编码';
COMMENT ON COLUMN audit_log.target_type IS '目标实体类型';
COMMENT ON COLUMN audit_log.target_id IS '目标业务编号';
COMMENT ON COLUMN audit_log.detail IS '操作摘要';
COMMENT ON COLUMN audit_log.occurred_at IS '业务实际发生时间';
COMMENT ON COLUMN audit_log.created_at IS '审计入库时间';
COMMENT ON COLUMN audit_log.updated_at IS '更新时间';
