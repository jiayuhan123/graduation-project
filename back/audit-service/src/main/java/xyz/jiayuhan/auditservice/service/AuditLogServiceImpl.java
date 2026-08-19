package xyz.jiayuhan.auditservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.auditservice.entity.AuditLog;
import xyz.jiayuhan.auditservice.mapper.AuditLogMapper;
import xyz.jiayuhan.auditservice.model.AuditLogVO;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** PostgreSQL 审计日志实现，以来源 eventId 唯一约束保证至少一次消费的幂等性。 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogMapper auditLogMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(UserRegisteredEvent event) {
        store(
                event.eventId(),
                event.userId(),
                event.source(),
                "USER_REGISTERED",
                "USER",
                String.valueOf(event.userId()),
                "username=" + event.username(),
                LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(AuditLogEvent event) {
        store(
                event.eventId(),
                event.operatorId(),
                event.source(),
                event.action(),
                event.targetType(),
                event.targetId(),
                event.detail(),
                LocalDateTime.ofInstant(event.occurredAt(), ZoneId.systemDefault()));
    }

    @Override
    public PageResult<AuditLogVO> pageByOperator(
            Long operatorId,
            long page,
            long size,
            String action) {
        Page<AuditLog> result = auditLogMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AuditLog>()
                        .eq(AuditLog::getOperatorId, operatorId)
                        .eq(StringUtils.hasText(action), AuditLog::getAction, action)
                        .orderByDesc(AuditLog::getOccurredAt));
        return new PageResult<>(
                result.getCurrent(),
                result.getSize(),
                result.getTotal(),
                result.getPages(),
                result.getRecords().stream().map(this::toVO).toList());
    }

    private void store(
            UUID eventId,
            Long operatorId,
            String source,
            String action,
            String targetType,
            String targetId,
            String detail,
            LocalDateTime occurredAt) {
        boolean consumed = auditLogMapper.exists(
                new LambdaQueryWrapper<AuditLog>().eq(AuditLog::getEventId, eventId));
        if (consumed) {
            return;
        }
        AuditLog auditLog = new AuditLog();
        auditLog.setId(UUID.randomUUID());
        auditLog.setEventId(eventId);
        auditLog.setOperatorId(operatorId);
        auditLog.setSource(source);
        auditLog.setAction(action);
        auditLog.setTargetType(targetType);
        auditLog.setTargetId(targetId);
        auditLog.setDetail(detail);
        auditLog.setOccurredAt(occurredAt);
        auditLogMapper.insert(auditLog);
    }

    private AuditLogVO toVO(AuditLog auditLog) {
        return new AuditLogVO(
                auditLog.getId(),
                auditLog.getSource(),
                auditLog.getAction(),
                auditLog.getTargetType(),
                auditLog.getTargetId(),
                auditLog.getDetail(),
                auditLog.getOccurredAt());
    }
}
