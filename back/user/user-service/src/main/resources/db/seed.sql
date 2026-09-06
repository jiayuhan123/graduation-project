-- user_db initialization data
-- Manual execution only. Run after schema.sql.
-- This script is idempotent and can be executed repeatedly.

BEGIN;

-- Keep existing databases compatible with passwordless first-time WeChat login.
ALTER TABLE t_user
    ALTER COLUMN password DROP NOT NULL;

ALTER TABLE t_user
    ADD COLUMN IF NOT EXISTS wechat_web_openid VARCHAR(64),
    ADD COLUMN IF NOT EXISTS wechat_unionid VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uk_t_user_wechat_web_openid
ON t_user (wechat_web_openid)
WHERE wechat_web_openid IS NOT NULL AND is_deleted = FALSE;

CREATE UNIQUE INDEX IF NOT EXISTS uk_t_user_wechat_unionid
ON t_user (wechat_unionid)
WHERE wechat_unionid IS NOT NULL AND is_deleted = FALSE;

COMMENT ON COLUMN t_user.wechat_web_openid IS
    '微信开放平台网站应用 OpenID，用于网页扫码登录';
COMMENT ON COLUMN t_user.wechat_unionid IS
    '微信开放平台 UnionID，用于跨端关联同一账号';

COMMENT ON COLUMN t_user.password IS
    'BCrypt 密码哈希；纯微信登录用户首次登录时可为空，不能存明文';

-- Built-in roles. WeChat first-time login depends on the student role.
INSERT INTO sys_role (code, name, description, status, is_built_in)
VALUES
    ('student', '学生', '使用练习、拍照批改和游戏闯关', 1, TRUE),
    ('teacher', '教师', '管理班级、布置作业、查看学情并进行复核', 1, TRUE),
    ('admin', '管理员', '维护平台内容、规则、用户与权限', 1, TRUE)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    status = EXCLUDED.status,
    is_built_in = EXCLUDED.is_built_in,
    updated_at = CURRENT_TIMESTAMP;

-- Stable permission codes shared by the gateway and business services.
INSERT INTO sys_permission (code, name, description)
VALUES
    ('learning:practice:use', '使用个性化练习', '创建和完成个人练习'),
    ('learning:photo:submit', '提交拍照批改', '上传并提交拍照批改任务'),
    ('game:play', '参与游戏闯关', '参与单人或团队闯关'),
    ('teaching:class:manage', '管理班级', '创建并维护本人负责的班级'),
    ('teaching:assignment:manage', '管理作业', '创建、发布和查看教师作业'),
    ('teaching:report:query', '查看学情', '查看学生和班级学习报告'),
    ('learning:marking:review', '复核批改', '复核低可信拍照批改结果'),
    ('system:user:manage', '管理用户', '管理账号、状态和角色'),
    ('system:role:manage', '管理角色权限', '维护角色及其权限'),
    ('content:manage', '管理学习内容', '维护模块、知识点、题目和错因'),
    ('game:level:manage', '管理游戏关卡', '维护关卡与奖励规则'),
    ('audit:query', '查询审计日志', '按权限查询系统审计记录')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    description = EXCLUDED.description,
    updated_at = CURRENT_TIMESTAMP;

-- Student permissions.
WITH permission_grants(role_code, permission_code) AS (
    VALUES
        ('student', 'learning:practice:use'),
        ('student', 'learning:photo:submit'),
        ('student', 'game:play')
)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM permission_grants grant_item
JOIN sys_role role ON role.code = grant_item.role_code
JOIN sys_permission permission ON permission.code = grant_item.permission_code
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Teacher permissions.
WITH permission_grants(role_code, permission_code) AS (
    VALUES
        ('teacher', 'teaching:class:manage'),
        ('teacher', 'teaching:assignment:manage'),
        ('teacher', 'teaching:report:query'),
        ('teacher', 'learning:marking:review')
)
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM permission_grants grant_item
JOIN sys_role role ON role.code = grant_item.role_code
JOIN sys_permission permission ON permission.code = grant_item.permission_code
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- The built-in administrator receives every currently defined permission.
INSERT INTO sys_role_permission (role_id, permission_id)
SELECT role.id, permission.id
FROM sys_role role
CROSS JOIN sys_permission permission
WHERE role.code = 'admin'
ON CONFLICT (role_id, permission_id) DO NOTHING;

COMMIT;
