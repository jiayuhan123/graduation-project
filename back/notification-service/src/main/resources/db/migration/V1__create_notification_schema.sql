CREATE TABLE IF NOT EXISTS notification (
    id              UUID         PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    type            VARCHAR(32)  NOT NULL,
    title           VARCHAR(128) NOT NULL,
    content         VARCHAR(2000) NOT NULL,
    read            BOOLEAN      NOT NULL DEFAULT FALSE,
    read_at         TIMESTAMP,
    source_event_id UUID         NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_notification_source_event UNIQUE (source_event_id)
);

CREATE INDEX IF NOT EXISTS idx_notification_user_created
    ON notification (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notification_user_unread
    ON notification (user_id, created_at DESC)
    WHERE read = FALSE;
