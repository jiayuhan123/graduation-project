package xyz.jiayuhan.auditservice.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import xyz.jiayuhan.auditservice.service.AuditLogService;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.AuditLogEvent;
import xyz.jiayuhan.common.mq.event.UserRegisteredEvent;

/** 消费用户注册和通用操作事件并持久化为审计记录。 */
@Component
@RequiredArgsConstructor
public class AuditEventConsumer {

    private final AuditLogService auditLogService;

    @RabbitListener(queues = MqTopology.AUDIT_USER_REGISTERED_QUEUE)
    public void onUserRegistered(UserRegisteredEvent event) {
        auditLogService.record(event);
    }

    @RabbitListener(queues = MqTopology.AUDIT_OPERATION_QUEUE)
    public void onAuditOperation(AuditLogEvent event) {
        auditLogService.record(event);
    }
}
