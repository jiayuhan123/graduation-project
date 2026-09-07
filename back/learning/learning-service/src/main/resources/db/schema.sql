-- learning_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE practice_task (
id                 BIGINT        CONSTRAINT pk_practice_task PRIMARY KEY,
student_id         BIGINT        NOT NULL,
source_type        VARCHAR(24)   NOT NULL,
source_id          VARCHAR(64),
title              VARCHAR(128)  NOT NULL,
mode               VARCHAR(24)   NOT NULL DEFAULT 'NORMAL',
status             VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
question_count     SMALLINT      NOT NULL,
time_limit_seconds INTEGER,
target_accuracy    NUMERIC(5,4),
started_at         TIMESTAMPTZ,
completed_at       TIMESTAMPTZ,
expires_at         TIMESTAMPTZ,
created_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version            INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT ck_practice_task_source CHECK (source_type IN
('SELF', 'RECOMMENDATION', 'ASSIGNMENT', 'GAME_REMEDIAL', 'PHOTO')),
CONSTRAINT ck_practice_task_mode CHECK (mode IN
('NORMAL', 'TIMED', 'SPECIAL', 'REVIEW')),
CONSTRAINT ck_practice_task_status CHECK (status IN
('PENDING', 'IN_PROGRESS', 'COMPLETED', 'EXPIRED', 'CANCELLED')),
CONSTRAINT ck_practice_task_question_count CHECK (question_count > 0),
CONSTRAINT ck_practice_task_time_limit CHECK
(time_limit_seconds IS NULL OR time_limit_seconds > 0),
CONSTRAINT ck_practice_task_accuracy CHECK
(target_accuracy IS NULL OR (target_accuracy > 0 AND target_accuracy <= 1)),
CONSTRAINT ck_practice_task_version CHECK (version >= 0)
);
CREATE INDEX idx_practice_task_student_created
ON practice_task (student_id, created_at DESC);
CREATE INDEX idx_practice_task_student_status
ON practice_task (student_id, status, expires_at);
CREATE INDEX idx_practice_task_source
ON practice_task (source_type, source_id) WHERE source_id IS NOT NULL;
CREATE UNIQUE INDEX uk_practice_task_assignment_student
ON practice_task (student_id, source_type, source_id)
WHERE source_type = 'ASSIGNMENT' AND source_id IS NOT NULL;
COMMENT ON TABLE practice_task IS '所有学习入口统一使用的练习任务';
COMMENT ON COLUMN practice_task.id IS '练习任务编号';
COMMENT ON COLUMN practice_task.student_id IS '完成练习的学生';
COMMENT ON COLUMN practice_task.source_type IS '任务来源';
COMMENT ON COLUMN practice_task.source_id IS '来源记录编号';
COMMENT ON COLUMN practice_task.title IS '练习标题';
COMMENT ON COLUMN practice_task.mode IS '普通、限时、专项或复习';
COMMENT ON COLUMN practice_task.status IS '任务状态';
COMMENT ON COLUMN practice_task.question_count IS '题目数量快照';
COMMENT ON COLUMN practice_task.time_limit_seconds IS '总答题时限，普通练习可为空';
COMMENT ON COLUMN practice_task.target_accuracy IS '目标正确率';
COMMENT ON COLUMN practice_task.started_at IS '开始时间';
COMMENT ON COLUMN practice_task.completed_at IS '完成时间';
COMMENT ON COLUMN practice_task.expires_at IS '过期时间';
COMMENT ON COLUMN practice_task.created_at IS '创建时间';
COMMENT ON COLUMN practice_task.updated_at IS '更新时间';
COMMENT ON COLUMN practice_task.version IS '乐观锁版本号';

CREATE TABLE practice_task_knowledge_point (
task_id               BIGINT    NOT NULL,
knowledge_point_id    BIGINT    NOT NULL,
target_question_count SMALLINT  NOT NULL DEFAULT 1,
created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_practice_task_knowledge_point PRIMARY KEY (task_id, knowledge_point_id),
CONSTRAINT ck_practice_task_kp_count CHECK (target_question_count > 0)
);
CREATE INDEX idx_practice_task_kp_knowledge
ON practice_task_knowledge_point (knowledge_point_id, task_id);
COMMENT ON TABLE practice_task_knowledge_point IS '练习任务目标知识点';
COMMENT ON COLUMN practice_task_knowledge_point.task_id IS '练习任务编号';
COMMENT ON COLUMN practice_task_knowledge_point.knowledge_point_id IS '知识点编号';
COMMENT ON COLUMN practice_task_knowledge_point.target_question_count IS '计划安排题数';
COMMENT ON COLUMN practice_task_knowledge_point.created_at IS '创建时间';
COMMENT ON COLUMN practice_task_knowledge_point.updated_at IS '更新时间';

CREATE TABLE practice_task_question (
id                BIGINT        CONSTRAINT pk_practice_task_question PRIMARY KEY,
task_id           BIGINT        NOT NULL,
question_id       BIGINT,
sequence_no       SMALLINT      NOT NULL,
question_snapshot JSONB         NOT NULL,
max_score         NUMERIC(8,2)  NOT NULL DEFAULT 1.00,
source_detail     JSONB,
created_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_practice_task_question_seq UNIQUE (task_id, sequence_no),
CONSTRAINT ck_practice_task_question_seq CHECK (sequence_no > 0),
CONSTRAINT ck_practice_task_question_score CHECK (max_score > 0)
);
CREATE INDEX idx_practice_task_question_question
ON practice_task_question (question_id) WHERE question_id IS NOT NULL;
COMMENT ON TABLE practice_task_question IS '练习任务题目与不可变快照';
COMMENT ON COLUMN practice_task_question.id IS '任务题目明细编号';
COMMENT ON COLUMN practice_task_question.task_id IS '练习任务编号';
COMMENT ON COLUMN practice_task_question.question_id IS '固定题或模板题编号';
COMMENT ON COLUMN practice_task_question.sequence_no IS '题目顺序';
COMMENT ON COLUMN practice_task_question.question_snapshot IS '当时题干、答案、难度、知识点等快照';
COMMENT ON COLUMN practice_task_question.max_score IS '满分';
COMMENT ON COLUMN practice_task_question.source_detail IS '生成参数或选题原因';
COMMENT ON COLUMN practice_task_question.created_at IS '创建时间';
COMMENT ON COLUMN practice_task_question.updated_at IS '更新时间';

CREATE TABLE answer_record (
id                     BIGINT        CONSTRAINT pk_answer_record PRIMARY KEY,
task_question_id       BIGINT        NOT NULL,
student_id             BIGINT        NOT NULL,
attempt_no             SMALLINT      NOT NULL DEFAULT 1,
student_answer         JSONB         NOT NULL,
grading_status         VARCHAR(24)   NOT NULL DEFAULT 'PENDING',
is_correct             BOOLEAN,
score                  NUMERIC(8,2),
response_ms            INTEGER,
answer_source          VARCHAR(16)   NOT NULL DEFAULT 'MANUAL',
recognition_confidence NUMERIC(5,4),
submitted_at           TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
graded_at              TIMESTAMPTZ,
created_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_answer_record_attempt
UNIQUE (task_question_id, student_id, attempt_no),
CONSTRAINT ck_answer_record_attempt CHECK (attempt_no > 0),
CONSTRAINT ck_answer_record_grading_status CHECK (grading_status IN
('PENDING', 'GRADED', 'REVIEW_REQUIRED', 'INVALID')),
CONSTRAINT ck_answer_record_score CHECK (score IS NULL OR score >= 0),
CONSTRAINT ck_answer_record_response CHECK (response_ms IS NULL OR response_ms >= 0),
CONSTRAINT ck_answer_record_source CHECK (answer_source IN ('MANUAL', 'OCR', 'GAME')),
CONSTRAINT ck_answer_record_confidence CHECK
(recognition_confidence IS NULL OR
(recognition_confidence >= 0 AND recognition_confidence <= 1))
);
CREATE INDEX idx_answer_record_student_submitted
ON answer_record (student_id, submitted_at DESC);
CREATE INDEX idx_answer_record_review_required
ON answer_record (submitted_at)
WHERE grading_status = 'REVIEW_REQUIRED';
COMMENT ON TABLE answer_record IS '全系统统一答题事实';
COMMENT ON COLUMN answer_record.id IS '答题记录编号';
COMMENT ON COLUMN answer_record.task_question_id IS '对应任务题目';
COMMENT ON COLUMN answer_record.student_id IS '学生编号';
COMMENT ON COLUMN answer_record.attempt_no IS '本题第几次尝试';
COMMENT ON COLUMN answer_record.student_answer IS '学生答案或计算步骤';
COMMENT ON COLUMN answer_record.grading_status IS '批改状态';
COMMENT ON COLUMN answer_record.is_correct IS '是否正确；待批改时为空';
COMMENT ON COLUMN answer_record.score IS '实际得分';
COMMENT ON COLUMN answer_record.response_ms IS '作答耗时毫秒';
COMMENT ON COLUMN answer_record.answer_source IS '答案来源';
COMMENT ON COLUMN answer_record.recognition_confidence IS 'OCR 识别可信度';
COMMENT ON COLUMN answer_record.submitted_at IS '提交时间';
COMMENT ON COLUMN answer_record.graded_at IS '批改完成时间';
COMMENT ON COLUMN answer_record.created_at IS '入库时间';
COMMENT ON COLUMN answer_record.updated_at IS '更新时间';

CREATE TABLE error_record (
id                    BIGINT        CONSTRAINT pk_error_record PRIMARY KEY,
answer_record_id      BIGINT        NOT NULL,
student_id            BIGINT        NOT NULL,
question_id           BIGINT,
knowledge_point_id    BIGINT        NOT NULL,
error_type_id         BIGINT        NOT NULL,
error_position        VARCHAR(255),
diagnosis             TEXT,
confidence            NUMERIC(5,4),
status                VARCHAR(16)   NOT NULL DEFAULT 'OPEN',
resolved_by_answer_id BIGINT,
created_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
resolved_at           TIMESTAMPTZ,
CONSTRAINT ck_error_record_not_self CHECK
(resolved_by_answer_id IS NULL OR resolved_by_answer_id <> answer_record_id),
CONSTRAINT ck_error_record_confidence CHECK
(confidence IS NULL OR (confidence >= 0 AND confidence <= 1)),
CONSTRAINT ck_error_record_status CHECK
(status IN ('OPEN', 'RETRAINING', 'RESOLVED'))
);
CREATE INDEX idx_error_record_student_status
ON error_record (student_id, status, created_at DESC);
CREATE INDEX idx_error_record_kp_status
ON error_record (knowledge_point_id, status, created_at DESC);
CREATE INDEX idx_error_record_type
ON error_record (error_type_id, created_at DESC);
CREATE INDEX idx_error_record_answer_record_id
ON error_record (answer_record_id);
CREATE INDEX idx_error_record_resolved_by_answer_id
ON error_record (resolved_by_answer_id);
COMMENT ON TABLE error_record IS '由错误作答诊断出的具体错因';
COMMENT ON COLUMN error_record.id IS '错因记录编号';
COMMENT ON COLUMN error_record.answer_record_id IS '原错误作答';
COMMENT ON COLUMN error_record.student_id IS '学生编号';
COMMENT ON COLUMN error_record.question_id IS '原题编号；动态题可为空';
COMMENT ON COLUMN error_record.knowledge_point_id IS '对应知识点';
COMMENT ON COLUMN error_record.error_type_id IS '错因类型编号';
COMMENT ON COLUMN error_record.error_position IS '错误步骤、数位或区域';
COMMENT ON COLUMN error_record.diagnosis IS '本次具体诊断说明';
COMMENT ON COLUMN error_record.confidence IS '诊断可信度';
COMMENT ON COLUMN error_record.status IS '处理状态';
COMMENT ON COLUMN error_record.resolved_by_answer_id IS '验证解决该错因的作答';
COMMENT ON COLUMN error_record.created_at IS '发现时间';
COMMENT ON COLUMN error_record.updated_at IS '更新时间';
COMMENT ON COLUMN error_record.resolved_at IS '解决时间';

CREATE TABLE mastery_record (
id                        BIGINT        CONSTRAINT pk_mastery_record PRIMARY KEY,
student_id                BIGINT        NOT NULL,
knowledge_point_id        BIGINT        NOT NULL,
mastery_score             NUMERIC(5,4)  NOT NULL DEFAULT 0.0000,
accuracy_rate             NUMERIC(5,4)  NOT NULL DEFAULT 0.0000,
avg_response_ms           INTEGER,
total_answer_count        INTEGER       NOT NULL DEFAULT 0,
correct_answer_count      INTEGER       NOT NULL DEFAULT 0,
consecutive_correct_count INTEGER       NOT NULL DEFAULT 0,
last_practiced_at         TIMESTAMPTZ,
next_review_at            TIMESTAMPTZ,
created_at                TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version                   INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_mastery_student_knowledge UNIQUE (student_id, knowledge_point_id),
CONSTRAINT ck_mastery_score CHECK (mastery_score BETWEEN 0 AND 1),
CONSTRAINT ck_mastery_accuracy CHECK (accuracy_rate BETWEEN 0 AND 1),
CONSTRAINT ck_mastery_avg_response CHECK
(avg_response_ms IS NULL OR avg_response_ms >= 0),
CONSTRAINT ck_mastery_counts CHECK
(total_answer_count >= 0 AND correct_answer_count >= 0 AND
correct_answer_count <= total_answer_count AND
consecutive_correct_count >= 0),
CONSTRAINT ck_mastery_version CHECK (version >= 0)
);
CREATE INDEX idx_mastery_student_score
ON mastery_record (student_id, mastery_score, updated_at DESC);
CREATE INDEX idx_mastery_next_review
ON mastery_record (next_review_at)
WHERE next_review_at IS NOT NULL;
COMMENT ON TABLE mastery_record IS '学生知识点当前掌握快照';
COMMENT ON COLUMN mastery_record.id IS '掌握记录编号';
COMMENT ON COLUMN mastery_record.student_id IS '学生编号';
COMMENT ON COLUMN mastery_record.knowledge_point_id IS '知识点编号';
COMMENT ON COLUMN mastery_record.mastery_score IS '综合掌握程度';
COMMENT ON COLUMN mastery_record.accuracy_rate IS '累计正确率';
COMMENT ON COLUMN mastery_record.avg_response_ms IS '平均作答耗时';
COMMENT ON COLUMN mastery_record.total_answer_count IS '累计答题数';
COMMENT ON COLUMN mastery_record.correct_answer_count IS '累计答对数';
COMMENT ON COLUMN mastery_record.consecutive_correct_count IS '当前连续答对数';
COMMENT ON COLUMN mastery_record.last_practiced_at IS '最近练习时间';
COMMENT ON COLUMN mastery_record.next_review_at IS '建议复习时间';
COMMENT ON COLUMN mastery_record.created_at IS '创建时间';
COMMENT ON COLUMN mastery_record.updated_at IS '更新时间';
COMMENT ON COLUMN mastery_record.version IS '乐观锁版本号';

CREATE TABLE mastery_change_log (
id               BIGINT        CONSTRAINT pk_mastery_change_log PRIMARY KEY,
mastery_id       BIGINT        NOT NULL,
answer_record_id BIGINT,
before_score     NUMERIC(5,4)  NOT NULL,
after_score      NUMERIC(5,4)  NOT NULL,
delta_score      NUMERIC(6,4)  NOT NULL,
reason           VARCHAR(16)   NOT NULL,
occurred_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT ck_mastery_change_before CHECK (before_score BETWEEN 0 AND 1),
CONSTRAINT ck_mastery_change_after CHECK (after_score BETWEEN 0 AND 1),
CONSTRAINT ck_mastery_change_delta CHECK (delta_score BETWEEN -1 AND 1),
CONSTRAINT ck_mastery_change_reason CHECK
(reason IN ('ANSWER', 'DECAY', 'MANUAL', 'REVIEW'))
);
CREATE INDEX idx_mastery_change_mastery_time
ON mastery_change_log (mastery_id, occurred_at DESC);
CREATE INDEX idx_mastery_change_answer
ON mastery_change_log (answer_record_id) WHERE answer_record_id IS NOT NULL;
COMMENT ON TABLE mastery_change_log IS '掌握度变化历史';
COMMENT ON COLUMN mastery_change_log.id IS '变化记录编号';
COMMENT ON COLUMN mastery_change_log.mastery_id IS '掌握记录编号';
COMMENT ON COLUMN mastery_change_log.answer_record_id IS '触发作答；衰减/人工调整可为空';
COMMENT ON COLUMN mastery_change_log.before_score IS '变化前分值';
COMMENT ON COLUMN mastery_change_log.after_score IS '变化后分值';
COMMENT ON COLUMN mastery_change_log.delta_score IS '变化量';
COMMENT ON COLUMN mastery_change_log.reason IS '变化原因';
COMMENT ON COLUMN mastery_change_log.occurred_at IS '发生时间';
COMMENT ON COLUMN mastery_change_log.created_at IS '创建时间';
COMMENT ON COLUMN mastery_change_log.updated_at IS '更新时间';

CREATE TABLE recommendation (
id                         BIGINT        CONSTRAINT pk_recommendation PRIMARY KEY,
student_id                 BIGINT        NOT NULL,
knowledge_point_id         BIGINT        NOT NULL,
source_type                VARCHAR(16)   NOT NULL,
source_id                  BIGINT,
reason_text                VARCHAR(500)  NOT NULL,
priority                   SMALLINT      NOT NULL DEFAULT 3,
recommended_question_count SMALLINT      NOT NULL DEFAULT 10,
status                     VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
generated_task_id          BIGINT,
generated_at               TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
expires_at                 TIMESTAMPTZ,
accepted_at                TIMESTAMPTZ,
completed_at               TIMESTAMPTZ,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT ck_recommendation_source CHECK
(source_type IN ('ERROR', 'MASTERY', 'REVIEW', 'GAP')),
CONSTRAINT ck_recommendation_priority CHECK (priority BETWEEN 1 AND 5),
CONSTRAINT ck_recommendation_count CHECK (recommended_question_count > 0),
CONSTRAINT ck_recommendation_status CHECK (status IN
('PENDING', 'ACCEPTED', 'COMPLETED', 'EXPIRED', 'DISMISSED'))
);
CREATE INDEX idx_recommendation_student_status
ON recommendation (student_id, status, priority DESC, generated_at DESC);
CREATE UNIQUE INDEX uk_recommendation_pending_kp
ON recommendation (student_id, knowledge_point_id, source_type)
WHERE status = 'PENDING';
CREATE INDEX idx_recommendation_generated_task_id
ON recommendation (generated_task_id);
COMMENT ON TABLE recommendation IS '个性化练习推荐';
COMMENT ON COLUMN recommendation.id IS '推荐编号';
COMMENT ON COLUMN recommendation.student_id IS '学生编号';
COMMENT ON COLUMN recommendation.knowledge_point_id IS '推荐知识点';
COMMENT ON COLUMN recommendation.source_type IS '推荐依据';
COMMENT ON COLUMN recommendation.source_id IS '具体来源记录';
COMMENT ON COLUMN recommendation.reason_text IS '可展示的推荐原因';
COMMENT ON COLUMN recommendation.priority IS '优先级，5 最高';
COMMENT ON COLUMN recommendation.recommended_question_count IS '建议题数';
COMMENT ON COLUMN recommendation.status IS '推荐状态';
COMMENT ON COLUMN recommendation.generated_task_id IS '接受后生成的任务';
COMMENT ON COLUMN recommendation.generated_at IS '生成时间';
COMMENT ON COLUMN recommendation.expires_at IS '过期时间';
COMMENT ON COLUMN recommendation.accepted_at IS '接受时间';
COMMENT ON COLUMN recommendation.completed_at IS '完成时间';
COMMENT ON COLUMN recommendation.created_at IS '创建时间';
COMMENT ON COLUMN recommendation.updated_at IS '更新时间';

CREATE TABLE photo_marking_task (
id                    BIGINT        CONSTRAINT pk_photo_marking_task PRIMARY KEY,
student_id            BIGINT        NOT NULL,
original_file_id      UUID          NOT NULL,
practice_task_id      BIGINT,
status                VARCHAR(24)   NOT NULL DEFAULT 'UPLOADED',
page_count            SMALLINT      NOT NULL DEFAULT 1,
overall_confidence    NUMERIC(5,4),
recognized_item_count SMALLINT      NOT NULL DEFAULT 0,
reviewer_id           BIGINT,
review_status         VARCHAR(16)   NOT NULL DEFAULT 'NOT_REQUIRED',
submitted_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
processed_at          TIMESTAMPTZ,
reviewed_at           TIMESTAMPTZ,
failure_reason        VARCHAR(1000),
created_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at            TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version               INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_photo_marking_original_file UNIQUE (original_file_id),
CONSTRAINT ck_photo_marking_status CHECK (status IN
('UPLOADED', 'PROCESSING', 'REVIEW_REQUIRED', 'COMPLETED', 'FAILED')),
CONSTRAINT ck_photo_marking_page_count CHECK (page_count > 0),
CONSTRAINT ck_photo_marking_confidence CHECK
(overall_confidence IS NULL OR
(overall_confidence >= 0 AND overall_confidence <= 1)),
CONSTRAINT ck_photo_marking_item_count CHECK (recognized_item_count >= 0),
CONSTRAINT ck_photo_marking_review_status CHECK (review_status IN
('NOT_REQUIRED', 'PENDING', 'APPROVED', 'CORRECTED')),
CONSTRAINT ck_photo_marking_version CHECK (version >= 0)
);
CREATE INDEX idx_photo_marking_student_created
ON photo_marking_task (student_id, created_at DESC);
CREATE INDEX idx_photo_marking_review_queue
ON photo_marking_task (submitted_at)
WHERE review_status = 'PENDING';
CREATE INDEX idx_photo_marking_practice_task_id
ON photo_marking_task (practice_task_id);
COMMENT ON TABLE photo_marking_task IS '拍照批改任务';
COMMENT ON COLUMN photo_marking_task.id IS '批改任务编号';
COMMENT ON COLUMN photo_marking_task.student_id IS '提交学生';
COMMENT ON COLUMN photo_marking_task.original_file_id IS '原始整页图片';
COMMENT ON COLUMN photo_marking_task.practice_task_id IS '处理后形成的统一练习任务';
COMMENT ON COLUMN photo_marking_task.status IS '处理状态';
COMMENT ON COLUMN photo_marking_task.page_count IS '页数';
COMMENT ON COLUMN photo_marking_task.overall_confidence IS '整体识别可信度';
COMMENT ON COLUMN photo_marking_task.recognized_item_count IS '识别题目数';
COMMENT ON COLUMN photo_marking_task.reviewer_id IS '最终复核教师';
COMMENT ON COLUMN photo_marking_task.review_status IS '复核状态';
COMMENT ON COLUMN photo_marking_task.submitted_at IS '提交时间';
COMMENT ON COLUMN photo_marking_task.processed_at IS '自动处理完成时间';
COMMENT ON COLUMN photo_marking_task.reviewed_at IS '最终复核时间';
COMMENT ON COLUMN photo_marking_task.failure_reason IS '失败摘要';
COMMENT ON COLUMN photo_marking_task.created_at IS '创建时间';
COMMENT ON COLUMN photo_marking_task.updated_at IS '更新时间';
COMMENT ON COLUMN photo_marking_task.version IS '乐观锁版本号';

CREATE TABLE photo_marking_item (
id                     BIGINT        CONSTRAINT pk_photo_marking_item PRIMARY KEY,
marking_task_id        BIGINT        NOT NULL,
item_no                SMALLINT      NOT NULL,
crop_file_id           UUID,
bounding_box           JSONB,
recognized_stem        TEXT,
recognized_answer      JSONB,
matched_question_id    BIGINT,
recognition_confidence NUMERIC(5,4),
task_question_id       BIGINT,
answer_record_id       BIGINT,
status                 VARCHAR(24)   NOT NULL DEFAULT 'RECOGNIZED',
created_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at             TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_photo_marking_item_no UNIQUE (marking_task_id, item_no),
CONSTRAINT ck_photo_marking_item_no CHECK (item_no > 0),
CONSTRAINT ck_photo_item_confidence CHECK
(recognition_confidence IS NULL OR
(recognition_confidence >= 0 AND recognition_confidence <= 1)),
CONSTRAINT ck_photo_item_status CHECK (status IN
('RECOGNIZED', 'REVIEW_REQUIRED', 'CONFIRMED', 'REJECTED'))
);
CREATE INDEX idx_photo_marking_item_task
ON photo_marking_item (marking_task_id, item_no);
CREATE INDEX idx_photo_marking_item_review
ON photo_marking_item (marking_task_id)
WHERE status = 'REVIEW_REQUIRED';
CREATE INDEX idx_photo_marking_item_task_question_id
ON photo_marking_item (task_question_id);
CREATE INDEX idx_photo_marking_item_answer_record_id
ON photo_marking_item (answer_record_id);
COMMENT ON TABLE photo_marking_item IS '拍照切题和识别明细';
COMMENT ON COLUMN photo_marking_item.id IS '识别题目明细编号';
COMMENT ON COLUMN photo_marking_item.marking_task_id IS '批改任务编号';
COMMENT ON COLUMN photo_marking_item.item_no IS '页面内题号';
COMMENT ON COLUMN photo_marking_item.crop_file_id IS '切题图片';
COMMENT ON COLUMN photo_marking_item.bounding_box IS '原图中的坐标区域';
COMMENT ON COLUMN photo_marking_item.recognized_stem IS '识别出的题干';
COMMENT ON COLUMN photo_marking_item.recognized_answer IS '识别出的学生答案/步骤';
COMMENT ON COLUMN photo_marking_item.matched_question_id IS '匹配到的题库题目';
COMMENT ON COLUMN photo_marking_item.recognition_confidence IS '本题识别可信度';
COMMENT ON COLUMN photo_marking_item.task_question_id IS '转换后的任务题目';
COMMENT ON COLUMN photo_marking_item.answer_record_id IS '转换后的统一作答';
COMMENT ON COLUMN photo_marking_item.status IS '明细处理状态';
COMMENT ON COLUMN photo_marking_item.created_at IS '创建时间';
COMMENT ON COLUMN photo_marking_item.updated_at IS '更新时间';

CREATE TABLE photo_review_record (
id              BIGINT        CONSTRAINT pk_photo_review_record PRIMARY KEY,
marking_task_id BIGINT        NOT NULL,
marking_item_id BIGINT,
reviewer_id     BIGINT        NOT NULL,
decision        VARCHAR(16)   NOT NULL,
before_data     JSONB,
after_data      JSONB,
comment         VARCHAR(500),
reviewed_at     TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT ck_photo_review_decision CHECK
(decision IN ('APPROVE', 'CORRECT', 'REJECT'))
);
CREATE INDEX idx_photo_review_task_time
ON photo_review_record (marking_task_id, reviewed_at DESC);
CREATE INDEX idx_photo_review_reviewer_time
ON photo_review_record (reviewer_id, reviewed_at DESC);
CREATE INDEX idx_photo_review_marking_item_id
ON photo_review_record (marking_item_id);
COMMENT ON TABLE photo_review_record IS '人工复核历史';
COMMENT ON COLUMN photo_review_record.id IS '复核记录编号';
COMMENT ON COLUMN photo_review_record.marking_task_id IS '批改任务';
COMMENT ON COLUMN photo_review_record.marking_item_id IS '具体题目；整单复核时为空';
COMMENT ON COLUMN photo_review_record.reviewer_id IS '复核人';
COMMENT ON COLUMN photo_review_record.decision IS '复核结论';
COMMENT ON COLUMN photo_review_record.before_data IS '修改前快照';
COMMENT ON COLUMN photo_review_record.after_data IS '修改后快照';
COMMENT ON COLUMN photo_review_record.comment IS '复核说明';
COMMENT ON COLUMN photo_review_record.reviewed_at IS '复核时间';
COMMENT ON COLUMN photo_review_record.created_at IS '创建时间';
COMMENT ON COLUMN photo_review_record.updated_at IS '更新时间';
