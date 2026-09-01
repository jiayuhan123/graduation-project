package xyz.jiayuhan.userservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 用户服务事务发件箱记录。
 *
 * <p>业务数据和待发布事件在同一数据库事务中提交，后台调度器随后把事件可靠投递到
 * RabbitMQ。Broker 确认前记录不会被标记为已发布，因此失败后可以安全重试。</p>
 */
@Data
@TableName("sys_outbox_event")
public class OutboxEvent {

    /** 与领域事件 ID 相同，便于端到端追踪和消费者幂等。 */
    @TableId(type = IdType.INPUT)
    private UUID id;

    /** 事件类型及版本，例如 user.registered.v1。 */
    private String eventType;

    /** RabbitMQ 路由键。 */
    private String routingKey;

    /** 领域事件的 JSON 快照。 */
    private String payload;

    /** PENDING、PUBLISHED 或 FAILED。 */
    private String status;

    /** 已发生的发布失败次数。 */
    private Integer retryCount;

    /** 下一次允许重试的时间。 */
    private OffsetDateTime nextRetryAt;

    /** 事件入箱时间。 */
    private OffsetDateTime createdAt;

    /** Broker 最终确认时间。 */
    private OffsetDateTime publishedAt;

    /** 最近一次失败摘要，最大保存 1000 字符。 */
    private String lastError;

    /** 最近更新时间。 */
    private OffsetDateTime updatedAt;
}
