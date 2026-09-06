-- notification_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE notification (
id              UUID          CONSTRAINT pk_notification PRIMARY KEY,
user_id         BIGINT        NOT NULL,
type            VARCHAR(32)   NOT NULL,
title           VARCHAR(128)  NOT NULL,
content         VARCHAR(2000) NOT NULL,
is_read            BOOLEAN       NOT NULL DEFAULT FALSE,
read_at         TIMESTAMPTZ,
source_event_id UUID          NOT NULL,
created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_notification_source_event UNIQUE (source_event_id),
CONSTRAINT ck_notification_read_time CHECK
((is_read = FALSE AND read_at IS NULL) OR
(is_read = TRUE AND read_at IS NOT NULL))
);
CREATE INDEX idx_notification_user_created
ON notification (user_id, created_at DESC);
CREATE INDEX idx_notification_user_unread
ON notification (user_id, created_at DESC) WHERE is_read = FALSE;
COMMENT ON TABLE notification IS '用户站内通知';
COMMENT ON COLUMN notification.id IS '通知编号';
COMMENT ON COLUMN notification.user_id IS '接收人';
COMMENT ON COLUMN notification.type IS '通知类型';
COMMENT ON COLUMN notification.title IS '标题';
COMMENT ON COLUMN notification.content IS '内容';
COMMENT ON COLUMN notification.is_read IS '是否已读';
COMMENT ON COLUMN notification.read_at IS '阅读时间';
COMMENT ON COLUMN notification.source_event_id IS '来源领域事件，保证消费幂等';
COMMENT ON COLUMN notification.created_at IS '创建时间';
COMMENT ON COLUMN notification.updated_at IS '更新时间';
