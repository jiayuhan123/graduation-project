-- 事务发件箱：业务事务只负责入箱，后台调度器在 Broker 确认后标记为已发布。
CREATE TABLE IF NOT EXISTS sys_outbox_event (
    id            UUID         PRIMARY KEY,
    event_type    VARCHAR(128) NOT NULL,
    routing_key   VARCHAR(128) NOT NULL,
    payload       TEXT         NOT NULL,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PENDING',
    retry_count   INTEGER      NOT NULL DEFAULT 0,
    next_retry_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at  TIMESTAMP,
    last_error    VARCHAR(1000),
    CONSTRAINT ck_sys_outbox_event_status
        CHECK (status IN ('PENDING', 'PUBLISHED', 'FAILED')),
    CONSTRAINT ck_sys_outbox_event_retry_count CHECK (retry_count >= 0)
);

-- 调度器只扫描到期的待发布记录；已发布历史不会进入热索引。
CREATE INDEX IF NOT EXISTS idx_sys_outbox_event_pending
    ON sys_outbox_event (next_retry_at, created_at)
    WHERE status = 'PENDING';

-- 为按保留期清理成功历史提供部分索引；FAILED 记录不参与自动清理。
CREATE INDEX IF NOT EXISTS idx_sys_outbox_event_published
    ON sys_outbox_event (published_at)
    WHERE status = 'PUBLISHED';
