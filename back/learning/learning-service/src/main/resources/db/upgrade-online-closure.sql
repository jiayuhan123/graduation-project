-- Existing learning_db upgrade for the online learning closure.
-- Safe to execute repeatedly after the original schema.sql.

BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS uk_practice_task_assignment_student
ON practice_task (student_id, source_type, source_id)
WHERE source_type = 'ASSIGNMENT' AND source_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS assignment_sync_event (
id              BIGINT       CONSTRAINT pk_assignment_sync_event PRIMARY KEY,
assignment_id   BIGINT       NOT NULL,
student_id      BIGINT       NOT NULL,
practice_task_id BIGINT      NOT NULL,
progress_status VARCHAR(16)  NOT NULL,
score           NUMERIC(8,2),
correct_count   INTEGER,
total_count     INTEGER,
started_at      TIMESTAMPTZ,
submitted_at    TIMESTAMPTZ,
graded_at       TIMESTAMPTZ,
delivery_status VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
retry_count     INTEGER      NOT NULL DEFAULT 0,
next_retry_at   TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
synced_at       TIMESTAMPTZ,
last_error      VARCHAR(1000),
created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_assignment_sync_event_progress
UNIQUE (practice_task_id, progress_status),
CONSTRAINT ck_assignment_sync_event_progress
CHECK (progress_status IN ('IN_PROGRESS', 'GRADED')),
CONSTRAINT ck_assignment_sync_event_delivery
CHECK (delivery_status IN ('PENDING', 'SENT')),
CONSTRAINT ck_assignment_sync_event_retry CHECK (retry_count >= 0)
);

CREATE INDEX IF NOT EXISTS idx_assignment_sync_event_pending
ON assignment_sync_event (next_retry_at, created_at, id)
WHERE delivery_status = 'PENDING';

CREATE INDEX IF NOT EXISTS idx_assignment_sync_event_sent
ON assignment_sync_event (synced_at)
WHERE delivery_status = 'SENT';

COMMENT ON TABLE assignment_sync_event IS '学习服务回写教学作业进度的事务发件箱';

COMMIT;
