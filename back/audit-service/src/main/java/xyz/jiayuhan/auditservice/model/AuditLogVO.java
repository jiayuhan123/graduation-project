package xyz.jiayuhan.auditservice.model;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 当前用户可查看的操作审计响应。
 *
 * @param id 审计记录 ID
 * @param source 来源服务
 * @param action 操作编码
 * @param targetType 对象类型
 * @param targetId 对象 ID
 * @param detail 不含敏感信息的说明
 * @param occurredAt 操作发生时间
 */
public record AuditLogVO(
        UUID id,
        String source,
        String action,
        String targetType,
        String targetId,
        String detail,
        LocalDateTime occurredAt
) {
}
