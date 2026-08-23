-- PostgreSQL 官方镜像只会在数据目录首次初始化时执行本脚本。
-- 每个微服务使用独立数据库，避免跨服务直接读写他人数据表。
-- 使用初始化账号作为所有者，使本地 DB_USERNAME 覆盖值保持有效。
SELECT format('CREATE DATABASE %I OWNER %I', database_name, current_user)
FROM (VALUES ('file_db'), ('notification_db'), ('audit_db')) AS databases(database_name)
\gexec
