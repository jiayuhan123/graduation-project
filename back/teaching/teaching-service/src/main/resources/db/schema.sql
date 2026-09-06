-- teaching_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE teaching_class (
id            BIGINT        CONSTRAINT pk_teaching_class PRIMARY KEY,
class_code    VARCHAR(32)   NOT NULL,
name          VARCHAR(100)  NOT NULL,
school_name   VARCHAR(150),
academic_year VARCHAR(9)    NOT NULL,
grade_level   SMALLINT      NOT NULL,
semester      SMALLINT      NOT NULL,
status        VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
created_by    BIGINT        NOT NULL,
created_at    TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at    TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version       INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_teaching_class_code UNIQUE (class_code),
CONSTRAINT ck_teaching_class_year CHECK
(academic_year ~ '^[0-9]{4}-[0-9]{4}$'),
CONSTRAINT ck_teaching_class_grade CHECK (grade_level BETWEEN 1 AND 6),
CONSTRAINT ck_teaching_class_semester CHECK (semester BETWEEN 1 AND 2),
CONSTRAINT ck_teaching_class_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
CONSTRAINT ck_teaching_class_version CHECK (version >= 0)
);
CREATE INDEX idx_teaching_class_creator_status
ON teaching_class (created_by, status, created_at DESC);
COMMENT ON TABLE teaching_class IS '教学班级';
COMMENT ON COLUMN teaching_class.id IS '班级编号';
COMMENT ON COLUMN teaching_class.class_code IS '邀请/检索使用的班级码';
COMMENT ON COLUMN teaching_class.name IS '班级名称';
COMMENT ON COLUMN teaching_class.school_name IS '学校名称，仅作为属性';
COMMENT ON COLUMN teaching_class.academic_year IS '学年，如 2026-2027';
COMMENT ON COLUMN teaching_class.grade_level IS '年级';
COMMENT ON COLUMN teaching_class.semester IS '学期';
COMMENT ON COLUMN teaching_class.status IS '班级状态';
COMMENT ON COLUMN teaching_class.created_by IS '创建教师';
COMMENT ON COLUMN teaching_class.created_at IS '创建时间';
COMMENT ON COLUMN teaching_class.updated_at IS '更新时间';
COMMENT ON COLUMN teaching_class.version IS '乐观锁版本号';

CREATE TABLE class_teacher (
class_id     BIGINT       NOT NULL,
teacher_id   BIGINT       NOT NULL,
teacher_role VARCHAR(16)  NOT NULL DEFAULT 'ASSISTANT',
joined_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_class_teacher PRIMARY KEY (class_id, teacher_id),
CONSTRAINT ck_class_teacher_role CHECK (teacher_role IN ('OWNER', 'ASSISTANT'))
);
CREATE UNIQUE INDEX uk_class_teacher_owner
ON class_teacher (class_id) WHERE teacher_role = 'OWNER';
CREATE INDEX idx_class_teacher_teacher
ON class_teacher (teacher_id, class_id);
COMMENT ON TABLE class_teacher IS '班级教师多对多关系';
COMMENT ON COLUMN class_teacher.class_id IS '班级编号';
COMMENT ON COLUMN class_teacher.teacher_id IS '教师编号';
COMMENT ON COLUMN class_teacher.teacher_role IS '班级内角色';
COMMENT ON COLUMN class_teacher.joined_at IS '加入时间';
COMMENT ON COLUMN class_teacher.created_at IS '创建时间';
COMMENT ON COLUMN class_teacher.updated_at IS '更新时间';

CREATE TABLE class_student (
class_id            BIGINT       NOT NULL,
student_id          BIGINT       NOT NULL,
student_no_in_class VARCHAR(32),
status              VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE',
joined_at           TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
left_at             TIMESTAMPTZ,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_class_student PRIMARY KEY (class_id, student_id),
CONSTRAINT ck_class_student_status CHECK (status IN ('ACTIVE', 'LEFT')),
CONSTRAINT ck_class_student_left_at CHECK
((status = 'ACTIVE' AND left_at IS NULL) OR
(status = 'LEFT' AND left_at IS NOT NULL))
);
CREATE UNIQUE INDEX uk_class_student_no
ON class_student (class_id, student_no_in_class)
WHERE student_no_in_class IS NOT NULL AND status = 'ACTIVE';
CREATE INDEX idx_class_student_student
ON class_student (student_id, status, class_id);
COMMENT ON TABLE class_student IS '班级学生多对多关系';
COMMENT ON COLUMN class_student.class_id IS '班级编号';
COMMENT ON COLUMN class_student.student_id IS '学生编号';
COMMENT ON COLUMN class_student.student_no_in_class IS '班内学号';
COMMENT ON COLUMN class_student.status IS '成员状态';
COMMENT ON COLUMN class_student.joined_at IS '加入时间';
COMMENT ON COLUMN class_student.left_at IS '离开时间';
COMMENT ON COLUMN class_student.created_at IS '创建时间';
COMMENT ON COLUMN class_student.updated_at IS '更新时间';

CREATE TABLE assignment (
id              BIGINT        CONSTRAINT pk_assignment PRIMARY KEY,
class_id        BIGINT        NOT NULL,
teacher_id      BIGINT        NOT NULL,
title           VARCHAR(128)  NOT NULL,
description     VARCHAR(1000),
assignment_type VARCHAR(16)   NOT NULL DEFAULT 'UNIFIED',
status          VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
start_at        TIMESTAMPTZ     NOT NULL,
due_at          TIMESTAMPTZ     NOT NULL,
total_score     NUMERIC(8,2)  NOT NULL,
settings        JSONB,
published_at    TIMESTAMPTZ,
created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version         INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT ck_assignment_type CHECK (assignment_type IN ('UNIFIED', 'LAYERED')),
CONSTRAINT ck_assignment_status CHECK
(status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'CANCELLED')),
CONSTRAINT ck_assignment_time CHECK (due_at > start_at),
CONSTRAINT ck_assignment_score CHECK (total_score > 0),
CONSTRAINT ck_assignment_version CHECK (version >= 0)
);
CREATE INDEX idx_assignment_class_status_due
ON assignment (class_id, status, due_at DESC);
CREATE INDEX idx_assignment_teacher_created
ON assignment (teacher_id, created_at DESC);
COMMENT ON TABLE assignment IS '教师作业';
COMMENT ON COLUMN assignment.id IS '作业编号';
COMMENT ON COLUMN assignment.class_id IS '所属班级';
COMMENT ON COLUMN assignment.teacher_id IS '创建教师';
COMMENT ON COLUMN assignment.title IS '作业标题';
COMMENT ON COLUMN assignment.description IS '作业说明';
COMMENT ON COLUMN assignment.assignment_type IS '统一或分层作业';
COMMENT ON COLUMN assignment.status IS '作业状态';
COMMENT ON COLUMN assignment.start_at IS '开始时间';
COMMENT ON COLUMN assignment.due_at IS '截止时间';
COMMENT ON COLUMN assignment.total_score IS '作业总分';
COMMENT ON COLUMN assignment.settings IS '限时、乱序、提交次数等规则';
COMMENT ON COLUMN assignment.published_at IS '发布时间';
COMMENT ON COLUMN assignment.created_at IS '创建时间';
COMMENT ON COLUMN assignment.updated_at IS '更新时间';
COMMENT ON COLUMN assignment.version IS '乐观锁版本号';

CREATE TABLE assignment_question (
id                BIGINT        CONSTRAINT pk_assignment_question PRIMARY KEY,
assignment_id     BIGINT        NOT NULL,
layer_code        VARCHAR(32)   NOT NULL DEFAULT 'ALL',
question_id       BIGINT        NOT NULL,
sequence_no       SMALLINT      NOT NULL,
score             NUMERIC(8,2)  NOT NULL,
question_snapshot JSONB         NOT NULL,
created_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_assignment_question_seq
UNIQUE (assignment_id, layer_code, sequence_no),
CONSTRAINT ck_assignment_question_seq CHECK (sequence_no > 0),
CONSTRAINT ck_assignment_question_score CHECK (score > 0)
);
CREATE INDEX idx_assignment_question_question
ON assignment_question (question_id, assignment_id);
COMMENT ON TABLE assignment_question IS '作业分层题目及快照';
COMMENT ON COLUMN assignment_question.id IS '作业题目明细编号';
COMMENT ON COLUMN assignment_question.assignment_id IS '作业编号';
COMMENT ON COLUMN assignment_question.layer_code IS '分层编码';
COMMENT ON COLUMN assignment_question.question_id IS '题目编号';
COMMENT ON COLUMN assignment_question.sequence_no IS '层内顺序';
COMMENT ON COLUMN assignment_question.score IS '本题分值';
COMMENT ON COLUMN assignment_question.question_snapshot IS '发布时题目快照';
COMMENT ON COLUMN assignment_question.created_at IS '创建时间';
COMMENT ON COLUMN assignment_question.updated_at IS '更新时间';

CREATE TABLE assignment_submission (
id              BIGINT        CONSTRAINT pk_assignment_submission PRIMARY KEY,
assignment_id   BIGINT        NOT NULL,
student_id      BIGINT        NOT NULL,
layer_code      VARCHAR(32)   NOT NULL DEFAULT 'ALL',
practice_task_id BIGINT,
status          VARCHAR(16)   NOT NULL DEFAULT 'ASSIGNED',
score           NUMERIC(8,2),
correct_count   SMALLINT      NOT NULL DEFAULT 0,
total_count     SMALLINT      NOT NULL DEFAULT 0,
started_at      TIMESTAMPTZ,
submitted_at    TIMESTAMPTZ,
graded_at       TIMESTAMPTZ,
teacher_comment VARCHAR(500),
created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version         INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_assignment_submission_student UNIQUE (assignment_id, student_id),
CONSTRAINT ck_assignment_submission_status CHECK (status IN
('ASSIGNED', 'IN_PROGRESS', 'SUBMITTED', 'GRADED', 'OVERDUE')),
CONSTRAINT ck_assignment_submission_score CHECK (score IS NULL OR score >= 0),
CONSTRAINT ck_assignment_submission_counts CHECK
(correct_count >= 0 AND total_count >= 0 AND correct_count <= total_count),
CONSTRAINT ck_assignment_submission_version CHECK (version >= 0)
);
CREATE INDEX idx_assignment_submission_student_status
ON assignment_submission (student_id, status, created_at DESC);
CREATE INDEX idx_assignment_submission_assignment_status
ON assignment_submission (assignment_id, status, submitted_at);
COMMENT ON TABLE assignment_submission IS '学生作业分配和提交汇总';
COMMENT ON COLUMN assignment_submission.id IS '作业提交编号';
COMMENT ON COLUMN assignment_submission.assignment_id IS '作业编号';
COMMENT ON COLUMN assignment_submission.student_id IS '学生编号';
COMMENT ON COLUMN assignment_submission.layer_code IS '学生适用题组';
COMMENT ON COLUMN assignment_submission.practice_task_id IS '学习服务生成的练习任务';
COMMENT ON COLUMN assignment_submission.status IS '提交状态';
COMMENT ON COLUMN assignment_submission.score IS '最终得分';
COMMENT ON COLUMN assignment_submission.correct_count IS '正确题数快照';
COMMENT ON COLUMN assignment_submission.total_count IS '总题数快照';
COMMENT ON COLUMN assignment_submission.started_at IS '开始时间';
COMMENT ON COLUMN assignment_submission.submitted_at IS '提交时间';
COMMENT ON COLUMN assignment_submission.graded_at IS '批改完成时间';
COMMENT ON COLUMN assignment_submission.teacher_comment IS '教师评语';
COMMENT ON COLUMN assignment_submission.created_at IS '分配时间';
COMMENT ON COLUMN assignment_submission.updated_at IS '更新时间';
COMMENT ON COLUMN assignment_submission.version IS '乐观锁版本号';

CREATE TABLE teaching_outbox_event (
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
CREATE INDEX idx_teaching_outbox_event_pending
ON teaching_outbox_event (next_retry_at, created_at)
WHERE status = 'PENDING';
CREATE INDEX idx_teaching_outbox_event_published
ON teaching_outbox_event (published_at)
WHERE status = 'PUBLISHED';
COMMENT ON TABLE teaching_outbox_event IS '教学服务事务发件箱';
