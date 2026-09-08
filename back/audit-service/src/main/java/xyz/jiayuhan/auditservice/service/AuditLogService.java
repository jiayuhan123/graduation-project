package xyz.jiayuhan.auditservice.service;

import xyz.jiayuhan.auditservice.model.AuditLogVO;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;

import java.time.LocalDateTime;

/** 审计事件幂等入库与当前用户审计查询服务。 */
public interface AuditLogService {

    void record(UserRegisteredEvent event);

    void record(AuditLogEvent event);

    PageResult<AuditLogVO> pageByOperator(Long operatorId, long page, long size, String action);

    PageResult<AuditLogVO> page(
            long page,
            long size,
            Long operatorId,
            String source,
            String action,
            String targetType,
            String targetId,
            LocalDateTime occurredFrom,
            LocalDateTime occurredTo);
}
