-- Portfolio demo dataset for the monolith database.
--
-- Characteristics:
--   * idempotent: rerunning refreshes the same deterministic demo records;
--   * isolated: all bigint identifiers use the 81-89 prefix range;
--   * reversible: use demo-data-reset.sql to remove only this dataset;
--   * safe for an existing database: base content and non-demo users are untouched.
--
-- Public demo accounts (portfolio environment only):
--   teacher: demo_teacher / Demo@2026
--   student: demo_student / Demo@2026

BEGIN;

DO $$
BEGIN
    IF to_regclass('public.t_user') IS NULL
        OR to_regclass('public.question') IS NULL
        OR to_regclass('public.teaching_class') IS NULL
        OR to_regclass('public.practice_task') IS NULL THEN
        RAISE EXCEPTION 'Core schema is incomplete. Run init-schema.sql first.';
    END IF;

    IF (SELECT COUNT(*) FROM question
        WHERE id = ANY (ARRAY[
            930301020001::BIGINT, 930301020002::BIGINT, 930301020003::BIGINT,
            930301020004::BIGINT, 930301040001::BIGINT, 930301040002::BIGINT,
            930301040003::BIGINT, 930301040004::BIGINT, 930301060001::BIGINT,
            930302020001::BIGINT, 930302020002::BIGINT, 930302020003::BIGINT,
            930302020004::BIGINT, 930302060001::BIGINT, 930302060002::BIGINT,
            930302060003::BIGINT, 930302060004::BIGINT
        ])) <> 17 THEN
        RAISE EXCEPTION 'Grade 3 content seed is incomplete. Run seed-data.sql first.';
    END IF;
END
$$;

-- Keep immutable question snapshots compatible with QuestionResponse.
CREATE OR REPLACE FUNCTION pg_temp.demo_question_snapshot(question_row question)
RETURNS JSONB
LANGUAGE SQL
STABLE
AS $$
    SELECT jsonb_build_object(
        'id', question_row.id,
        'code', question_row.code,
        'questionMode', question_row.question_mode,
        'questionType', question_row.question_type,
        'stem', question_row.stem,
        'options', CASE
            WHEN question_row.options IS NULL THEN NULL
            ELSE question_row.options::TEXT
        END,
        'standardAnswer', question_row.standard_answer::TEXT,
        'analysis', question_row.analysis,
        'generationRule', CASE
            WHEN question_row.generation_rule IS NULL THEN NULL
            ELSE question_row.generation_rule::TEXT
        END,
        'difficulty', question_row.difficulty,
        'estimatedSeconds', question_row.estimated_seconds,
        'status', question_row.status,
        'knowledgePointIds', COALESCE(
            (
                SELECT jsonb_agg(relation.knowledge_point_id ORDER BY relation.knowledge_point_id)
                FROM question_knowledge_point relation
                WHERE relation.question_id = question_row.id
            ),
            '[]'::JSONB
        )
    );
$$;

-- 1. Teacher and student accounts.
WITH demo_user (
    id, username, password_hash, nickname, real_name, email, phone, created_days_ago
) AS (
    VALUES
        (810000000000000001::BIGINT, 'demo_teacher',
         '$2a$10$SSA9aYNeuydQDw6sbV/OB.zgFr8vejYHO8KuvKJX1INz1k0CVig4G',
         '王老师', '王明远', 'teacher.demo@example.com', NULL, 120),
        (810000000000000101::BIGINT, 'demo_student',
         '$2a$10$SSA9aYNeuydQDw6sbV/OB.zgFr8vejYHO8KuvKJX1INz1k0CVig4G',
         '小宇', '陈小宇', 'student.demo@example.com', NULL, 100),
        (810000000000000102::BIGINT, 'demo_student02', NULL, '思齐', '林思齐', NULL, NULL, 99),
        (810000000000000103::BIGINT, 'demo_student03', NULL, '可欣', '周可欣', NULL, NULL, 98),
        (810000000000000104::BIGINT, 'demo_student04', NULL, '浩然', '赵浩然', NULL, NULL, 97),
        (810000000000000105::BIGINT, 'demo_student05', NULL, '雨桐', '沈雨桐', NULL, NULL, 96),
        (810000000000000106::BIGINT, 'demo_student06', NULL, '子墨', '徐子墨', NULL, NULL, 95),
        (810000000000000107::BIGINT, 'demo_student07', NULL, '梓涵', '吴梓涵', NULL, NULL, 94),
        (810000000000000108::BIGINT, 'demo_student08', NULL, '晨曦', '郑晨曦', NULL, NULL, 93),
        (810000000000000109::BIGINT, 'demo_student09', NULL, '嘉怡', '孙嘉怡', NULL, NULL, 92),
        (810000000000000110::BIGINT, 'demo_student10', NULL, '明轩', '何明轩', NULL, NULL, 91),
        (810000000000000111::BIGINT, 'demo_student11', NULL, '若彤', '许若彤', NULL, NULL, 90),
        (810000000000000112::BIGINT, 'demo_student12', NULL, '景行', '李景行', NULL, NULL, 89)
)
INSERT INTO t_user (
    id, username, password, nickname, real_name, email, phone, status,
    last_login_at, last_login_ip, password_changed_at,
    created_at, updated_at, is_deleted, version
)
SELECT
    id, username, password_hash, nickname, real_name, email, phone, 1,
    CASE WHEN password_hash IS NULL THEN NULL ELSE CURRENT_TIMESTAMP - INTERVAL '1 day' END,
    CASE WHEN password_hash IS NULL THEN NULL ELSE '127.0.0.1' END,
    CASE WHEN password_hash IS NULL THEN NULL ELSE CURRENT_TIMESTAMP - INTERVAL '30 days' END,
    CURRENT_TIMESTAMP - make_interval(days => created_days_ago::INTEGER),
    CURRENT_TIMESTAMP, FALSE, 0
FROM demo_user
ON CONFLICT (id) DO UPDATE
SET username = EXCLUDED.username,
    password = EXCLUDED.password,
    nickname = EXCLUDED.nickname,
    real_name = EXCLUDED.real_name,
    email = EXCLUDED.email,
    phone = EXCLUDED.phone,
    status = 1,
    is_deleted = FALSE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_teacher_profile (
    user_id, teacher_no, title, introduction, created_at, updated_at, version
) VALUES (
    810000000000000001, 'DEMO-T-001', '小学数学教师',
    '负责三年级数学教学，关注数据驱动的分层练习与过程性评价。',
    CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (user_id) DO UPDATE
SET teacher_no = EXCLUDED.teacher_no,
    title = EXCLUDED.title,
    introduction = EXCLUDED.introduction,
    updated_at = CURRENT_TIMESTAMP;

WITH student_profile (user_id, student_no, sequence_no) AS (
    SELECT
        810000000000000100::BIGINT + sequence_no,
        'DEMO-S-' || LPAD(sequence_no::TEXT, 3, '0'),
        sequence_no
    FROM generate_series(1, 12) AS series(sequence_no)
)
INSERT INTO sys_student_profile (
    user_id, student_no, grade_level, semester, created_at, updated_at, version
)
SELECT
    user_id, student_no, 3, 1,
    CURRENT_TIMESTAMP - make_interval(days => (101 - sequence_no)::INTEGER),
    CURRENT_TIMESTAMP, 0
FROM student_profile
ON CONFLICT (user_id) DO UPDATE
SET student_no = EXCLUDED.student_no,
    grade_level = 3,
    semester = 1,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT 810000000000000001, role.id, CURRENT_TIMESTAMP - INTERVAL '120 days', CURRENT_TIMESTAMP
FROM sys_role role
WHERE role.code = 'teacher'
ON CONFLICT (user_id, role_id) DO NOTHING;

INSERT INTO sys_user_role (user_id, role_id, created_at, updated_at)
SELECT student.user_id, role.id, CURRENT_TIMESTAMP - INTERVAL '100 days', CURRENT_TIMESTAMP
FROM (
    SELECT 810000000000000100::BIGINT + sequence_no AS user_id
    FROM generate_series(1, 12) AS series(sequence_no)
) student
CROSS JOIN sys_role role
WHERE role.code = 'student'
ON CONFLICT (user_id, role_id) DO NOTHING;

-- 2. One active class and three assignments in different lifecycle states.
INSERT INTO teaching_class (
    id, class_code, name, school_name, academic_year, grade_level, semester,
    status, created_by, created_at, updated_at, version
) VALUES (
    820000000000000001, 'DEMO2026', '三年级（1）班', '向阳实验小学',
    '2026-2027', 3, 1, 'ACTIVE', 810000000000000001,
    CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE
SET class_code = EXCLUDED.class_code,
    name = EXCLUDED.name,
    school_name = EXCLUDED.school_name,
    academic_year = EXCLUDED.academic_year,
    grade_level = EXCLUDED.grade_level,
    semester = EXCLUDED.semester,
    status = 'ACTIVE',
    created_by = EXCLUDED.created_by,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO class_teacher (
    class_id, teacher_id, teacher_role, joined_at, created_at, updated_at
) VALUES (
    820000000000000001, 810000000000000001, 'OWNER',
    CURRENT_TIMESTAMP - INTERVAL '90 days',
    CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP
)
ON CONFLICT (class_id, teacher_id) DO UPDATE
SET teacher_role = 'OWNER', updated_at = CURRENT_TIMESTAMP;

INSERT INTO class_student (
    class_id, student_id, student_no_in_class, status,
    joined_at, left_at, created_at, updated_at
)
SELECT
    820000000000000001,
    810000000000000100::BIGINT + sequence_no,
    LPAD(sequence_no::TEXT, 2, '0'),
    'ACTIVE',
    CURRENT_TIMESTAMP - make_interval(days => (80 - sequence_no)::INTEGER),
    NULL,
    CURRENT_TIMESTAMP - make_interval(days => (80 - sequence_no)::INTEGER),
    CURRENT_TIMESTAMP
FROM generate_series(1, 12) AS series(sequence_no)
ON CONFLICT (class_id, student_id) DO UPDATE
SET student_no_in_class = EXCLUDED.student_no_in_class,
    status = 'ACTIVE',
    left_at = NULL,
    updated_at = CURRENT_TIMESTAMP;

WITH demo_assignment (
    id, title, description, assignment_type, status,
    start_offset, due_offset, published_offset, total_score, settings
) AS (
    VALUES
        (821000000000000001::BIGINT, '三年级混合运算周测',
         '检测运算顺序、括号和多位数计算的掌握情况。',
         'UNIFIED', 'CLOSED', INTERVAL '-16 days', INTERVAL '-9 days',
         INTERVAL '-16 days', 100.00::NUMERIC,
         '{"shuffleQuestions":false,"maxAttempts":1,"showAnalysisAfterSubmit":true}'::JSONB),
        (821000000000000002::BIGINT, '薄弱知识点分层巩固',
         '根据班级近期学情分层布置，重点巩固进位乘法与分数计算。',
         'LAYERED', 'PUBLISHED', INTERVAL '-1 day', INTERVAL '14 days',
         INTERVAL '-2 days', 100.00::NUMERIC,
         '{"shuffleQuestions":true,"maxAttempts":1,"showAnalysisAfterSubmit":false}'::JSONB),
        (821000000000000003::BIGINT, '小数加减预习任务',
         '用于下单元课前诊断，教师确认后再发布。',
         'UNIFIED', 'DRAFT', INTERVAL '2 days', INTERVAL '21 days',
         NULL, 100.00::NUMERIC,
         '{"shuffleQuestions":false,"maxAttempts":2,"showAnalysisAfterSubmit":true}'::JSONB)
)
INSERT INTO assignment (
    id, class_id, teacher_id, title, description, assignment_type, status,
    start_at, due_at, total_score, settings, published_at,
    created_at, updated_at, version
)
SELECT
    id, 820000000000000001, 810000000000000001,
    title, description, assignment_type, status,
    CURRENT_TIMESTAMP + start_offset,
    CURRENT_TIMESTAMP + due_offset,
    total_score, settings,
    CASE WHEN published_offset IS NULL THEN NULL ELSE CURRENT_TIMESTAMP + published_offset END,
    CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP, 0
FROM demo_assignment
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    description = EXCLUDED.description,
    assignment_type = EXCLUDED.assignment_type,
    status = EXCLUDED.status,
    start_at = EXCLUDED.start_at,
    due_at = EXCLUDED.due_at,
    total_score = EXCLUDED.total_score,
    settings = EXCLUDED.settings,
    published_at = EXCLUDED.published_at,
    updated_at = CURRENT_TIMESTAMP;

WITH assignment_seed (slot, assignment_id, layer_code, question_ids) AS (
    VALUES
        (1::BIGINT, 821000000000000001::BIGINT, 'ALL', ARRAY[
            930301020001::BIGINT, 930301020002::BIGINT, 930301020003::BIGINT,
            930301020004::BIGINT, 930301040001::BIGINT
        ]),
        (2::BIGINT, 821000000000000002::BIGINT, 'A', ARRAY[
            930301040001::BIGINT, 930301040002::BIGINT, 930301040003::BIGINT,
            930301040004::BIGINT, 930301060002::BIGINT
        ]),
        (3::BIGINT, 821000000000000002::BIGINT, 'B', ARRAY[
            930301020001::BIGINT, 930301020002::BIGINT, 930301040001::BIGINT,
            930301060001::BIGINT, 930301060002::BIGINT
        ]),
        (4::BIGINT, 821000000000000003::BIGINT, 'ALL', ARRAY[
            930302060001::BIGINT, 930302060002::BIGINT, 930302060003::BIGINT,
            930302060004::BIGINT, 930302020001::BIGINT
        ])
), expanded AS (
    SELECT seed.slot, seed.assignment_id, seed.layer_code,
           selected.question_id, selected.sequence_no
    FROM assignment_seed seed
    CROSS JOIN LATERAL unnest(seed.question_ids)
        WITH ORDINALITY AS selected(question_id, sequence_no)
)
INSERT INTO assignment_question (
    id, assignment_id, layer_code, question_id, sequence_no, score,
    question_snapshot, created_at, updated_at
)
SELECT
    822000000000000000 + expanded.slot * 100 + expanded.sequence_no,
    expanded.assignment_id,
    expanded.layer_code,
    expanded.question_id,
    expanded.sequence_no,
    20.00,
    pg_temp.demo_question_snapshot(question),
    CURRENT_TIMESTAMP - INTERVAL '18 days', CURRENT_TIMESTAMP
FROM expanded
JOIN question ON question.id = expanded.question_id
ON CONFLICT (id) DO UPDATE
SET question_id = EXCLUDED.question_id,
    sequence_no = EXCLUDED.sequence_no,
    score = EXCLUDED.score,
    question_snapshot = EXCLUDED.question_snapshot,
    updated_at = CURRENT_TIMESTAMP;

-- 3. Completed assignment tasks for every class member.
INSERT INTO practice_task (
    id, student_id, source_type, source_id, title, mode, status,
    question_count, time_limit_seconds, target_accuracy,
    started_at, completed_at, expires_at, created_at, updated_at, version
)
SELECT
    831000000000000000 + sequence_no,
    810000000000000100::BIGINT + sequence_no,
    'ASSIGNMENT', '821000000000000001', '三年级混合运算周测',
    'NORMAL', 'COMPLETED', 5, NULL, NULL,
    CURRENT_TIMESTAMP - make_interval(days => (14 - (sequence_no % 4))::INTEGER),
    CURRENT_TIMESTAMP - make_interval(days => (13 - (sequence_no % 4))::INTEGER),
    CURRENT_TIMESTAMP - INTERVAL '9 days',
    CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP, 0
FROM generate_series(1, 12) AS series(sequence_no)
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    status = 'COMPLETED',
    started_at = EXCLUDED.started_at,
    completed_at = EXCLUDED.completed_at,
    expires_at = EXCLUDED.expires_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO practice_task_question (
    id, task_id, question_id, sequence_no, question_snapshot, max_score,
    source_detail, created_at, updated_at
)
SELECT
    832000000000000000 + student.sequence_no * 10 + assignment_question.sequence_no,
    831000000000000000 + student.sequence_no,
    assignment_question.question_id,
    assignment_question.sequence_no,
    assignment_question.question_snapshot,
    assignment_question.score,
    jsonb_build_object('sourceType', 'ASSIGNMENT', 'assignmentId', 821000000000000001::BIGINT),
    CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP
FROM generate_series(1, 12) student(sequence_no)
CROSS JOIN assignment_question
WHERE assignment_question.assignment_id = 821000000000000001
  AND assignment_question.layer_code = 'ALL'
ON CONFLICT (id) DO UPDATE
SET question_snapshot = EXCLUDED.question_snapshot,
    max_score = EXCLUDED.max_score,
    source_detail = EXCLUDED.source_detail,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO practice_task_knowledge_point (
    task_id, knowledge_point_id, target_question_count, created_at, updated_at
)
SELECT DISTINCT
    831000000000000000 + student.sequence_no,
    relation.knowledge_point_id,
    1,
    CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP
FROM generate_series(1, 12) student(sequence_no)
CROSS JOIN assignment_question assignment_question
JOIN question_knowledge_point relation
  ON relation.question_id = assignment_question.question_id
WHERE assignment_question.assignment_id = 821000000000000001
  AND assignment_question.layer_code = 'ALL'
ON CONFLICT (task_id, knowledge_point_id) DO UPDATE
SET target_question_count = EXCLUDED.target_question_count,
    updated_at = CURRENT_TIMESTAMP;

WITH answer_seed AS (
    SELECT
        student.sequence_no AS student_no,
        task_question.id AS task_question_id,
        task_question.sequence_no,
        task_question.question_id,
        (task_question.sequence_no <= 3 + (student.sequence_no % 3)) AS is_correct
    FROM generate_series(1, 12) student(sequence_no)
    JOIN practice_task_question task_question
      ON task_question.task_id = 831000000000000000 + student.sequence_no
)
INSERT INTO answer_record (
    id, task_question_id, student_id, attempt_no, student_answer,
    grading_status, is_correct, score, response_ms, answer_source,
    recognition_confidence, submitted_at, graded_at, created_at, updated_at
)
SELECT
    833000000000000000 + answer_seed.student_no * 10 + answer_seed.sequence_no,
    answer_seed.task_question_id,
    810000000000000100::BIGINT + answer_seed.student_no,
    1,
    CASE WHEN answer_seed.is_correct
        THEN question.standard_answer
        ELSE jsonb_build_object('value', 0)
    END,
    'GRADED', answer_seed.is_correct,
    CASE WHEN answer_seed.is_correct THEN 20.00 ELSE 0.00 END,
    9000 + answer_seed.student_no * 700 + answer_seed.sequence_no * 850,
    'MANUAL', NULL,
    CURRENT_TIMESTAMP
        - make_interval(days => (13 - (answer_seed.student_no % 4))::INTEGER)
        + make_interval(mins => (answer_seed.sequence_no * 3)::INTEGER),
    CURRENT_TIMESTAMP
        - make_interval(days => (13 - (answer_seed.student_no % 4))::INTEGER)
        + make_interval(mins => (answer_seed.sequence_no * 3)::INTEGER),
    CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP
FROM answer_seed
JOIN question ON question.id = answer_seed.question_id
ON CONFLICT (id) DO UPDATE
SET student_answer = EXCLUDED.student_answer,
    grading_status = EXCLUDED.grading_status,
    is_correct = EXCLUDED.is_correct,
    score = EXCLUDED.score,
    response_ms = EXCLUDED.response_ms,
    submitted_at = EXCLUDED.submitted_at,
    graded_at = EXCLUDED.graded_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO assignment_submission (
    id, assignment_id, student_id, layer_code, practice_task_id, status,
    score, correct_count, total_count, started_at, submitted_at, graded_at,
    teacher_comment, created_at, updated_at, version
)
SELECT
    823000000000000000 + sequence_no,
    821000000000000001,
    810000000000000100::BIGINT + sequence_no,
    'ALL',
    831000000000000000 + sequence_no,
    'GRADED',
    (3 + (sequence_no % 3)) * 20.00,
    3 + (sequence_no % 3),
    5,
    CURRENT_TIMESTAMP - make_interval(days => (14 - (sequence_no % 4))::INTEGER),
    CURRENT_TIMESTAMP - make_interval(days => (13 - (sequence_no % 4))::INTEGER),
    CURRENT_TIMESTAMP - make_interval(days => (13 - (sequence_no % 4))::INTEGER),
    CASE
        WHEN sequence_no % 3 = 2 THEN '基础扎实，继续保持。'
        WHEN sequence_no % 3 = 1 THEN '注意审题并写清运算顺序。'
        ELSE '建议完成系统推荐的专项巩固。'
    END,
    CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP, 0
FROM generate_series(1, 12) AS series(sequence_no)
ON CONFLICT (id) DO UPDATE
SET practice_task_id = EXCLUDED.practice_task_id,
    status = EXCLUDED.status,
    score = EXCLUDED.score,
    correct_count = EXCLUDED.correct_count,
    total_count = EXCLUDED.total_count,
    started_at = EXCLUDED.started_at,
    submitted_at = EXCLUDED.submitted_at,
    graded_at = EXCLUDED.graded_at,
    teacher_comment = EXCLUDED.teacher_comment,
    updated_at = CURRENT_TIMESTAMP;

-- Current layered assignment: mixed statuses make the teacher page useful,
-- while the main student can still open and complete the assignment.
INSERT INTO assignment_submission (
    id, assignment_id, student_id, layer_code, practice_task_id, status,
    score, correct_count, total_count, started_at, submitted_at, graded_at,
    teacher_comment, created_at, updated_at, version
)
SELECT
    823000000000000100 + sequence_no,
    821000000000000002,
    810000000000000100::BIGINT + sequence_no,
    CASE WHEN sequence_no % 2 = 0 THEN 'A' ELSE 'B' END,
    NULL,
    CASE
        WHEN sequence_no = 1 OR sequence_no >= 10 THEN 'ASSIGNED'
        WHEN sequence_no BETWEEN 2 AND 4 THEN 'IN_PROGRESS'
        ELSE 'GRADED'
    END,
    CASE WHEN sequence_no BETWEEN 5 AND 9 THEN (3 + sequence_no % 3) * 20.00 ELSE NULL END,
    CASE WHEN sequence_no BETWEEN 5 AND 9 THEN 3 + sequence_no % 3 ELSE 0 END,
    5,
    CASE WHEN sequence_no BETWEEN 2 AND 9 THEN CURRENT_TIMESTAMP - INTERVAL '12 hours' ELSE NULL END,
    CASE WHEN sequence_no BETWEEN 5 AND 9 THEN CURRENT_TIMESTAMP - INTERVAL '4 hours' ELSE NULL END,
    CASE WHEN sequence_no BETWEEN 5 AND 9 THEN CURRENT_TIMESTAMP - INTERVAL '4 hours' ELSE NULL END,
    CASE WHEN sequence_no BETWEEN 5 AND 9 THEN '已完成本轮分层巩固。' ELSE NULL END,
    CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP, 0
FROM generate_series(1, 12) AS series(sequence_no)
ON CONFLICT (id) DO UPDATE
SET layer_code = EXCLUDED.layer_code,
    status = EXCLUDED.status,
    score = EXCLUDED.score,
    correct_count = EXCLUDED.correct_count,
    total_count = EXCLUDED.total_count,
    started_at = EXCLUDED.started_at,
    submitted_at = EXCLUDED.submitted_at,
    graded_at = EXCLUDED.graded_at,
    teacher_comment = EXCLUDED.teacher_comment,
    updated_at = CURRENT_TIMESTAMP;

-- 4. Extra learning history for the public student account.
WITH extra_task (
    id, source_type, source_id, title, mode, status, question_count,
    time_limit_seconds, target_accuracy, started_at, completed_at, expires_at, created_at
) AS (
    VALUES
        (831000000000000101::BIGINT, 'SELF', NULL, '多位数乘一位数专项练习',
         'SPECIAL', 'COMPLETED', 5::SMALLINT, NULL, 0.8000::NUMERIC,
         CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days' + INTERVAL '11 minutes',
         NULL, CURRENT_TIMESTAMP - INTERVAL '6 days'),
        (831000000000000102::BIGINT, 'SELF', NULL, '小数加减课前诊断',
         'TIMED', 'IN_PROGRESS', 5::SMALLINT, 600, 0.8000::NUMERIC,
         CURRENT_TIMESTAMP - INTERVAL '35 minutes', NULL,
         CURRENT_TIMESTAMP + INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '40 minutes'),
        (831000000000000201::BIGINT, 'GAME', '840000000000000001', '三年级计算闯关记录',
         'TIMED', 'COMPLETED', 5::SMALLINT, 300, NULL,
         CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '4 minutes',
         NULL, CURRENT_TIMESTAMP - INTERVAL '3 days')
)
INSERT INTO practice_task (
    id, student_id, source_type, source_id, title, mode, status,
    question_count, time_limit_seconds, target_accuracy,
    started_at, completed_at, expires_at, created_at, updated_at, version
)
SELECT
    id, 810000000000000101, source_type, source_id, title, mode, status,
    question_count, time_limit_seconds, target_accuracy,
    started_at, completed_at, expires_at, created_at, CURRENT_TIMESTAMP, 0
FROM extra_task
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    status = EXCLUDED.status,
    started_at = EXCLUDED.started_at,
    completed_at = EXCLUDED.completed_at,
    expires_at = EXCLUDED.expires_at,
    updated_at = CURRENT_TIMESTAMP;

WITH task_seed (slot, task_id, source_type, question_ids) AS (
    VALUES
        (1::BIGINT, 831000000000000101::BIGINT, 'SELF', ARRAY[
            930301040001::BIGINT, 930301040002::BIGINT, 930301040003::BIGINT,
            930301040004::BIGINT, 930301020002::BIGINT
        ]),
        (2::BIGINT, 831000000000000102::BIGINT, 'SELF', ARRAY[
            930302060001::BIGINT, 930302060002::BIGINT, 930302060003::BIGINT,
            930302060004::BIGINT, 930302020001::BIGINT
        ]),
        (3::BIGINT, 831000000000000201::BIGINT, 'GAME', ARRAY[
            930301020001::BIGINT, 930301020002::BIGINT, 930301020003::BIGINT,
            930301020004::BIGINT, 930301040001::BIGINT
        ])
), expanded AS (
    SELECT seed.slot, seed.task_id, seed.source_type,
           selected.question_id, selected.sequence_no
    FROM task_seed seed
    CROSS JOIN LATERAL unnest(seed.question_ids)
        WITH ORDINALITY AS selected(question_id, sequence_no)
)
INSERT INTO practice_task_question (
    id, task_id, question_id, sequence_no, question_snapshot,
    max_score, source_detail, created_at, updated_at
)
SELECT
    832000000000001000 + expanded.slot * 100 + expanded.sequence_no,
    expanded.task_id,
    expanded.question_id,
    expanded.sequence_no,
    pg_temp.demo_question_snapshot(question),
    1.00,
    jsonb_build_object('sourceType', expanded.source_type, 'demo', TRUE),
    CURRENT_TIMESTAMP - make_interval(days => (7 - expanded.slot)::INTEGER),
    CURRENT_TIMESTAMP
FROM expanded
JOIN question ON question.id = expanded.question_id
ON CONFLICT (id) DO UPDATE
SET question_snapshot = EXCLUDED.question_snapshot,
    source_detail = EXCLUDED.source_detail,
    updated_at = CURRENT_TIMESTAMP;

WITH task_seed (task_id, question_ids) AS (
    VALUES
        (831000000000000101::BIGINT, ARRAY[
            930301040001::BIGINT, 930301040002::BIGINT, 930301040003::BIGINT,
            930301040004::BIGINT, 930301020002::BIGINT
        ]),
        (831000000000000102::BIGINT, ARRAY[
            930302060001::BIGINT, 930302060002::BIGINT, 930302060003::BIGINT,
            930302060004::BIGINT, 930302020001::BIGINT
        ]),
        (831000000000000201::BIGINT, ARRAY[
            930301020001::BIGINT, 930301020002::BIGINT, 930301020003::BIGINT,
            930301020004::BIGINT, 930301040001::BIGINT
        ])
)
INSERT INTO practice_task_knowledge_point (
    task_id, knowledge_point_id, target_question_count, created_at, updated_at
)
SELECT DISTINCT
    task_seed.task_id,
    relation.knowledge_point_id,
    1,
    CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP
FROM task_seed
CROSS JOIN LATERAL unnest(task_seed.question_ids) AS selected(question_id)
JOIN question_knowledge_point relation ON relation.question_id = selected.question_id
ON CONFLICT (task_id, knowledge_point_id) DO UPDATE
SET target_question_count = EXCLUDED.target_question_count,
    updated_at = CURRENT_TIMESTAMP;

-- Completed self-practice: four correct answers and one diagnosed mistake.
INSERT INTO answer_record (
    id, task_question_id, student_id, attempt_no, student_answer,
    grading_status, is_correct, score, response_ms, answer_source,
    submitted_at, graded_at, created_at, updated_at
)
SELECT
    833000000000001100 + task_question.sequence_no,
    task_question.id,
    810000000000000101,
    1,
    CASE WHEN task_question.sequence_no <= 4
        THEN question.standard_answer
        ELSE jsonb_build_object('value', 42)
    END,
    'GRADED', task_question.sequence_no <= 4,
    CASE WHEN task_question.sequence_no <= 4 THEN 1.00 ELSE 0.00 END,
    10500 + task_question.sequence_no * 1200,
    'MANUAL',
    CURRENT_TIMESTAMP - INTERVAL '6 days' + make_interval(mins => (task_question.sequence_no * 2)::INTEGER),
    CURRENT_TIMESTAMP - INTERVAL '6 days' + make_interval(mins => (task_question.sequence_no * 2)::INTEGER),
    CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP
FROM practice_task_question task_question
JOIN question ON question.id = task_question.question_id
WHERE task_question.task_id = 831000000000000101
ON CONFLICT (id) DO UPDATE
SET student_answer = EXCLUDED.student_answer,
    is_correct = EXCLUDED.is_correct,
    score = EXCLUDED.score,
    response_ms = EXCLUDED.response_ms,
    submitted_at = EXCLUDED.submitted_at,
    graded_at = EXCLUDED.graded_at,
    updated_at = CURRENT_TIMESTAMP;

-- In-progress diagnostic: only the first two questions have answers.
INSERT INTO answer_record (
    id, task_question_id, student_id, attempt_no, student_answer,
    grading_status, is_correct, score, response_ms, answer_source,
    submitted_at, graded_at, created_at, updated_at
)
SELECT
    833000000000001200 + task_question.sequence_no,
    task_question.id,
    810000000000000101,
    1,
    CASE WHEN task_question.sequence_no = 1
        THEN question.standard_answer
        ELSE jsonb_build_object('value', 4.2)
    END,
    'GRADED', task_question.sequence_no = 1,
    CASE WHEN task_question.sequence_no = 1 THEN 1.00 ELSE 0.00 END,
    14000 + task_question.sequence_no * 1700,
    'MANUAL',
    CURRENT_TIMESTAMP - INTERVAL '30 minutes' + make_interval(mins => (task_question.sequence_no * 3)::INTEGER),
    CURRENT_TIMESTAMP - INTERVAL '30 minutes' + make_interval(mins => (task_question.sequence_no * 3)::INTEGER),
    CURRENT_TIMESTAMP - INTERVAL '30 minutes', CURRENT_TIMESTAMP
FROM practice_task_question task_question
JOIN question ON question.id = task_question.question_id
WHERE task_question.task_id = 831000000000000102
  AND task_question.sequence_no <= 2
ON CONFLICT (id) DO UPDATE
SET student_answer = EXCLUDED.student_answer,
    is_correct = EXCLUDED.is_correct,
    score = EXCLUDED.score,
    response_ms = EXCLUDED.response_ms,
    submitted_at = EXCLUDED.submitted_at,
    graded_at = EXCLUDED.graded_at,
    updated_at = CURRENT_TIMESTAMP;

-- Game answers are also synchronized into the unified learning history.
INSERT INTO answer_record (
    id, task_question_id, student_id, attempt_no, student_answer,
    grading_status, is_correct, score, response_ms, answer_source,
    submitted_at, graded_at, created_at, updated_at
)
SELECT
    833000000000001300 + task_question.sequence_no,
    task_question.id,
    810000000000000101,
    1,
    CASE WHEN task_question.sequence_no <= 4
        THEN question.standard_answer
        ELSE jsonb_build_object('value', 1600)
    END,
    'GRADED', task_question.sequence_no <= 4,
    CASE WHEN task_question.sequence_no <= 4 THEN 1.00 ELSE 0.00 END,
    7000 + task_question.sequence_no * 900,
    'GAME',
    CURRENT_TIMESTAMP - INTERVAL '3 days' + make_interval(secs => (task_question.sequence_no * 35)::DOUBLE PRECISION),
    CURRENT_TIMESTAMP - INTERVAL '3 days' + make_interval(secs => (task_question.sequence_no * 35)::DOUBLE PRECISION),
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP
FROM practice_task_question task_question
JOIN question ON question.id = task_question.question_id
WHERE task_question.task_id = 831000000000000201
ON CONFLICT (id) DO UPDATE
SET student_answer = EXCLUDED.student_answer,
    is_correct = EXCLUDED.is_correct,
    score = EXCLUDED.score,
    response_ms = EXCLUDED.response_ms,
    submitted_at = EXCLUDED.submitted_at,
    graded_at = EXCLUDED.graded_at,
    updated_at = CURRENT_TIMESTAMP;

-- 5. Mastery, error diagnosis, and personalized recommendations.
WITH mastery_seed AS (
    SELECT
        student_no,
        knowledge_point_id,
        point_no,
        (10 + ((student_no + point_no) % 8))::INTEGER AS total_count,
        (0.50 + (((student_no + point_no) % 5)::NUMERIC / 10))::NUMERIC(5,4) AS accuracy,
        (0.35 + (((student_no * 11 + point_no * 7) % 55)::NUMERIC / 100))::NUMERIC(5,4)
            AS mastery
    FROM generate_series(1, 12) AS student_series(student_no)
    CROSS JOIN (
        VALUES
            (1, 9203010201::BIGINT),
            (2, 9203010202::BIGINT),
            (3, 9203010203::BIGINT),
            (4, 9203010401::BIGINT),
            (5, 9203010404::BIGINT)
    ) point(point_no, knowledge_point_id)
)
INSERT INTO mastery_record (
    id, student_id, knowledge_point_id, mastery_score, accuracy_rate,
    avg_response_ms, total_answer_count, correct_answer_count,
    consecutive_correct_count, last_practiced_at, next_review_at,
    created_at, updated_at, version
)
SELECT
    836000000000000000 + student_no * 10 + point_no,
    810000000000000100::BIGINT + student_no,
    knowledge_point_id,
    mastery,
    accuracy,
    12000 + student_no * 450 + point_no * 800,
    total_count,
    FLOOR(total_count * accuracy)::INTEGER,
    (student_no + point_no) % 4,
    CURRENT_TIMESTAMP - make_interval(days => ((student_no + point_no) % 7)::INTEGER),
    CURRENT_TIMESTAMP + CASE
        WHEN mastery < 0.6000 THEN INTERVAL '1 day'
        WHEN mastery < 0.8000 THEN INTERVAL '3 days'
        ELSE INTERVAL '7 days'
    END,
    CURRENT_TIMESTAMP - INTERVAL '60 days', CURRENT_TIMESTAMP, 0
FROM mastery_seed
ON CONFLICT (id) DO UPDATE
SET mastery_score = EXCLUDED.mastery_score,
    accuracy_rate = EXCLUDED.accuracy_rate,
    avg_response_ms = EXCLUDED.avg_response_ms,
    total_answer_count = EXCLUDED.total_answer_count,
    correct_answer_count = EXCLUDED.correct_answer_count,
    consecutive_correct_count = EXCLUDED.consecutive_correct_count,
    last_practiced_at = EXCLUDED.last_practiced_at,
    next_review_at = EXCLUDED.next_review_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO mastery_change_log (
    id, mastery_id, answer_record_id, before_score, after_score,
    delta_score, reason, occurred_at, created_at, updated_at
)
SELECT
    837000000000000000 + point_no,
    mastery.id,
    NULL,
    GREATEST(0, mastery.mastery_score - 0.0800),
    mastery.mastery_score,
    LEAST(0.0800, mastery.mastery_score),
    'REVIEW',
    CURRENT_TIMESTAMP - make_interval(days => (8 - point_no)::INTEGER),
    CURRENT_TIMESTAMP - make_interval(days => (8 - point_no)::INTEGER),
    CURRENT_TIMESTAMP
FROM generate_series(1, 5) AS point_series(point_no)
JOIN mastery_record mastery
  ON mastery.id = 836000000000000010 + point_no
ON CONFLICT (id) DO UPDATE
SET before_score = EXCLUDED.before_score,
    after_score = EXCLUDED.after_score,
    delta_score = EXCLUDED.delta_score,
    occurred_at = EXCLUDED.occurred_at,
    updated_at = CURRENT_TIMESTAMP;

WITH wrong_answer AS (
    SELECT
        answer.id AS answer_id,
        answer.student_id,
        task_question.question_id,
        task_question.sequence_no,
        ROW_NUMBER() OVER (ORDER BY answer.student_id, task_question.sequence_no) AS error_no,
        (
            SELECT relation.knowledge_point_id
            FROM question_knowledge_point relation
            WHERE relation.question_id = task_question.question_id
            ORDER BY relation.is_primary DESC, relation.knowledge_point_id
            LIMIT 1
        ) AS knowledge_point_id
    FROM answer_record answer
    JOIN practice_task_question task_question ON task_question.id = answer.task_question_id
    WHERE task_question.task_id BETWEEN 831000000000000001 AND 831000000000000012
      AND answer.is_correct = FALSE
)
INSERT INTO error_record (
    id, answer_record_id, student_id, question_id, knowledge_point_id,
    error_type_id, error_position, diagnosis, confidence, status,
    resolved_by_answer_id, created_at, updated_at, resolved_at
)
SELECT
    834000000000000000 + error_no,
    answer_id,
    student_id,
    question_id,
    knowledge_point_id,
    CASE
        WHEN sequence_no IN (2, 3) THEN 94000025
        WHEN sequence_no = 4 THEN 94000011
        ELSE 94000001
    END,
    '第 ' || sequence_no || ' 题',
    CASE
        WHEN sequence_no IN (2, 3) THEN '混合运算顺序判断不够稳定，建议先标出第一步。'
        WHEN sequence_no = 4 THEN '实际问题中的数量关系选择有误，建议圈出关键条件。'
        ELSE '计算结果有误，建议检查口算过程。'
    END,
    0.9200,
    'OPEN', NULL,
    CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP, NULL
FROM wrong_answer
ON CONFLICT (id) DO UPDATE
SET diagnosis = EXCLUDED.diagnosis,
    confidence = EXCLUDED.confidence,
    status = 'OPEN',
    resolved_by_answer_id = NULL,
    resolved_at = NULL,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO error_record (
    id, answer_record_id, student_id, question_id, knowledge_point_id,
    error_type_id, error_position, diagnosis, confidence, status,
    resolved_by_answer_id, created_at, updated_at, resolved_at
)
SELECT
    834000000000000101,
    answer.id,
    810000000000000101,
    task_question.question_id,
    9203010202,
    94000025,
    '乘法运算步骤',
    '先加后乘导致结果偏小，建议先圈出乘法并完成第一步。',
    0.9600,
    'RETRAINING', NULL,
    CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP, NULL
FROM answer_record answer
JOIN practice_task_question task_question ON task_question.id = answer.task_question_id
WHERE task_question.task_id = 831000000000000101
  AND task_question.sequence_no = 5
ON CONFLICT (id) DO UPDATE
SET diagnosis = EXCLUDED.diagnosis,
    status = 'RETRAINING',
    updated_at = CURRENT_TIMESTAMP;

WITH recommendation_seed (
    id, knowledge_point_id, source_type, source_id, reason_text,
    priority, question_count, status, generated_task_id,
    generated_at, expires_at, accepted_at, completed_at
) AS (
    VALUES
        (835000000000000001::BIGINT, 9203010202::BIGINT, 'ERROR',
         834000000000000101::BIGINT,
         '混合运算顺序近期连续失分，建议完成 5 道针对性巩固。',
         5::SMALLINT, 5::SMALLINT, 'PENDING', NULL,
         CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP + INTERVAL '7 days', NULL, NULL),
        (835000000000000002::BIGINT, 9203010404::BIGINT, 'MASTERY',
         836000000000000015::BIGINT,
         '连续进位乘法掌握度低于班级平均值，建议复习竖式进位。',
         4::SMALLINT, 5::SMALLINT, 'PENDING', NULL,
         CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP + INTERVAL '10 days', NULL, NULL),
        (835000000000000003::BIGINT, 9203010401::BIGINT, 'REVIEW',
         836000000000000014::BIGINT,
         '已完成整十整百数乘法的间隔复习。',
         3::SMALLINT, 5::SMALLINT, 'COMPLETED', 831000000000000101::BIGINT,
         CURRENT_TIMESTAMP - INTERVAL '9 days', CURRENT_TIMESTAMP + INTERVAL '20 days',
         CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '6 days')
)
INSERT INTO recommendation (
    id, student_id, knowledge_point_id, source_type, source_id, reason_text,
    priority, recommended_question_count, status, generated_task_id,
    generated_at, expires_at, accepted_at, completed_at, created_at, updated_at
)
SELECT
    id, 810000000000000101, knowledge_point_id, source_type, source_id,
    reason_text, priority, question_count, status, generated_task_id,
    generated_at, expires_at, accepted_at, completed_at, generated_at, CURRENT_TIMESTAMP
FROM recommendation_seed
ON CONFLICT (id) DO UPDATE
SET reason_text = EXCLUDED.reason_text,
    priority = EXCLUDED.priority,
    recommended_question_count = EXCLUDED.recommended_question_count,
    status = EXCLUDED.status,
    generated_task_id = EXCLUDED.generated_task_id,
    generated_at = EXCLUDED.generated_at,
    expires_at = EXCLUDED.expires_at,
    accepted_at = EXCLUDED.accepted_at,
    completed_at = EXCLUDED.completed_at,
    updated_at = CURRENT_TIMESTAMP;

-- 6. A completed game session linked to the learning history.
INSERT INTO game_session (
    id, mode, level_id, creator_student_id, match_mode, invite_code, status,
    member_count, grade_level_snapshot, knowledge_point_id_snapshot,
    difficulty_snapshot, question_count_snapshot, time_limit_seconds_snapshot,
    pass_rate_snapshot, progress_value, winner_student_id, result_type,
    started_at, ended_at, expires_at, settled_at, result_detail,
    created_at, updated_at, version
) VALUES (
    840000000000000001, 'LEVEL', 95000003, 810000000000000101,
    NULL, NULL, 'COMPLETED', 1, 3, 9203010201, 2, 5, 300,
    0.8000, 100.00, NULL, 'LEVEL_PASSED',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '4 minutes',
    NULL,
    CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '4 minutes',
    '{"correctCount":4,"starCount":2,"durationSeconds":238}'::JSONB,
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP, 0
)
ON CONFLICT (id) DO UPDATE
SET status = EXCLUDED.status,
    progress_value = EXCLUDED.progress_value,
    result_type = EXCLUDED.result_type,
    started_at = EXCLUDED.started_at,
    ended_at = EXCLUDED.ended_at,
    settled_at = EXCLUDED.settled_at,
    result_detail = EXCLUDED.result_detail,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO game_session_member (
    session_id, student_id, seat_no, member_role, status, result,
    practice_task_id, answered_count, correct_count, score,
    contribution_value, current_combo, max_combo, total_response_ms,
    rating_before, rating_after, points_reward, experience_reward,
    joined_at, last_answered_at, finished_at, created_at, updated_at
) VALUES (
    840000000000000001, 810000000000000101, 1, 'CREATOR', 'FINISHED', 'PASSED',
    831000000000000201, 5, 4, 4.00, 100.00, 0, 4, 57500,
    NULL, NULL, 70, 40,
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '4 minutes',
    CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '4 minutes',
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP
)
ON CONFLICT (session_id, student_id) DO UPDATE
SET status = EXCLUDED.status,
    result = EXCLUDED.result,
    practice_task_id = EXCLUDED.practice_task_id,
    answered_count = EXCLUDED.answered_count,
    correct_count = EXCLUDED.correct_count,
    score = EXCLUDED.score,
    max_combo = EXCLUDED.max_combo,
    points_reward = EXCLUDED.points_reward,
    experience_reward = EXCLUDED.experience_reward,
    finished_at = EXCLUDED.finished_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO game_session_question (
    id, session_id, student_id, sequence_no, question_id, question_snapshot,
    max_score, grading_status, student_answer, answer_request_id,
    answer_record_id, is_correct, score, response_ms, combo_after,
    progress_delta, energy_delta, answered_at, created_at, updated_at
)
SELECT
    841000000000000000 + task_question.sequence_no,
    840000000000000001,
    810000000000000101,
    task_question.sequence_no,
    task_question.question_id,
    task_question.question_snapshot,
    1.00,
    'GRADED',
    answer.student_answer,
    'demo-game-2026-answer-' || task_question.sequence_no,
    answer.id,
    answer.is_correct,
    answer.score,
    answer.response_ms,
    CASE WHEN answer.is_correct THEN task_question.sequence_no ELSE 0 END,
    20.00,
    CASE WHEN answer.is_correct THEN 10 ELSE 0 END,
    answer.submitted_at,
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP
FROM practice_task_question task_question
JOIN answer_record answer
  ON answer.task_question_id = task_question.id
 AND answer.student_id = 810000000000000101
WHERE task_question.task_id = 831000000000000201
ON CONFLICT (id) DO UPDATE
SET question_snapshot = EXCLUDED.question_snapshot,
    student_answer = EXCLUDED.student_answer,
    answer_record_id = EXCLUDED.answer_record_id,
    is_correct = EXCLUDED.is_correct,
    score = EXCLUDED.score,
    response_ms = EXCLUDED.response_ms,
    combo_after = EXCLUDED.combo_after,
    answered_at = EXCLUDED.answered_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO game_player_progress (
    student_id, total_points, experience, player_level, total_sessions,
    success_sessions, level_passed_count, pk_sessions, pk_wins, pk_losses,
    pk_draws, pk_rating, current_win_streak, best_win_streak, best_combo,
    last_played_at, created_at, updated_at, version
) VALUES (
    810000000000000101, 270, 140, 2, 4, 3, 2,
    1, 1, 0, 0, 1028, 2, 2, 4,
    CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '30 days',
    CURRENT_TIMESTAMP, 0
)
ON CONFLICT (student_id) DO UPDATE
SET total_points = EXCLUDED.total_points,
    experience = EXCLUDED.experience,
    player_level = EXCLUDED.player_level,
    total_sessions = EXCLUDED.total_sessions,
    success_sessions = EXCLUDED.success_sessions,
    level_passed_count = EXCLUDED.level_passed_count,
    pk_sessions = EXCLUDED.pk_sessions,
    pk_wins = EXCLUDED.pk_wins,
    pk_losses = EXCLUDED.pk_losses,
    pk_draws = EXCLUDED.pk_draws,
    pk_rating = EXCLUDED.pk_rating,
    current_win_streak = EXCLUDED.current_win_streak,
    best_win_streak = EXCLUDED.best_win_streak,
    best_combo = EXCLUDED.best_combo,
    last_played_at = EXCLUDED.last_played_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO game_level_progress (
    student_id, level_id, status, attempt_count, best_score,
    best_correct_count, best_star_count, best_duration_seconds,
    last_session_id, first_unlocked_at, first_passed_at, last_played_at,
    created_at, updated_at
) VALUES (
    810000000000000101, 95000003, 'PASSED', 2, 4.00,
    4, 2, 238, 840000000000000001,
    CURRENT_TIMESTAMP - INTERVAL '15 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '3 days',
    CURRENT_TIMESTAMP - INTERVAL '15 days', CURRENT_TIMESTAMP
)
ON CONFLICT (student_id, level_id) DO UPDATE
SET status = EXCLUDED.status,
    attempt_count = EXCLUDED.attempt_count,
    best_score = EXCLUDED.best_score,
    best_correct_count = EXCLUDED.best_correct_count,
    best_star_count = EXCLUDED.best_star_count,
    best_duration_seconds = EXCLUDED.best_duration_seconds,
    last_session_id = EXCLUDED.last_session_id,
    first_passed_at = EXCLUDED.first_passed_at,
    last_played_at = EXCLUDED.last_played_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO game_reward_record (
    id, student_id, session_id, business_type, reward_type,
    amount, balance_after, reason, idempotency_key, created_at, updated_at
) VALUES
    (842000000000000001, 810000000000000101, 840000000000000001,
     'LEVEL', 'POINTS', 70, 270, 'LEVEL_PASSED', 'demo-level-840000000000000001-points',
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),
    (842000000000000002, 810000000000000101, 840000000000000001,
     'LEVEL', 'EXPERIENCE', 40, 140, 'LEVEL_PASSED', 'demo-level-840000000000000001-exp',
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET amount = EXCLUDED.amount,
    balance_after = EXCLUDED.balance_after,
    reason = EXCLUDED.reason,
    updated_at = CURRENT_TIMESTAMP;

-- 7. Notifications, content feedback, and audit history for non-empty pages.
INSERT INTO notification (
    id, user_id, type, title, content, is_read, read_at,
    source_event_id, created_at, updated_at
) VALUES
    ('85000000-0000-0000-0000-000000000001', 810000000000000101,
     'ASSIGNMENT_PUBLISHED', '新的分层作业已发布',
     '王老师发布了“薄弱知识点分层巩固”，截止时间为两周后。',
     FALSE, NULL, '85000000-0000-0000-1000-000000000001',
     CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP),
    ('85000000-0000-0000-0000-000000000002', 810000000000000101,
     'LEARNING_RECOMMENDATION', '你有新的巩固建议',
     '系统根据最近错题生成了混合运算顺序专项练习建议。',
     FALSE, NULL, '85000000-0000-0000-1000-000000000002',
     CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP),
    ('85000000-0000-0000-0000-000000000003', 810000000000000101,
     'GAME_REWARD', '闯关奖励到账',
     '三年级计算起步闯关成功，获得 70 积分和 40 经验。',
     TRUE, CURRENT_TIMESTAMP - INTERVAL '3 days' + INTERVAL '10 minutes',
     '85000000-0000-0000-1000-000000000003',
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),
    ('85000000-0000-0000-0000-000000000004', 810000000000000001,
     'CLASS_ALERT', '班级学情提醒',
     '三年级（1）班有 4 名学生的混合运算掌握度低于 60%。',
     FALSE, NULL, '85000000-0000-0000-1000-000000000004',
     CURRENT_TIMESTAMP - INTERVAL '8 hours', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET title = EXCLUDED.title,
    content = EXCLUDED.content,
    is_read = EXCLUDED.is_read,
    read_at = EXCLUDED.read_at,
    created_at = EXCLUDED.created_at,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO question_feedback (
    id, question_id, reporter_id, category, description, status,
    handler_id, handle_comment, handled_at, created_at, updated_at, version
) VALUES
    (860000000000000001, 930301020004, 810000000000000101,
     'AMBIGUOUS', '建议在题干中明确“又放入”的球不属于原来的 4 盒。',
     'PENDING', NULL, NULL, NULL,
     CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP, 0)
ON CONFLICT (id) DO UPDATE
SET description = EXCLUDED.description,
    status = 'PENDING',
    handler_id = NULL,
    handle_comment = NULL,
    handled_at = NULL,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO audit_log (
    id, event_id, operator_id, source, action, target_type,
    target_id, detail, occurred_at, created_at, updated_at
) VALUES
    ('87000000-0000-0000-0000-000000000001', '87000000-0000-0000-1000-000000000001',
     810000000000000001, 'teaching-service', 'CLASS_CREATED', 'TEACHING_CLASS',
     '820000000000000001', '创建演示班级“三年级（1）班”',
     CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP - INTERVAL '90 days', CURRENT_TIMESTAMP),
    ('87000000-0000-0000-0000-000000000002', '87000000-0000-0000-1000-000000000002',
     810000000000000001, 'teaching-service', 'ASSIGNMENT_PUBLISHED', 'ASSIGNMENT',
     '821000000000000001', '发布“三年级混合运算周测”',
     CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP - INTERVAL '16 days', CURRENT_TIMESTAMP),
    ('87000000-0000-0000-0000-000000000003', '87000000-0000-0000-1000-000000000003',
     810000000000000101, 'learning-service', 'PRACTICE_COMPLETED', 'PRACTICE_TASK',
     '831000000000000101', '完成多位数乘一位数专项练习，正确率 80%',
     CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP - INTERVAL '6 days', CURRENT_TIMESTAMP),
    ('87000000-0000-0000-0000-000000000004', '87000000-0000-0000-1000-000000000004',
     810000000000000101, 'game-service', 'LEVEL_PASSED', 'GAME_SESSION',
     '840000000000000001', '通过三年级计算起步关卡并获得 2 星',
     CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP),
    ('87000000-0000-0000-0000-000000000005', '87000000-0000-0000-1000-000000000005',
     810000000000000001, 'teaching-service', 'ASSIGNMENT_PUBLISHED', 'ASSIGNMENT',
     '821000000000000002', '发布“薄弱知识点分层巩固”',
     CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP),
    ('87000000-0000-0000-0000-000000000006', '87000000-0000-0000-1000-000000000006',
     810000000000000101, 'content-service', 'QUESTION_FEEDBACK_CREATED', 'QUESTION',
     '930301020004', '提交题目表述建议',
     CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP - INTERVAL '5 days', CURRENT_TIMESTAMP)
ON CONFLICT (id) DO UPDATE
SET detail = EXCLUDED.detail,
    occurred_at = EXCLUDED.occurred_at,
    updated_at = CURRENT_TIMESTAMP;

COMMIT;
