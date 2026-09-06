-- game_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE game_level (
id                 BIGINT        CONSTRAINT pk_game_level PRIMARY KEY,
code               VARCHAR(64)   NOT NULL,
name               VARCHAR(100)  NOT NULL,
description        VARCHAR(500),
grade_level        SMALLINT      NOT NULL,
knowledge_point_id BIGINT        NOT NULL,
mode               VARCHAR(16)   NOT NULL DEFAULT 'BOTH',
question_count     SMALLINT      NOT NULL DEFAULT 10,
time_limit_seconds INTEGER       NOT NULL,
pass_rate          NUMERIC(5,4)  NOT NULL DEFAULT 0.8000,
min_players        SMALLINT      NOT NULL DEFAULT 1,
max_players        SMALLINT      NOT NULL DEFAULT 4,
base_points        INTEGER       NOT NULL DEFAULT 0,
base_experience    INTEGER       NOT NULL DEFAULT 0,
previous_level_id  BIGINT,
sort_order         INTEGER       NOT NULL DEFAULT 0,
status             VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
rule_config        JSONB,
created_by         BIGINT        NOT NULL,
created_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at         TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version            INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_game_level_code UNIQUE (code),
CONSTRAINT ck_game_level_not_self CHECK
(previous_level_id IS NULL OR previous_level_id <> id),
CONSTRAINT ck_game_level_grade CHECK (grade_level BETWEEN 1 AND 6),
CONSTRAINT ck_game_level_mode CHECK (mode IN ('SOLO', 'TEAM', 'BOTH')),
CONSTRAINT ck_game_level_question_count CHECK (question_count > 0),
CONSTRAINT ck_game_level_time CHECK (time_limit_seconds > 0),
CONSTRAINT ck_game_level_pass_rate CHECK (pass_rate > 0 AND pass_rate <= 1),
CONSTRAINT ck_game_level_players CHECK
(min_players BETWEEN 1 AND 4 AND max_players BETWEEN 1 AND 4
AND min_players <= max_players
AND (mode <> 'SOLO' OR (min_players = 1 AND max_players = 1))
AND (mode <> 'TEAM' OR min_players >= 2)),
CONSTRAINT ck_game_level_rewards CHECK
(base_points >= 0 AND base_experience >= 0),
CONSTRAINT ck_game_level_sort CHECK (sort_order >= 0),
CONSTRAINT ck_game_level_status CHECK (status IN ('DRAFT', 'ENABLED', 'DISABLED')),
CONSTRAINT ck_game_level_version CHECK (version >= 0)
);
CREATE INDEX idx_game_level_grade_sort
ON game_level (grade_level, sort_order)
WHERE status = 'ENABLED';
CREATE INDEX idx_game_level_knowledge
ON game_level (knowledge_point_id, status);
CREATE INDEX idx_game_level_previous_level_id
ON game_level (previous_level_id);
COMMENT ON TABLE game_level IS '可配置游戏关卡';
COMMENT ON COLUMN game_level.id IS '关卡编号';
COMMENT ON COLUMN game_level.code IS '稳定关卡编码';
COMMENT ON COLUMN game_level.name IS '关卡名称';
COMMENT ON COLUMN game_level.description IS '关卡说明';
COMMENT ON COLUMN game_level.grade_level IS '适用年级';
COMMENT ON COLUMN game_level.knowledge_point_id IS '主要知识点';
COMMENT ON COLUMN game_level.mode IS '支持模式';
COMMENT ON COLUMN game_level.question_count IS '默认题数';
COMMENT ON COLUMN game_level.time_limit_seconds IS '总时限';
COMMENT ON COLUMN game_level.pass_rate IS '通过正确率';
COMMENT ON COLUMN game_level.min_players IS '最少参与人数';
COMMENT ON COLUMN game_level.max_players IS '最多参与人数';
COMMENT ON COLUMN game_level.base_points IS '基础积分奖励';
COMMENT ON COLUMN game_level.base_experience IS '基础经验奖励';
COMMENT ON COLUMN game_level.previous_level_id IS '前置关卡';
COMMENT ON COLUMN game_level.sort_order IS '展示顺序';
COMMENT ON COLUMN game_level.status IS '关卡状态';
COMMENT ON COLUMN game_level.rule_config IS '连击、能量、推进等规则';
COMMENT ON COLUMN game_level.created_by IS '创建人';
COMMENT ON COLUMN game_level.created_at IS '创建时间';
COMMENT ON COLUMN game_level.updated_at IS '更新时间';
COMMENT ON COLUMN game_level.version IS '乐观锁版本号';

CREATE TABLE game_session (
id                          BIGINT        CONSTRAINT pk_game_session PRIMARY KEY,
level_id                    BIGINT        NOT NULL,
mode                        VARCHAR(16)   NOT NULL,
creator_student_id          BIGINT        NOT NULL,
status                      VARCHAR(16)   NOT NULL DEFAULT 'WAITING',
member_count                SMALLINT      NOT NULL DEFAULT 1,
question_count_snapshot     SMALLINT      NOT NULL,
time_limit_seconds_snapshot INTEGER       NOT NULL,
pass_rate_snapshot          NUMERIC(5,4)  NOT NULL,
team_progress               NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
started_at                  TIMESTAMPTZ,
ended_at                    TIMESTAMPTZ,
result_detail               JSONB,
created_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                  TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
version                     INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT ck_game_session_mode CHECK (mode IN ('SOLO', 'TEAM')),
CONSTRAINT ck_game_session_status CHECK (status IN
('WAITING', 'IN_PROGRESS', 'SUCCESS', 'FAILED', 'CANCELLED')),
CONSTRAINT ck_game_session_members CHECK
(member_count BETWEEN 1 AND 4
AND (mode <> 'SOLO' OR member_count = 1)
AND (mode <> 'TEAM' OR status = 'WAITING' OR member_count BETWEEN 2 AND 4)),
CONSTRAINT ck_game_session_question_count CHECK (question_count_snapshot > 0),
CONSTRAINT ck_game_session_time CHECK (time_limit_seconds_snapshot > 0),
CONSTRAINT ck_game_session_pass_rate CHECK
(pass_rate_snapshot > 0 AND pass_rate_snapshot <= 1),
CONSTRAINT ck_game_session_progress CHECK (team_progress >= 0),
CONSTRAINT ck_game_session_version CHECK (version >= 0)
);
CREATE INDEX idx_game_session_creator_created
ON game_session (creator_student_id, created_at DESC);
CREATE INDEX idx_game_session_level_status
ON game_session (level_id, status, created_at DESC);
COMMENT ON TABLE game_session IS '单人或团队闯关对局';
COMMENT ON COLUMN game_session.id IS '对局编号';
COMMENT ON COLUMN game_session.level_id IS '关卡编号';
COMMENT ON COLUMN game_session.mode IS '实际对局模式';
COMMENT ON COLUMN game_session.creator_student_id IS '发起学生';
COMMENT ON COLUMN game_session.status IS '对局状态/结果';
COMMENT ON COLUMN game_session.member_count IS '成员数快照';
COMMENT ON COLUMN game_session.question_count_snapshot IS '本局题数规则快照';
COMMENT ON COLUMN game_session.time_limit_seconds_snapshot IS '本局时限快照';
COMMENT ON COLUMN game_session.pass_rate_snapshot IS '本局通过率快照';
COMMENT ON COLUMN game_session.team_progress IS '队伍推进值';
COMMENT ON COLUMN game_session.started_at IS '开始时间';
COMMENT ON COLUMN game_session.ended_at IS '结束时间';
COMMENT ON COLUMN game_session.result_detail IS '结算规则与结果快照';
COMMENT ON COLUMN game_session.created_at IS '创建时间';
COMMENT ON COLUMN game_session.updated_at IS '更新时间';
COMMENT ON COLUMN game_session.version IS '乐观锁版本号';

CREATE TABLE game_session_member (
session_id         BIGINT        NOT NULL,
student_id         BIGINT        NOT NULL,
seat_no            SMALLINT      NOT NULL,
status             VARCHAR(16)   NOT NULL DEFAULT 'JOINED',
practice_task_id   BIGINT,
answered_count     SMALLINT      NOT NULL DEFAULT 0,
correct_count      SMALLINT      NOT NULL DEFAULT 0,
score              NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
contribution_value NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
max_combo          SMALLINT      NOT NULL DEFAULT 0,
joined_at          TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
finished_at        TIMESTAMPTZ,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_game_session_member PRIMARY KEY (session_id, student_id),
CONSTRAINT uk_game_session_member_seat UNIQUE (session_id, seat_no),
CONSTRAINT ck_game_session_member_seat CHECK (seat_no BETWEEN 1 AND 4),
CONSTRAINT ck_game_session_member_status CHECK
(status IN ('JOINED', 'READY', 'PLAYING', 'FINISHED', 'LEFT')),
CONSTRAINT ck_game_session_member_counts CHECK
(answered_count >= 0 AND correct_count >= 0 AND
correct_count <= answered_count AND max_combo >= 0),
CONSTRAINT ck_game_session_member_values CHECK
(score >= 0 AND contribution_value >= 0)
);
CREATE INDEX idx_game_session_member_student
ON game_session_member (student_id, joined_at DESC);
COMMENT ON TABLE game_session_member IS '对局成员及个人结算快照';
COMMENT ON COLUMN game_session_member.session_id IS '对局编号';
COMMENT ON COLUMN game_session_member.student_id IS '学生编号';
COMMENT ON COLUMN game_session_member.seat_no IS '成员位置';
COMMENT ON COLUMN game_session_member.status IS '成员状态';
COMMENT ON COLUMN game_session_member.practice_task_id IS '为本成员生成的等价题组';
COMMENT ON COLUMN game_session_member.answered_count IS '已答题数';
COMMENT ON COLUMN game_session_member.correct_count IS '答对题数';
COMMENT ON COLUMN game_session_member.score IS '个人得分';
COMMENT ON COLUMN game_session_member.contribution_value IS '团队贡献值';
COMMENT ON COLUMN game_session_member.max_combo IS '最大连击';
COMMENT ON COLUMN game_session_member.joined_at IS '加入时间';
COMMENT ON COLUMN game_session_member.finished_at IS '完成时间';
COMMENT ON COLUMN game_session_member.created_at IS '创建时间';
COMMENT ON COLUMN game_session_member.updated_at IS '更新时间';

CREATE TABLE game_session_question (
id                BIGINT        CONSTRAINT pk_game_session_question PRIMARY KEY,
session_id        BIGINT        NOT NULL,
student_id        BIGINT        NOT NULL,
sequence_no       SMALLINT      NOT NULL,
question_id       BIGINT,
question_snapshot JSONB         NOT NULL,
answer_record_id  BIGINT,
is_correct        BOOLEAN,
progress_delta    NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
energy_delta      INTEGER       NOT NULL DEFAULT 0,
answered_at       TIMESTAMPTZ,
created_at        TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_game_session_question_seq
UNIQUE (session_id, student_id, sequence_no),
CONSTRAINT uk_game_session_question_answer UNIQUE (answer_record_id),
CONSTRAINT ck_game_session_question_seq CHECK (sequence_no > 0),
CONSTRAINT ck_game_session_question_progress CHECK (progress_delta >= 0)
);
CREATE INDEX idx_game_session_question_member
ON game_session_question (session_id, student_id, sequence_no);
COMMENT ON TABLE game_session_question IS '游戏推进所需的题目过程';
COMMENT ON COLUMN game_session_question.id IS '游戏题目过程编号';
COMMENT ON COLUMN game_session_question.session_id IS '对局编号';
COMMENT ON COLUMN game_session_question.student_id IS '学生编号';
COMMENT ON COLUMN game_session_question.sequence_no IS '题目顺序';
COMMENT ON COLUMN game_session_question.question_id IS '固定/模板题编号';
COMMENT ON COLUMN game_session_question.question_snapshot IS '本成员实际题目快照';
COMMENT ON COLUMN game_session_question.answer_record_id IS '统一作答记录';
COMMENT ON COLUMN game_session_question.is_correct IS '游戏结算使用的正确性快照';
COMMENT ON COLUMN game_session_question.progress_delta IS '本题推进值';
COMMENT ON COLUMN game_session_question.energy_delta IS '本题能量变化';
COMMENT ON COLUMN game_session_question.answered_at IS '作答时间';
COMMENT ON COLUMN game_session_question.created_at IS '创建时间';
COMMENT ON COLUMN game_session_question.updated_at IS '更新时间';

CREATE TABLE game_player_progress (
student_id      BIGINT    CONSTRAINT pk_game_player_progress PRIMARY KEY,
total_points    BIGINT    NOT NULL DEFAULT 0,
experience      BIGINT    NOT NULL DEFAULT 0,
player_level    INTEGER   NOT NULL DEFAULT 1,
total_sessions  INTEGER   NOT NULL DEFAULT 0,
success_sessions INTEGER  NOT NULL DEFAULT 0,
best_combo      INTEGER   NOT NULL DEFAULT 0,
created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
version         INTEGER   NOT NULL DEFAULT 0,
CONSTRAINT ck_game_player_nonnegative CHECK
(total_points >= 0 AND experience >= 0 AND player_level >= 1
AND total_sessions >= 0 AND success_sessions >= 0
AND success_sessions <= total_sessions AND best_combo >= 0),
CONSTRAINT ck_game_player_version CHECK (version >= 0)
);
CREATE INDEX idx_game_player_points ON game_player_progress (total_points DESC);
CREATE INDEX idx_game_player_experience ON game_player_progress (experience DESC);
COMMENT ON TABLE game_player_progress IS '玩家积分经验和总体统计';
COMMENT ON COLUMN game_player_progress.student_id IS '学生编号';
COMMENT ON COLUMN game_player_progress.total_points IS '当前总积分';
COMMENT ON COLUMN game_player_progress.experience IS '当前总经验';
COMMENT ON COLUMN game_player_progress.player_level IS '玩家等级';
COMMENT ON COLUMN game_player_progress.total_sessions IS '总对局数';
COMMENT ON COLUMN game_player_progress.success_sessions IS '成功对局数';
COMMENT ON COLUMN game_player_progress.best_combo IS '历史最大连击';
COMMENT ON COLUMN game_player_progress.created_at IS '创建时间';
COMMENT ON COLUMN game_player_progress.updated_at IS '更新时间';
COMMENT ON COLUMN game_player_progress.version IS '乐观锁版本号';

CREATE TABLE game_level_progress (
student_id           BIGINT        NOT NULL,
level_id             BIGINT        NOT NULL,
status               VARCHAR(16)   NOT NULL DEFAULT 'LOCKED',
attempt_count        INTEGER       NOT NULL DEFAULT 0,
best_score           NUMERIC(8,2),
best_duration_seconds INTEGER,
first_unlocked_at    TIMESTAMPTZ,
first_passed_at      TIMESTAMPTZ,
last_played_at       TIMESTAMPTZ,
updated_at           TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_game_level_progress PRIMARY KEY (student_id, level_id),
CONSTRAINT ck_game_level_progress_status CHECK
(status IN ('LOCKED', 'UNLOCKED', 'PASSED')),
CONSTRAINT ck_game_level_progress_attempt CHECK (attempt_count >= 0),
CONSTRAINT ck_game_level_progress_score CHECK (best_score IS NULL OR best_score >= 0),
CONSTRAINT ck_game_level_progress_duration CHECK
(best_duration_seconds IS NULL OR best_duration_seconds > 0)
);
CREATE INDEX idx_game_level_progress_student_status
ON game_level_progress (student_id, status, last_played_at DESC);
CREATE INDEX idx_game_level_progress_level_id
ON game_level_progress (level_id);
COMMENT ON TABLE game_level_progress IS '学生关卡解锁与最佳成绩';
COMMENT ON COLUMN game_level_progress.student_id IS '学生编号';
COMMENT ON COLUMN game_level_progress.level_id IS '关卡编号';
COMMENT ON COLUMN game_level_progress.status IS '解锁状态';
COMMENT ON COLUMN game_level_progress.attempt_count IS '尝试次数';
COMMENT ON COLUMN game_level_progress.best_score IS '最高得分';
COMMENT ON COLUMN game_level_progress.best_duration_seconds IS '最快通过时长';
COMMENT ON COLUMN game_level_progress.first_unlocked_at IS '首次解锁时间';
COMMENT ON COLUMN game_level_progress.first_passed_at IS '首次通过时间';
COMMENT ON COLUMN game_level_progress.last_played_at IS '最近挑战时间';
COMMENT ON COLUMN game_level_progress.updated_at IS '更新时间';
COMMENT ON COLUMN game_level_progress.created_at IS '创建时间';

CREATE TABLE game_reward_record (
id              BIGINT        CONSTRAINT pk_game_reward_record PRIMARY KEY,
student_id      BIGINT        NOT NULL,
session_id      BIGINT        NOT NULL,
reward_type     VARCHAR(16)   NOT NULL,
amount          BIGINT        NOT NULL,
balance_after   BIGINT        NOT NULL,
reason          VARCHAR(64)   NOT NULL,
idempotency_key VARCHAR(128)  NOT NULL,
created_at      TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_game_reward_idempotency UNIQUE (idempotency_key),
CONSTRAINT ck_game_reward_type CHECK
(reward_type IN ('POINTS', 'EXPERIENCE', 'BONUS')),
CONSTRAINT ck_game_reward_amount CHECK (amount > 0),
CONSTRAINT ck_game_reward_balance CHECK (balance_after >= 0)
);
CREATE INDEX idx_game_reward_student_time
ON game_reward_record (student_id, created_at DESC);
CREATE INDEX idx_game_reward_session
ON game_reward_record (session_id, created_at);
COMMENT ON TABLE game_reward_record IS '积分经验奖励不可变流水';
COMMENT ON COLUMN game_reward_record.id IS '奖励流水编号';
COMMENT ON COLUMN game_reward_record.student_id IS '学生编号';
COMMENT ON COLUMN game_reward_record.session_id IS '来源对局';
COMMENT ON COLUMN game_reward_record.reward_type IS '奖励类型';
COMMENT ON COLUMN game_reward_record.amount IS '本次增加数量';
COMMENT ON COLUMN game_reward_record.balance_after IS '发奖后余额';
COMMENT ON COLUMN game_reward_record.reason IS '发奖原因';
COMMENT ON COLUMN game_reward_record.idempotency_key IS '幂等键';
COMMENT ON COLUMN game_reward_record.created_at IS '发放时间';
COMMENT ON COLUMN game_reward_record.updated_at IS '更新时间';
