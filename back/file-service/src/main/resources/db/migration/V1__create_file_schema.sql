-- 文件服务只保存可查询元数据，二进制内容存放在阿里云 OSS 私有 Bucket。
CREATE TABLE IF NOT EXISTS file_object (
    id            UUID         PRIMARY KEY,
    owner_id      BIGINT       NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    object_key    VARCHAR(700) NOT NULL,
    content_type  VARCHAR(255) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    sha256        CHAR(64)     NOT NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_file_object_key UNIQUE (object_key),
    CONSTRAINT ck_file_object_size CHECK (size_bytes > 0)
);

CREATE INDEX IF NOT EXISTS idx_file_object_owner_created
    ON file_object (owner_id, created_at DESC)
    WHERE deleted = FALSE;
