-- PostgreSQL 官方镜像只会在数据目录首次初始化时执行本脚本。
-- 每个微服务使用独立数据库，避免跨服务直接读写他人数据表。
CREATE DATABASE file_db OWNER root;
CREATE DATABASE notification_db OWNER root;
CREATE DATABASE audit_db OWNER root;
