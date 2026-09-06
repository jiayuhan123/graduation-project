-- Existing teaching_db upgrade: add the transactional outbox used by teaching audit events.
-- Manual execution only. Safe to execute repeatedly.

CREATE TABLE IF NOT EXISTS teaching_outbox_event (
id            UUID          CONSTRAINT pk_teaching_outbox_event PRIMARY KEY,
event_type    VARCHAR(128)  NOT NULL,
routing_key   VARCHAR(128)  NOT NULL,
payload       TEXT          NOT NULL,
status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
retry_count   INTEGER       NOT NULL DEFAULT 0,
next_retry_at TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
published_at  TIMESTAMPTZ,
last_error    VARCHAR(1000),
updated_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT ck_teaching_outbox_event_status
CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
CONSTRAINT ck_teaching_outbox_event_retry_count CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_teaching_outbox_event_pending
ON teaching_outbox_event (next_retry_at, created_at)
WHERE status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_teaching_outbox_event_published
ON teaching_outbox_event (published_at)
WHERE status = 'PUBLISHED';

COMMENT ON TABLE teaching_outbox_event IS '教学服务事务发件箱';
