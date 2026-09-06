-- user_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE t_user (
id                  BIGINT CONSTRAINT pk_t_user PRIMARY KEY,
username            VARCHAR(32)  NOT NULL,
wechat_openid                 VARCHAR(64),
wechat_web_openid             VARCHAR(64),
wechat_unionid                VARCHAR(64),
password            VARCHAR(100),
nickname            VARCHAR(50),
real_name           VARCHAR(50),
email               VARCHAR(254),
phone               VARCHAR(20),
avatar              VARCHAR(500),
status              SMALLINT     NOT NULL DEFAULT 1,
last_login_at       TIMESTAMPTZ,
last_login_ip       VARCHAR(64),
password_changed_at TIMESTAMPTZ,
created_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at          TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted             BOOLEAN      NOT NULL DEFAULT FALSE,
version             INTEGER      NOT NULL DEFAULT 0,
CONSTRAINT ck_t_user_status CHECK (status IN (0, 1)),
CONSTRAINT ck_t_user_version CHECK (version >= 0)
);
CREATE UNIQUE INDEX uk_t_user_username_active
ON t_user (LOWER(username)) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uk_t_user_phone_active
ON t_user (phone) WHERE is_deleted = FALSE AND phone IS NOT NULL;
CREATE UNIQUE INDEX uk_t_user_email_active
ON t_user (LOWER(email)) WHERE is_deleted = FALSE AND email IS NOT NULL;
CREATE INDEX idx_t_user_status_created_at
ON t_user (status, created_at DESC) WHERE is_deleted = FALSE;
CREATE UNIQUE INDEX uk_t_user_wechat_openid
ON t_user (wechat_openid)
WHERE wechat_openid IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX uk_t_user_wechat_web_openid
ON t_user (wechat_web_openid)
WHERE wechat_web_openid IS NOT NULL AND is_deleted = FALSE;
CREATE UNIQUE INDEX uk_t_user_wechat_unionid
ON t_user (wechat_unionid)
WHERE wechat_unionid IS NOT NULL AND is_deleted = FALSE;
COMMENT ON TABLE t_user IS '统一账号表';
COMMENT ON COLUMN t_user.id IS '用户编号，后端雪花 ID';
COMMENT ON COLUMN t_user.username IS '登录用户名';
COMMENT ON COLUMN t_user.wechat_openid IS '微信小程序 OpenID，用于登录与账号绑定';
COMMENT ON COLUMN t_user.wechat_web_openid IS '微信开放平台网站应用 OpenID，用于网页扫码登录';
COMMENT ON COLUMN t_user.wechat_unionid IS '微信开放平台 UnionID，用于跨端关联同一账号';
COMMENT ON COLUMN t_user.password IS 'BCrypt 密码哈希；纯微信登录用户首次登录时可为空，不能存明文';
COMMENT ON COLUMN t_user.nickname IS '昵称';
COMMENT ON COLUMN t_user.real_name IS '真实姓名';
COMMENT ON COLUMN t_user.email IS '邮箱';
COMMENT ON COLUMN t_user.phone IS '手机号';
COMMENT ON COLUMN t_user.avatar IS '头像地址或文件访问标识';
COMMENT ON COLUMN t_user.status IS '账号状态';
COMMENT ON COLUMN t_user.last_login_at IS '最近登录成功时间';
COMMENT ON COLUMN t_user.last_login_ip IS '最近登录 IP';
COMMENT ON COLUMN t_user.password_changed_at IS '最近密码设置时间';
COMMENT ON COLUMN t_user.created_at IS '创建时间';
COMMENT ON COLUMN t_user.updated_at IS '更新时间';
COMMENT ON COLUMN t_user.is_deleted IS '是否已删除';
COMMENT ON COLUMN t_user.version IS '乐观锁版本号';

CREATE TABLE sys_student_profile (
user_id      BIGINT    CONSTRAINT pk_sys_student_profile PRIMARY KEY,
student_no   VARCHAR(32),
grade_level  SMALLINT  NOT NULL,
semester     SMALLINT  NOT NULL DEFAULT 1,
created_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at   TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
version      INTEGER   NOT NULL DEFAULT 0,
CONSTRAINT ck_student_profile_grade CHECK (grade_level BETWEEN 1 AND 6),
CONSTRAINT ck_student_profile_semester CHECK (semester BETWEEN 1 AND 2),
CONSTRAINT ck_student_profile_version CHECK (version >= 0)
);
CREATE UNIQUE INDEX uk_sys_student_profile_student_no
ON sys_student_profile (student_no) WHERE student_no IS NOT NULL;
COMMENT ON TABLE sys_student_profile IS '学生业务资料表';
COMMENT ON COLUMN sys_student_profile.user_id IS '学生对应的用户编号';
COMMENT ON COLUMN sys_student_profile.student_no IS '校内学号；独立使用系统时可为空';
COMMENT ON COLUMN sys_student_profile.grade_level IS '当前年级';
COMMENT ON COLUMN sys_student_profile.semester IS '当前学期';
COMMENT ON COLUMN sys_student_profile.created_at IS '创建时间';
COMMENT ON COLUMN sys_student_profile.updated_at IS '更新时间';
COMMENT ON COLUMN sys_student_profile.version IS '乐观锁版本号';

CREATE TABLE sys_teacher_profile (
user_id       BIGINT       CONSTRAINT pk_sys_teacher_profile PRIMARY KEY,
teacher_no    VARCHAR(32),
title         VARCHAR(50),
introduction  VARCHAR(500),
created_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at    TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
version       INTEGER      NOT NULL DEFAULT 0,
CONSTRAINT ck_teacher_profile_version CHECK (version >= 0)
);
CREATE UNIQUE INDEX uk_sys_teacher_profile_teacher_no
ON sys_teacher_profile (teacher_no) WHERE teacher_no IS NOT NULL;
COMMENT ON TABLE sys_teacher_profile IS '教师业务资料表';
COMMENT ON COLUMN sys_teacher_profile.user_id IS '教师对应的用户编号';
COMMENT ON COLUMN sys_teacher_profile.teacher_no IS '教师工号';
COMMENT ON COLUMN sys_teacher_profile.title IS '职称或岗位称谓';
COMMENT ON COLUMN sys_teacher_profile.introduction IS '简介';
COMMENT ON COLUMN sys_teacher_profile.created_at IS '创建时间';
COMMENT ON COLUMN sys_teacher_profile.updated_at IS '更新时间';
COMMENT ON COLUMN sys_teacher_profile.version IS '乐观锁版本号';

CREATE TABLE sys_role (
id          BIGINT GENERATED BY DEFAULT AS IDENTITY CONSTRAINT pk_sys_role PRIMARY KEY,
code        VARCHAR(64)  NOT NULL,
name        VARCHAR(64)  NOT NULL,
description VARCHAR(255),
status      SMALLINT     NOT NULL DEFAULT 1,
is_built_in    BOOLEAN      NOT NULL DEFAULT FALSE,
created_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_sys_role_code UNIQUE (code),
CONSTRAINT ck_sys_role_status CHECK (status IN (0, 1))
);
COMMENT ON TABLE sys_role IS 'RBAC角色字典';
COMMENT ON COLUMN sys_role.id IS '角色编号';
COMMENT ON COLUMN sys_role.code IS '稳定角色编码：student/teacher/admin';
COMMENT ON COLUMN sys_role.name IS '展示名称';
COMMENT ON COLUMN sys_role.description IS '角色说明';
COMMENT ON COLUMN sys_role.status IS '是否启用';
COMMENT ON COLUMN sys_role.is_built_in IS '是否内置角色';
COMMENT ON COLUMN sys_role.created_at IS '创建时间';
COMMENT ON COLUMN sys_role.updated_at IS '更新时间';

CREATE TABLE sys_permission (
id          BIGINT GENERATED BY DEFAULT AS IDENTITY CONSTRAINT pk_sys_permission PRIMARY KEY,
code        VARCHAR(128) NOT NULL,
name        VARCHAR(64)  NOT NULL,
description VARCHAR(255),
created_at  TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_sys_permission_code UNIQUE (code)
);
COMMENT ON TABLE sys_permission IS 'RBAC权限字典';
COMMENT ON COLUMN sys_permission.id IS '权限编号';
COMMENT ON COLUMN sys_permission.code IS '程序鉴权使用的权限编码';
COMMENT ON COLUMN sys_permission.name IS '权限名称';
COMMENT ON COLUMN sys_permission.description IS '权限说明';
COMMENT ON COLUMN sys_permission.created_at IS '创建时间';
COMMENT ON COLUMN sys_permission.updated_at IS '更新时间';

CREATE TABLE sys_user_role (
user_id    BIGINT    NOT NULL,
role_id    BIGINT    NOT NULL,
created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_sys_user_role PRIMARY KEY (user_id, role_id)
);
CREATE INDEX idx_sys_user_role_role_id ON sys_user_role (role_id);
COMMENT ON TABLE sys_user_role IS '用户角色关联';
COMMENT ON COLUMN sys_user_role.user_id IS '用户编号';
COMMENT ON COLUMN sys_user_role.role_id IS '角色编号';
COMMENT ON COLUMN sys_user_role.created_at IS '授权时间';
COMMENT ON COLUMN sys_user_role.updated_at IS '更新时间';

CREATE TABLE sys_role_permission (
role_id       BIGINT    NOT NULL,
permission_id BIGINT    NOT NULL,
created_at    TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT pk_sys_role_permission PRIMARY KEY (role_id, permission_id)
);
CREATE INDEX idx_sys_role_permission_permission_id
ON sys_role_permission (permission_id);
COMMENT ON TABLE sys_role_permission IS '角色权限关联';
COMMENT ON COLUMN sys_role_permission.role_id IS '角色编号';
COMMENT ON COLUMN sys_role_permission.permission_id IS '权限编号';
COMMENT ON COLUMN sys_role_permission.created_at IS '授权时间';
COMMENT ON COLUMN sys_role_permission.updated_at IS '更新时间';

CREATE TABLE sys_outbox_event (
id            UUID          CONSTRAINT pk_sys_outbox_event PRIMARY KEY,
event_type    VARCHAR(128)  NOT NULL,
routing_key   VARCHAR(128)  NOT NULL,
payload       TEXT          NOT NULL,
status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
retry_count   INTEGER       NOT NULL DEFAULT 0,
next_retry_at TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
created_at    TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
published_at  TIMESTAMPTZ,
last_error    VARCHAR(1000),
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT ck_sys_outbox_event_status
CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
CONSTRAINT ck_sys_outbox_event_retry_count CHECK (retry_count >= 0)
);
CREATE INDEX idx_sys_outbox_event_pending
ON sys_outbox_event (next_retry_at, created_at)
WHERE status = 'PENDING';
CREATE INDEX idx_sys_outbox_event_published
ON sys_outbox_event (published_at)
WHERE status = 'PUBLISHED';
COMMENT ON TABLE sys_outbox_event IS '事务发件箱';
COMMENT ON COLUMN sys_outbox_event.id IS '领域事件编号';
COMMENT ON COLUMN sys_outbox_event.event_type IS '事件类型和版本';
COMMENT ON COLUMN sys_outbox_event.routing_key IS 'RabbitMQ 路由键';
COMMENT ON COLUMN sys_outbox_event.payload IS 'JSON 事件快照';
COMMENT ON COLUMN sys_outbox_event.status IS '发布状态';
COMMENT ON COLUMN sys_outbox_event.retry_count IS '失败重试次数';
COMMENT ON COLUMN sys_outbox_event.next_retry_at IS '下次可重试时间';
COMMENT ON COLUMN sys_outbox_event.created_at IS '入箱时间';
COMMENT ON COLUMN sys_outbox_event.published_at IS 'Broker 确认时间';
COMMENT ON COLUMN sys_outbox_event.last_error IS '最近失败摘要';
COMMENT ON COLUMN sys_outbox_event.updated_at IS '更新时间';
