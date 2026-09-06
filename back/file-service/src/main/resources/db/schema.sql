-- file_db schema
-- Extracted from 文档/全系统数据库建表说明.md (V1.1).
-- Manual reference only; this file is not configured for automatic execution.

CREATE TABLE file_object (
id            UUID          CONSTRAINT pk_file_object PRIMARY KEY,
owner_id      BIGINT        NOT NULL,
original_name VARCHAR(255)  NOT NULL,
object_key    VARCHAR(700)  NOT NULL,
content_type  VARCHAR(255)  NOT NULL,
size_bytes    BIGINT        NOT NULL,
sha256        CHAR(64)      NOT NULL,
created_at    TIMESTAMPTZ     NOT NULL DEFAULT CURRENT_TIMESTAMP,
is_deleted       BOOLEAN       NOT NULL DEFAULT FALSE,
updated_at                    TIMESTAMPTZ   NOT NULL DEFAULT CURRENT_TIMESTAMP,
CONSTRAINT uk_file_object_key UNIQUE (object_key),
CONSTRAINT ck_file_object_size CHECK (size_bytes > 0),
CONSTRAINT ck_file_object_sha256 CHECK (sha256 ~ '^[0-9A-Fa-f]{64}$')
);
CREATE INDEX idx_file_object_owner_created
ON file_object (owner_id, created_at DESC) WHERE is_deleted = FALSE;
COMMENT ON TABLE file_object IS 'OSS私有文件元数据';
COMMENT ON COLUMN file_object.id IS '随机文件编号';
COMMENT ON COLUMN file_object.owner_id IS '文件所有者';
COMMENT ON COLUMN file_object.original_name IS '上传时安全文件名';
COMMENT ON COLUMN file_object.object_key IS 'OSS 私有对象键';
COMMENT ON COLUMN file_object.content_type IS 'MIME 类型';
COMMENT ON COLUMN file_object.size_bytes IS '文件字节数';
COMMENT ON COLUMN file_object.sha256 IS '内容 SHA-256';
COMMENT ON COLUMN file_object.created_at IS '上传时间';
COMMENT ON COLUMN file_object.is_deleted IS '是否删除';
COMMENT ON COLUMN file_object.updated_at IS '更新时间';
