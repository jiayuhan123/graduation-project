-- V1.2: 题目错误反馈与处理后下架流程。
CREATE TABLE IF NOT EXISTS question_feedback (
id             BIGINT        CONSTRAINT pk_question_feedback PRIMARY KEY,
question_id    BIGINT        NOT NULL,
reporter_id    BIGINT        NOT NULL,
category       VARCHAR(24)   NOT NULL,
description    VARCHAR(1000) NOT NULL,
status         VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
handler_id     BIGINT,
handle_comment VARCHAR(1000),
handled_at     TIMESTAMPTZ,
created_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at     TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
version        INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT fk_question_feedback_question FOREIGN KEY (question_id) REFERENCES question (id),
CONSTRAINT ck_question_feedback_category CHECK
(category IN ('STEM_ERROR', 'ANSWER_ERROR', 'AMBIGUOUS', 'FORMAT_ERROR', 'OTHER')),
CONSTRAINT ck_question_feedback_status CHECK
(status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'REJECTED')),
CONSTRAINT ck_question_feedback_version CHECK (version >= 0),
CONSTRAINT ck_question_feedback_handled CHECK
((status IN ('PENDING', 'PROCESSING') AND handled_at IS NULL)
OR (status IN ('RESOLVED', 'REJECTED') AND handler_id IS NOT NULL AND handled_at IS NOT NULL))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_question_feedback_open_reporter
ON question_feedback (question_id, reporter_id)
WHERE status IN ('PENDING', 'PROCESSING');
CREATE INDEX IF NOT EXISTS idx_question_feedback_status_created
ON question_feedback (status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_question_feedback_question_created
ON question_feedback (question_id, created_at DESC);
