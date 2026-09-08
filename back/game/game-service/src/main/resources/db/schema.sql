-- game_db schema for level challenges and two-player PK.
-- PostgreSQL 18. Manual execution only; no automatic migration is configured.

CREATE TABLE game_level (
id                 BIGINT        CONSTRAINT pk_game_level PRIMARY KEY,
code               VARCHAR(64)   NOT NULL,
name               VARCHAR(100)  NOT NULL,
description        VARCHAR(500),
grade_level        SMALLINT      NOT NULL,
knowledge_point_id BIGINT        NOT NULL,
difficulty         SMALLINT      NOT NULL DEFAULT 3,
question_source    VARCHAR(24)   NOT NULL DEFAULT 'KNOWLEDGE_POINT',
mode               VARCHAR(16)   NOT NULL DEFAULT 'SOLO',
question_count     SMALLINT      NOT NULL DEFAULT 10,
time_limit_seconds INTEGER       NOT NULL DEFAULT 300,
pass_rate          NUMERIC(5,4)  NOT NULL DEFAULT 0.8000,
min_players        SMALLINT      NOT NULL DEFAULT 1,
max_players        SMALLINT      NOT NULL DEFAULT 1,
base_points        INTEGER       NOT NULL DEFAULT 0,
base_experience    INTEGER       NOT NULL DEFAULT 0,
previous_level_id  BIGINT,
sort_order         INTEGER       NOT NULL DEFAULT 0,
status             VARCHAR(16)   NOT NULL DEFAULT 'DRAFT',
rule_config        JSONB,
created_by         BIGINT        NOT NULL,
created_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at         TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
version            INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT uk_game_level_code UNIQUE (code),
CONSTRAINT fk_game_level_previous FOREIGN KEY (previous_level_id)
REFERENCES game_level (id),
CONSTRAINT ck_game_level_not_self CHECK (previous_level_id IS NULL OR previous_level_id <> id),
CONSTRAINT ck_game_level_grade CHECK (grade_level BETWEEN 1 AND 6),
CONSTRAINT ck_game_level_difficulty CHECK (difficulty BETWEEN 1 AND 5),
CONSTRAINT ck_game_level_source CHECK
(question_source IN ('KNOWLEDGE_POINT', 'CURATED')),
CONSTRAINT ck_game_level_solo CHECK
(mode = 'SOLO' AND min_players = 1 AND max_players = 1),
CONSTRAINT ck_game_level_question_count CHECK (question_count BETWEEN 1 AND 100),
CONSTRAINT ck_game_level_time CHECK (time_limit_seconds BETWEEN 10 AND 7200),
CONSTRAINT ck_game_level_pass_rate CHECK (pass_rate > 0 AND pass_rate <= 1),
CONSTRAINT ck_game_level_rewards CHECK (base_points >= 0 AND base_experience >= 0),
CONSTRAINT ck_game_level_sort CHECK (sort_order >= 0),
CONSTRAINT ck_game_level_status CHECK (status IN ('DRAFT', 'ENABLED', 'DISABLED')),
CONSTRAINT ck_game_level_version CHECK (version >= 0)
);
CREATE INDEX idx_game_level_grade_sort
ON game_level (grade_level, sort_order, id)
WHERE status = 'ENABLED';
CREATE INDEX idx_game_level_knowledge
ON game_level (knowledge_point_id, difficulty, status);
CREATE INDEX idx_game_level_previous
ON game_level (previous_level_id)
WHERE previous_level_id IS NOT NULL;
COMMENT ON TABLE game_level IS '单人闯关关卡配置；PK 不依赖关卡';
COMMENT ON COLUMN game_level.question_source IS 'KNOWLEDGE_POINT 按知识点选题，CURATED 使用关卡题池';
COMMENT ON COLUMN game_level.rule_config IS '星级、连击、能量及推进规则快照来源';

CREATE TABLE game_level_question (
level_id      BIGINT        NOT NULL,
question_id   BIGINT        NOT NULL,
sequence_no   SMALLINT,
weight        NUMERIC(6,3)  NOT NULL DEFAULT 1.000,
required      BOOLEAN       NOT NULL DEFAULT FALSE,
created_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_game_level_question PRIMARY KEY (level_id, question_id),
CONSTRAINT uk_game_level_question_sequence UNIQUE (level_id, sequence_no),
CONSTRAINT fk_game_level_question_level FOREIGN KEY (level_id)
REFERENCES game_level (id) ON DELETE CASCADE,
CONSTRAINT ck_game_level_question_sequence CHECK (sequence_no IS NULL OR sequence_no > 0),
CONSTRAINT ck_game_level_question_weight CHECK (weight > 0)
);
CREATE INDEX idx_game_level_question_level
ON game_level_question (level_id, required DESC, sequence_no, question_id);
COMMENT ON TABLE game_level_question IS 'CURATED 关卡使用的固定题目池；question_id 指向 content_db';

CREATE TABLE game_session (
id                          BIGINT        CONSTRAINT pk_game_session PRIMARY KEY,
mode                        VARCHAR(16)   NOT NULL,
level_id                    BIGINT,
creator_student_id          BIGINT        NOT NULL,
match_mode                  VARCHAR(16),
invite_code                 VARCHAR(12),
status                      VARCHAR(16)   NOT NULL DEFAULT 'WAITING',
member_count                SMALLINT      NOT NULL DEFAULT 1,
grade_level_snapshot        SMALLINT      NOT NULL,
knowledge_point_id_snapshot BIGINT        NOT NULL,
difficulty_snapshot         SMALLINT      NOT NULL,
question_count_snapshot     SMALLINT      NOT NULL,
time_limit_seconds_snapshot INTEGER       NOT NULL,
pass_rate_snapshot          NUMERIC(5,4),
progress_value              NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
winner_student_id           BIGINT,
result_type                 VARCHAR(24),
started_at                  TIMESTAMPTZ,
ended_at                    TIMESTAMPTZ,
expires_at                  TIMESTAMPTZ,
settled_at                  TIMESTAMPTZ,
result_detail               JSONB,
created_at                  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                  TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
version                     INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT fk_game_session_level FOREIGN KEY (level_id)
REFERENCES game_level (id),
CONSTRAINT ck_game_session_mode CHECK (mode IN ('LEVEL', 'PK')),
CONSTRAINT ck_game_session_match_mode CHECK
(match_mode IS NULL OR match_mode IN ('ROOM', 'RANDOM')),
CONSTRAINT ck_game_session_status CHECK
(status IN ('WAITING', 'READY', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'EXPIRED')),
CONSTRAINT ck_game_session_members CHECK (member_count BETWEEN 1 AND 2),
CONSTRAINT ck_game_session_grade CHECK (grade_level_snapshot BETWEEN 1 AND 6),
CONSTRAINT ck_game_session_difficulty CHECK (difficulty_snapshot BETWEEN 1 AND 5),
CONSTRAINT ck_game_session_question_count CHECK (question_count_snapshot BETWEEN 1 AND 100),
CONSTRAINT ck_game_session_time CHECK (time_limit_seconds_snapshot BETWEEN 10 AND 7200),
CONSTRAINT ck_game_session_pass_rate CHECK
(pass_rate_snapshot IS NULL OR (pass_rate_snapshot > 0 AND pass_rate_snapshot <= 1)),
CONSTRAINT ck_game_session_progress CHECK (progress_value >= 0),
CONSTRAINT ck_game_session_version CHECK (version >= 0),
CONSTRAINT ck_game_session_mode_fields CHECK (
    (mode = 'LEVEL' AND level_id IS NOT NULL AND match_mode IS NULL
        AND invite_code IS NULL AND member_count = 1 AND pass_rate_snapshot IS NOT NULL)
    OR
    (mode = 'PK' AND level_id IS NULL AND match_mode IS NOT NULL
        AND pass_rate_snapshot IS NULL)
),
CONSTRAINT ck_game_session_invite CHECK (
    (mode = 'PK' AND match_mode = 'ROOM' AND invite_code IS NOT NULL)
    OR
    ((mode <> 'PK' OR match_mode <> 'ROOM') AND invite_code IS NULL)
),
CONSTRAINT ck_game_session_invite_format CHECK
(invite_code IS NULL OR invite_code ~ '^[A-Z0-9]{6,12}$'),
CONSTRAINT ck_game_session_pk_ready CHECK (
    mode <> 'PK'
    OR status IN ('WAITING', 'CANCELLED', 'EXPIRED')
    OR member_count = 2
),
CONSTRAINT ck_game_session_result CHECK (
    (status <> 'COMPLETED' AND result_type IS NULL
        AND winner_student_id IS NULL AND settled_at IS NULL)
    OR
    (status = 'COMPLETED' AND settled_at IS NOT NULL AND (
        (mode = 'LEVEL' AND result_type IN ('LEVEL_PASSED', 'LEVEL_FAILED')
            AND winner_student_id IS NULL)
        OR
        (mode = 'PK' AND result_type = 'PK_WIN' AND winner_student_id IS NOT NULL)
        OR
        (mode = 'PK' AND result_type = 'PK_DRAW' AND winner_student_id IS NULL)
    ))
)
);
CREATE UNIQUE INDEX uk_game_session_active_invite
ON game_session (invite_code)
WHERE invite_code IS NOT NULL AND status IN ('WAITING', 'READY');
CREATE INDEX idx_game_session_creator_created
ON game_session (creator_student_id, created_at DESC);
CREATE INDEX idx_game_session_mode_status
ON game_session (mode, status, created_at DESC);
CREATE INDEX idx_game_session_level
ON game_session (level_id, created_at DESC)
WHERE level_id IS NOT NULL;
CREATE INDEX idx_game_session_winner
ON game_session (winner_student_id, ended_at DESC)
WHERE winner_student_id IS NOT NULL;
COMMENT ON TABLE game_session IS '统一承载单人闯关与双人 PK 的对局规则和结算快照';
COMMENT ON COLUMN game_session.mode IS 'LEVEL 单人闯关，PK 双人对战';
COMMENT ON COLUMN game_session.match_mode IS 'PK 的房间码或随机匹配方式';

CREATE TABLE game_match_ticket (
id                  BIGINT        CONSTRAINT pk_game_match_ticket PRIMARY KEY,
student_id          BIGINT        NOT NULL,
status              VARCHAR(16)   NOT NULL DEFAULT 'WAITING',
grade_level         SMALLINT      NOT NULL,
knowledge_point_id  BIGINT        NOT NULL,
difficulty          SMALLINT      NOT NULL,
question_count      SMALLINT      NOT NULL DEFAULT 10,
time_limit_seconds  INTEGER       NOT NULL DEFAULT 180,
matched_session_id  BIGINT,
expires_at          TIMESTAMPTZ   NOT NULL,
matched_at          TIMESTAMPTZ,
created_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
version             INTEGER       NOT NULL DEFAULT 0,
CONSTRAINT fk_game_match_ticket_session FOREIGN KEY (matched_session_id)
REFERENCES game_session (id),
CONSTRAINT ck_game_match_ticket_status CHECK
(status IN ('WAITING', 'MATCHED', 'CANCELLED', 'EXPIRED')),
CONSTRAINT ck_game_match_ticket_grade CHECK (grade_level BETWEEN 1 AND 6),
CONSTRAINT ck_game_match_ticket_difficulty CHECK (difficulty BETWEEN 1 AND 5),
CONSTRAINT ck_game_match_ticket_question_count CHECK (question_count BETWEEN 1 AND 100),
CONSTRAINT ck_game_match_ticket_time CHECK (time_limit_seconds BETWEEN 10 AND 7200),
CONSTRAINT ck_game_match_ticket_version CHECK (version >= 0),
CONSTRAINT ck_game_match_ticket_result CHECK (
    (status = 'MATCHED' AND matched_session_id IS NOT NULL AND matched_at IS NOT NULL)
    OR
    (status <> 'MATCHED' AND matched_session_id IS NULL AND matched_at IS NULL)
)
);
CREATE UNIQUE INDEX uk_game_match_ticket_waiting_student
ON game_match_ticket (student_id)
WHERE status = 'WAITING';
CREATE INDEX idx_game_match_ticket_queue
ON game_match_ticket (
    grade_level, knowledge_point_id, difficulty,
    question_count, time_limit_seconds, created_at, id
)
WHERE status = 'WAITING';
CREATE INDEX idx_game_match_ticket_expiry
ON game_match_ticket (expires_at, id)
WHERE status = 'WAITING';
COMMENT ON TABLE game_match_ticket IS '双人 PK 随机匹配票据；房间码模式直接使用 game_session';

CREATE TABLE game_session_member (
session_id           BIGINT        NOT NULL,
student_id           BIGINT        NOT NULL,
seat_no              SMALLINT      NOT NULL,
member_role          VARCHAR(16)   NOT NULL,
status               VARCHAR(16)   NOT NULL DEFAULT 'JOINED',
result               VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
practice_task_id     BIGINT,
answered_count       SMALLINT      NOT NULL DEFAULT 0,
correct_count        SMALLINT      NOT NULL DEFAULT 0,
score                NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
contribution_value   NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
current_combo        SMALLINT      NOT NULL DEFAULT 0,
max_combo            SMALLINT      NOT NULL DEFAULT 0,
total_response_ms    BIGINT        NOT NULL DEFAULT 0,
rating_before        INTEGER,
rating_after         INTEGER,
points_reward        INTEGER       NOT NULL DEFAULT 0,
experience_reward    INTEGER       NOT NULL DEFAULT 0,
joined_at            TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
last_answered_at     TIMESTAMPTZ,
finished_at          TIMESTAMPTZ,
created_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at           TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_game_session_member PRIMARY KEY (session_id, student_id),
CONSTRAINT uk_game_session_member_seat UNIQUE (session_id, seat_no),
CONSTRAINT uk_game_session_member_role UNIQUE (session_id, member_role),
CONSTRAINT fk_game_session_member_session FOREIGN KEY (session_id)
REFERENCES game_session (id) ON DELETE CASCADE,
CONSTRAINT ck_game_session_member_seat CHECK (seat_no BETWEEN 1 AND 2),
CONSTRAINT ck_game_session_member_role CHECK (member_role IN ('CREATOR', 'OPPONENT')),
CONSTRAINT ck_game_session_member_status CHECK
(status IN ('JOINED', 'READY', 'PLAYING', 'FINISHED', 'LEFT')),
CONSTRAINT ck_game_session_member_result CHECK
(result IN ('PENDING', 'PASSED', 'FAILED', 'WIN', 'LOSE', 'DRAW', 'CANCELLED')),
CONSTRAINT ck_game_session_member_counts CHECK
(answered_count >= 0 AND correct_count >= 0 AND correct_count <= answered_count
AND current_combo >= 0 AND max_combo >= current_combo),
CONSTRAINT ck_game_session_member_values CHECK
(score >= 0 AND contribution_value >= 0 AND total_response_ms >= 0
AND points_reward >= 0 AND experience_reward >= 0),
CONSTRAINT ck_game_session_member_rating CHECK
((rating_before IS NULL AND rating_after IS NULL)
OR (rating_before >= 0 AND rating_after >= 0))
);
CREATE INDEX idx_game_session_member_student
ON game_session_member (student_id, joined_at DESC);
CREATE INDEX idx_game_session_member_session_status
ON game_session_member (session_id, status, seat_no);
CREATE UNIQUE INDEX uk_game_session_member_active_student
ON game_session_member (student_id)
WHERE status IN ('JOINED', 'READY', 'PLAYING');
COMMENT ON TABLE game_session_member IS '闯关玩家或 PK 双方的实时状态与个人结算快照';
COMMENT ON COLUMN game_session_member.practice_task_id IS '学习服务中统一记录作答的任务编号';

CREATE TABLE game_session_question (
id                  BIGINT        CONSTRAINT pk_game_session_question PRIMARY KEY,
session_id          BIGINT        NOT NULL,
student_id          BIGINT        NOT NULL,
sequence_no         SMALLINT      NOT NULL,
question_id         BIGINT        NOT NULL,
question_snapshot   JSONB         NOT NULL,
max_score           NUMERIC(8,2)  NOT NULL DEFAULT 1.00,
grading_status      VARCHAR(16)   NOT NULL DEFAULT 'UNANSWERED',
student_answer      JSONB,
answer_request_id   VARCHAR(64),
answer_record_id    BIGINT,
is_correct          BOOLEAN,
score               NUMERIC(8,2),
response_ms         INTEGER,
combo_after         SMALLINT,
progress_delta      NUMERIC(8,2)  NOT NULL DEFAULT 0.00,
energy_delta        INTEGER       NOT NULL DEFAULT 0,
answered_at         TIMESTAMPTZ,
created_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at          TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_game_session_question_seq UNIQUE (session_id, student_id, sequence_no),
CONSTRAINT uk_game_session_question_answer_record UNIQUE (answer_record_id),
CONSTRAINT fk_game_session_question_member FOREIGN KEY (session_id, student_id)
REFERENCES game_session_member (session_id, student_id) ON DELETE CASCADE,
CONSTRAINT ck_game_session_question_seq CHECK (sequence_no > 0),
CONSTRAINT ck_game_session_question_max_score CHECK (max_score > 0),
CONSTRAINT ck_game_session_question_grading CHECK
(grading_status IN ('UNANSWERED', 'GRADED')),
CONSTRAINT ck_game_session_question_response CHECK (response_ms IS NULL OR response_ms >= 0),
CONSTRAINT ck_game_session_question_combo CHECK (combo_after IS NULL OR combo_after >= 0),
CONSTRAINT ck_game_session_question_progress CHECK (progress_delta >= 0),
CONSTRAINT ck_game_session_question_request CHECK
(answer_request_id IS NULL OR char_length(answer_request_id) BETWEEN 8 AND 64),
CONSTRAINT ck_game_session_question_state CHECK (
    (grading_status = 'UNANSWERED' AND student_answer IS NULL
        AND answer_request_id IS NULL AND answer_record_id IS NULL
        AND is_correct IS NULL AND score IS NULL AND response_ms IS NULL
        AND combo_after IS NULL AND answered_at IS NULL)
    OR
    (grading_status = 'GRADED' AND student_answer IS NOT NULL
        AND answer_request_id IS NOT NULL AND is_correct IS NOT NULL
        AND score IS NOT NULL AND score >= 0 AND score <= max_score
        AND response_ms IS NOT NULL AND combo_after IS NOT NULL
        AND answered_at IS NOT NULL)
)
);
CREATE UNIQUE INDEX uk_game_session_question_request
ON game_session_question (answer_request_id)
WHERE answer_request_id IS NOT NULL;
CREATE INDEX idx_game_session_question_member
ON game_session_question (session_id, student_id, sequence_no);
CREATE INDEX idx_game_session_question_unanswered
ON game_session_question (session_id, student_id, sequence_no)
WHERE grading_status = 'UNANSWERED';
COMMENT ON TABLE game_session_question IS '每名玩家的固定题目快照、幂等作答和推进结果';
COMMENT ON COLUMN game_session_question.question_id IS '指向 content_db 已启用固定题目';

CREATE TABLE game_player_progress (
student_id            BIGINT      CONSTRAINT pk_game_player_progress PRIMARY KEY,
total_points          BIGINT      NOT NULL DEFAULT 0,
experience            BIGINT      NOT NULL DEFAULT 0,
player_level          INTEGER     NOT NULL DEFAULT 1,
total_sessions        INTEGER     NOT NULL DEFAULT 0,
success_sessions      INTEGER     NOT NULL DEFAULT 0,
level_passed_count    INTEGER     NOT NULL DEFAULT 0,
pk_sessions           INTEGER     NOT NULL DEFAULT 0,
pk_wins               INTEGER     NOT NULL DEFAULT 0,
pk_losses             INTEGER     NOT NULL DEFAULT 0,
pk_draws              INTEGER     NOT NULL DEFAULT 0,
pk_rating             INTEGER     NOT NULL DEFAULT 1000,
current_win_streak    INTEGER     NOT NULL DEFAULT 0,
best_win_streak       INTEGER     NOT NULL DEFAULT 0,
best_combo            INTEGER     NOT NULL DEFAULT 0,
last_played_at        TIMESTAMPTZ,
created_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at            TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
version               INTEGER     NOT NULL DEFAULT 0,
CONSTRAINT ck_game_player_nonnegative CHECK
(total_points >= 0 AND experience >= 0 AND player_level >= 1
AND total_sessions >= 0 AND success_sessions >= 0
AND level_passed_count >= 0 AND pk_sessions >= 0
AND pk_wins >= 0 AND pk_losses >= 0 AND pk_draws >= 0
AND pk_rating >= 0 AND current_win_streak >= 0
AND best_win_streak >= current_win_streak AND best_combo >= 0),
CONSTRAINT ck_game_player_pk_total CHECK (pk_wins + pk_losses + pk_draws = pk_sessions),
CONSTRAINT ck_game_player_session_total CHECK
(success_sessions <= total_sessions AND pk_sessions <= total_sessions),
CONSTRAINT ck_game_player_version CHECK (version >= 0)
);
CREATE INDEX idx_game_player_points
ON game_player_progress (total_points DESC, student_id);
CREATE INDEX idx_game_player_rating
ON game_player_progress (pk_rating DESC, pk_wins DESC, student_id);
COMMENT ON TABLE game_player_progress IS '玩家闯关与 PK 的累计成长、战绩和评分';

CREATE TABLE game_level_progress (
student_id             BIGINT        NOT NULL,
level_id               BIGINT        NOT NULL,
status                 VARCHAR(16)   NOT NULL DEFAULT 'LOCKED',
attempt_count          INTEGER       NOT NULL DEFAULT 0,
best_score             NUMERIC(8,2),
best_correct_count     SMALLINT,
best_star_count        SMALLINT,
best_duration_seconds  INTEGER,
last_session_id        BIGINT,
first_unlocked_at      TIMESTAMPTZ,
first_passed_at        TIMESTAMPTZ,
last_played_at         TIMESTAMPTZ,
created_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at             TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_game_level_progress PRIMARY KEY (student_id, level_id),
CONSTRAINT fk_game_level_progress_level FOREIGN KEY (level_id)
REFERENCES game_level (id),
CONSTRAINT fk_game_level_progress_session FOREIGN KEY (last_session_id)
REFERENCES game_session (id),
CONSTRAINT ck_game_level_progress_status CHECK
(status IN ('LOCKED', 'UNLOCKED', 'PASSED')),
CONSTRAINT ck_game_level_progress_attempt CHECK (attempt_count >= 0),
CONSTRAINT ck_game_level_progress_score CHECK (best_score IS NULL OR best_score >= 0),
CONSTRAINT ck_game_level_progress_correct CHECK
(best_correct_count IS NULL OR best_correct_count >= 0),
CONSTRAINT ck_game_level_progress_stars CHECK
(best_star_count IS NULL OR best_star_count BETWEEN 0 AND 3),
CONSTRAINT ck_game_level_progress_duration CHECK
(best_duration_seconds IS NULL OR best_duration_seconds > 0)
);
CREATE INDEX idx_game_level_progress_student_status
ON game_level_progress (student_id, status, last_played_at DESC);
CREATE INDEX idx_game_level_progress_level
ON game_level_progress (level_id, status);
COMMENT ON TABLE game_level_progress IS '学生关卡解锁、星级和最佳成绩';

CREATE TABLE game_reward_record (
id               BIGINT        CONSTRAINT pk_game_reward_record PRIMARY KEY,
student_id       BIGINT        NOT NULL,
session_id       BIGINT        NOT NULL,
business_type    VARCHAR(16)   NOT NULL,
reward_type      VARCHAR(16)   NOT NULL,
amount           BIGINT        NOT NULL,
balance_after    BIGINT        NOT NULL,
reason           VARCHAR(64)   NOT NULL,
idempotency_key  VARCHAR(128)  NOT NULL,
created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_game_reward_idempotency UNIQUE (idempotency_key),
CONSTRAINT fk_game_reward_session FOREIGN KEY (session_id)
REFERENCES game_session (id),
CONSTRAINT ck_game_reward_business CHECK (business_type IN ('LEVEL', 'PK')),
CONSTRAINT ck_game_reward_type CHECK
(reward_type IN ('POINTS', 'EXPERIENCE', 'BONUS')),
CONSTRAINT ck_game_reward_amount CHECK (amount > 0),
CONSTRAINT ck_game_reward_balance CHECK (balance_after >= 0)
);
CREATE INDEX idx_game_reward_student_time
ON game_reward_record (student_id, created_at DESC, id DESC);
CREATE INDEX idx_game_reward_session
ON game_reward_record (session_id, student_id, created_at);
COMMENT ON TABLE game_reward_record IS '闯关和 PK 的积分、经验不可变幂等奖励流水';

CREATE TABLE game_event_outbox (
id               BIGINT        CONSTRAINT pk_game_event_outbox PRIMARY KEY,
event_key        VARCHAR(128)  NOT NULL,
event_type       VARCHAR(64)   NOT NULL,
aggregate_type   VARCHAR(32)   NOT NULL,
aggregate_id     BIGINT        NOT NULL,
student_id       BIGINT,
payload          JSONB         NOT NULL,
delivery_status  VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
retry_count      INTEGER       NOT NULL DEFAULT 0,
next_retry_at    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
sent_at          TIMESTAMPTZ,
last_error       VARCHAR(1000),
created_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at       TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_game_event_outbox_key UNIQUE (event_key),
CONSTRAINT ck_game_event_outbox_key CHECK (char_length(trim(event_key)) > 0),
CONSTRAINT ck_game_event_outbox_aggregate CHECK
(aggregate_type IN ('SESSION', 'ANSWER', 'REWARD')),
CONSTRAINT ck_game_event_outbox_delivery CHECK
(delivery_status IN ('PENDING', 'SENT', 'DEAD')),
CONSTRAINT ck_game_event_outbox_retry CHECK (retry_count >= 0),
CONSTRAINT ck_game_event_outbox_sent CHECK
((delivery_status = 'SENT' AND sent_at IS NOT NULL)
OR (delivery_status <> 'SENT' AND sent_at IS NULL))
);
CREATE INDEX idx_game_event_outbox_pending
ON game_event_outbox (next_retry_at, created_at, id)
WHERE delivery_status = 'PENDING';
CREATE INDEX idx_game_event_outbox_aggregate
ON game_event_outbox (aggregate_type, aggregate_id, created_at);
CREATE INDEX idx_game_event_outbox_sent
ON game_event_outbox (sent_at)
WHERE delivery_status = 'SENT';
COMMENT ON TABLE game_event_outbox IS '向学习、通知等服务可靠发布游戏结果的事务发件箱';
