-- content_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE content_module (
id                  BIGINT       CONSTRAINT pk_content_module PRIMARY KEY,
code                VARCHAR(64)  NOT NULL,
name                VARCHAR(100) NOT NULL,
grade_level         SMALLINT     NOT NULL,
semester            SMALLINT     NOT NULL,
learning_theme      VARCHAR(100) NOT NULL,
publisher           VARCHAR(100),
edition_description VARCHAR(100),
description         VARCHAR(500),
sort_order          INTEGER      NOT NULL DEFAULT 0,
status              SMALLINT     NOT NULL DEFAULT 1,
created_by          BIGINT       NOT NULL,
created_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
version             INTEGER      NOT NULL DEFAULT 0,
CONSTRAINT uk_content_module_code UNIQUE (code),
CONSTRAINT ck_content_module_grade CHECK (grade_level BETWEEN 1 AND 6),
CONSTRAINT ck_content_module_semester CHECK (semester BETWEEN 1 AND 2),
CONSTRAINT ck_content_module_sort CHECK (sort_order >= 0),
CONSTRAINT ck_content_module_status CHECK (status IN (0, 1)),
CONSTRAINT ck_content_module_version CHECK (version >= 0)
);
CREATE INDEX idx_content_module_grade_semester
ON content_module (grade_level, semester, sort_order)
WHERE is_deleted = FALSE AND status = 1;
COMMENT ON TABLE content_module IS '按年级学期和主题组织的内容模块';
COMMENT ON COLUMN content_module.id IS '模块编号';
COMMENT ON COLUMN content_module.code IS '稳定模块编码';
COMMENT ON COLUMN content_module.name IS '模块名称';
COMMENT ON COLUMN content_module.grade_level IS '适用年级';
COMMENT ON COLUMN content_module.semester IS '适用学期';
COMMENT ON COLUMN content_module.learning_theme IS '学习主题，如 100 以内加减法';
COMMENT ON COLUMN content_module.publisher IS '出版社，可为空表示通用内容';
COMMENT ON COLUMN content_module.edition_description IS '版本说明';
COMMENT ON COLUMN content_module.description IS '模块说明';
COMMENT ON COLUMN content_module.sort_order IS '展示顺序';
COMMENT ON COLUMN content_module.status IS '启用状态';
COMMENT ON COLUMN content_module.created_by IS '创建管理员编号';
COMMENT ON COLUMN content_module.created_at IS '创建时间';
COMMENT ON COLUMN content_module.updated_at IS '更新时间';
COMMENT ON COLUMN content_module.is_deleted IS '是否删除';
COMMENT ON COLUMN content_module.version IS '乐观锁版本号';

CREATE TABLE knowledge_point (
id               BIGINT        CONSTRAINT pk_knowledge_point PRIMARY KEY,
module_id        BIGINT        NOT NULL,
parent_id        BIGINT,
code             VARCHAR(64)   NOT NULL,
name             VARCHAR(100)  NOT NULL,
description      VARCHAR(1000),
difficulty_level SMALLINT      NOT NULL DEFAULT 1,
sort_order       INTEGER       NOT NULL DEFAULT 0,
status           SMALLINT      NOT NULL DEFAULT 1,
created_by       BIGINT        NOT NULL,
created_at       TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at       TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted          BOOLEAN       NOT NULL DEFAULT FALSE,
version          INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_knowledge_point_code UNIQUE (code),
CONSTRAINT ck_knowledge_point_not_self CHECK (parent_id IS NULL OR parent_id <> id),
CONSTRAINT ck_knowledge_point_difficulty CHECK (difficulty_level BETWEEN 1 AND 5),
CONSTRAINT ck_knowledge_point_sort CHECK (sort_order >= 0),
CONSTRAINT ck_knowledge_point_status CHECK (status IN (0, 1)),
CONSTRAINT ck_knowledge_point_version CHECK (version >= 0)
);
CREATE INDEX idx_knowledge_point_module_sort
ON knowledge_point (module_id, sort_order)
WHERE is_deleted = FALSE AND status = 1;
CREATE INDEX idx_knowledge_point_parent ON knowledge_point (parent_id)
WHERE parent_id IS NOT NULL AND is_deleted = FALSE;
COMMENT ON TABLE knowledge_point IS '知识点';
COMMENT ON COLUMN knowledge_point.id IS '知识点编号';
COMMENT ON COLUMN knowledge_point.module_id IS '所属内容模块';
COMMENT ON COLUMN knowledge_point.parent_id IS '上级知识点';
COMMENT ON COLUMN knowledge_point.code IS '稳定知识点编码';
COMMENT ON COLUMN knowledge_point.name IS '知识点名称';
COMMENT ON COLUMN knowledge_point.description IS '知识点说明';
COMMENT ON COLUMN knowledge_point.difficulty_level IS '基础难度等级';
COMMENT ON COLUMN knowledge_point.sort_order IS '模块内排序';
COMMENT ON COLUMN knowledge_point.status IS '启用状态';
COMMENT ON COLUMN knowledge_point.created_by IS '创建管理员编号';
COMMENT ON COLUMN knowledge_point.created_at IS '创建时间';
COMMENT ON COLUMN knowledge_point.updated_at IS '更新时间';
COMMENT ON COLUMN knowledge_point.is_deleted IS '是否删除';
COMMENT ON COLUMN knowledge_point.version IS '乐观锁版本号';

CREATE TABLE question (
id                BIGINT       CONSTRAINT pk_question PRIMARY KEY,
code              VARCHAR(64)  NOT NULL,
question_mode     VARCHAR(16)  NOT NULL DEFAULT 'FIXED',
question_type     VARCHAR(32)  NOT NULL,
stem              TEXT         NOT NULL,
options           JSONB,
standard_answer   JSONB        NOT NULL,
analysis          TEXT,
generation_rule   JSONB,
difficulty        SMALLINT     NOT NULL DEFAULT 1,
estimated_seconds INTEGER,
status            VARCHAR(16)  NOT NULL DEFAULT 'DRAFT',
created_by        BIGINT       NOT NULL,
reviewed_by       BIGINT,
reviewed_at       TIMESTAMPTZ,
created_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted           BOOLEAN      NOT NULL DEFAULT FALSE,
version           INTEGER      NOT NULL DEFAULT 0,
CONSTRAINT uk_question_code UNIQUE (code),
CONSTRAINT ck_question_mode CHECK (question_mode IN ('FIXED', 'TEMPLATE')),
CONSTRAINT ck_question_type CHECK (question_type IN
('ARITHMETIC', 'FILL_BLANK', 'VERTICAL', 'CHOICE', 'JUDGEMENT')),
CONSTRAINT ck_question_template_rule CHECK
(question_mode = 'FIXED' OR generation_rule IS NOT NULL),
CONSTRAINT ck_question_difficulty CHECK (difficulty BETWEEN 1 AND 5),
CONSTRAINT ck_question_estimated_seconds CHECK
(estimated_seconds IS NULL OR estimated_seconds > 0),
CONSTRAINT ck_question_status CHECK (status IN ('DRAFT', 'ENABLED', 'DISABLED')),
CONSTRAINT ck_question_version CHECK (version >= 0)
);
CREATE INDEX idx_question_type_difficulty
ON question (question_type, difficulty)
WHERE is_deleted = FALSE AND status = 'ENABLED';
COMMENT ON TABLE question IS '固定题与生成模板题';
COMMENT ON COLUMN question.id IS '题目编号';
COMMENT ON COLUMN question.code IS '稳定题目编码';
COMMENT ON COLUMN question.question_mode IS '固定题或生成模板';
COMMENT ON COLUMN question.question_type IS '口算、填空、竖式、选择、判断';
COMMENT ON COLUMN question.stem IS '题干；模板题可含占位符';
COMMENT ON COLUMN question.options IS '选择题选项，其他题型为空';
COMMENT ON COLUMN question.standard_answer IS '标准答案和必要步骤结构';
COMMENT ON COLUMN question.analysis IS '解析';
COMMENT ON COLUMN question.generation_rule IS '数值范围、运算规则等生成参数';
COMMENT ON COLUMN question.difficulty IS '题目难度';
COMMENT ON COLUMN question.estimated_seconds IS '预计作答秒数';
COMMENT ON COLUMN question.status IS '题目状态';
COMMENT ON COLUMN question.created_by IS '创建人';
COMMENT ON COLUMN question.reviewed_by IS '审核人';
COMMENT ON COLUMN question.reviewed_at IS '审核时间';
COMMENT ON COLUMN question.created_at IS '创建时间';
COMMENT ON COLUMN question.updated_at IS '更新时间';
COMMENT ON COLUMN question.is_deleted IS '是否删除';
COMMENT ON COLUMN question.version IS '乐观锁版本号';

CREATE TABLE question_knowledge_point (
question_id       BIGINT       NOT NULL,
knowledge_point_id BIGINT      NOT NULL,
is_primary        BOOLEAN      NOT NULL DEFAULT FALSE,
weight            NUMERIC(5,4) NOT NULL DEFAULT 1.0000,
created_at        TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_question_knowledge_point PRIMARY KEY (question_id, knowledge_point_id),
CONSTRAINT ck_question_kp_weight CHECK (weight > 0 AND weight <= 1)
);
CREATE UNIQUE INDEX uk_question_primary_knowledge
ON question_knowledge_point (question_id) WHERE is_primary = TRUE;
CREATE INDEX idx_question_kp_knowledge
ON question_knowledge_point (knowledge_point_id, question_id);
COMMENT ON TABLE question_knowledge_point IS '题目知识点多对多关联';
COMMENT ON COLUMN question_knowledge_point.question_id IS '题目编号';
COMMENT ON COLUMN question_knowledge_point.knowledge_point_id IS '知识点编号';
COMMENT ON COLUMN question_knowledge_point.is_primary IS '是否主要知识点';
COMMENT ON COLUMN question_knowledge_point.weight IS '对掌握度更新的权重';
COMMENT ON COLUMN question_knowledge_point.created_at IS '关联时间';
COMMENT ON COLUMN question_knowledge_point.updated_at IS '更新时间';

CREATE TABLE error_type (
id                       BIGINT        CONSTRAINT pk_error_type PRIMARY KEY,
code                     VARCHAR(64)   NOT NULL,
name                     VARCHAR(100)  NOT NULL,
category                 VARCHAR(32)   NOT NULL,
applicable_question_type VARCHAR(32),
description              VARCHAR(500),
explanation_template     VARCHAR(1000),
remediation_hint         VARCHAR(1000),
status                   SMALLINT      NOT NULL DEFAULT 1,
created_at               TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at               TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted                  BOOLEAN       NOT NULL DEFAULT FALSE,
version                  INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_error_type_code UNIQUE (code),
CONSTRAINT ck_error_type_category CHECK (category IN
('CALCULATION', 'PLACE_VALUE', 'CARRY', 'BORROW', 'FORMAT', 'CONCEPT', 'OTHER')),
CONSTRAINT ck_error_type_question_type CHECK
(applicable_question_type IS NULL OR applicable_question_type IN
('ARITHMETIC', 'FILL_BLANK', 'VERTICAL', 'CHOICE', 'JUDGEMENT')),
CONSTRAINT ck_error_type_status CHECK (status IN (0, 1)),
CONSTRAINT ck_error_type_version CHECK (version >= 0)
);
CREATE INDEX idx_error_type_category
ON error_type (category, code) WHERE is_deleted = FALSE AND status = 1;
COMMENT ON TABLE error_type IS '可解释错因字典';
COMMENT ON COLUMN error_type.id IS '错因类型编号';
COMMENT ON COLUMN error_type.code IS '稳定错因编码';
COMMENT ON COLUMN error_type.name IS '错因名称';
COMMENT ON COLUMN error_type.category IS '错因大类';
COMMENT ON COLUMN error_type.applicable_question_type IS '适用题型；为空表示通用';
COMMENT ON COLUMN error_type.description IS '判定说明';
COMMENT ON COLUMN error_type.explanation_template IS '面向学生的解释模板';
COMMENT ON COLUMN error_type.remediation_hint IS '针对性练习提示';
COMMENT ON COLUMN error_type.status IS '启用状态';
COMMENT ON COLUMN error_type.created_at IS '创建时间';
COMMENT ON COLUMN error_type.updated_at IS '更新时间';
COMMENT ON COLUMN error_type.is_deleted IS '是否删除';
COMMENT ON COLUMN error_type.version IS '乐观锁版本号';

CREATE TABLE content_outbox_event (
id            UUID          CONSTRAINT pk_content_outbox_event PRIMARY KEY,
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
CONSTRAINT ck_content_outbox_event_status
CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
CONSTRAINT ck_content_outbox_event_retry_count CHECK (retry_count >= 0)
);
CREATE INDEX idx_content_outbox_event_pending
ON content_outbox_event (next_retry_at, created_at)
WHERE status = 'PENDING';
CREATE INDEX idx_content_outbox_event_published
ON content_outbox_event (published_at)
WHERE status = 'PUBLISHED';
COMMENT ON TABLE content_outbox_event IS '内容服务事务发件箱';
COMMENT ON COLUMN content_outbox_event.id IS '领域事件编号';
COMMENT ON COLUMN content_outbox_event.event_type IS '事件类型和版本';
COMMENT ON COLUMN content_outbox_event.routing_key IS 'RabbitMQ 路由键';
COMMENT ON COLUMN content_outbox_event.payload IS 'JSON 事件快照';
COMMENT ON COLUMN content_outbox_event.status IS '发布状态';
COMMENT ON COLUMN content_outbox_event.retry_count IS '失败重试次数';
COMMENT ON COLUMN content_outbox_event.next_retry_at IS '下次可重试时间';
COMMENT ON COLUMN content_outbox_event.created_at IS '入箱时间';
COMMENT ON COLUMN content_outbox_event.published_at IS 'Broker 确认时间';
COMMENT ON COLUMN content_outbox_event.last_error IS '最近失败摘要';
COMMENT ON COLUMN content_outbox_event.updated_at IS '更新时间';
