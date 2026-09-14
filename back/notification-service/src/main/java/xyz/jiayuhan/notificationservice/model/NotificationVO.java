package xyz.jiayuhan.notificationservice.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 站内通知响应。
 *
 * @param id 通知 ID
 * @param type 通知类型
 * @param title 标题
 * @param content 内容
 * @param read 是否已读
 * @param readAt 阅读时间
 * @param createdAt 创建时间
 */
public record NotificationVO(
        UUID id,
        String type,
        String title,
        String content,
        boolean read,
        OffsetDateTime readAt,
        OffsetDateTime createdAt
) {
}
