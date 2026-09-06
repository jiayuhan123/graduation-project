package xyz.jiayuhan.teachingservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

/** 教学服务事务发件箱记录。 */
@Data
@TableName("teaching_outbox_event")
public class TeachingOutboxEvent {

    @TableId(type = IdType.INPUT)
    private UUID id;
    private String eventType;
    private String routingKey;
    private String payload;
    private String status;
    private Integer retryCount;
    private OffsetDateTime nextRetryAt;
    private OffsetDateTime createdAt;
    private OffsetDateTime publishedAt;
    private String lastError;
    private OffsetDateTime updatedAt;
}
