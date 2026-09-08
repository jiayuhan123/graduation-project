package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.time.OffsetDateTime;

/** 游戏结果对外可靠同步使用的事务发件箱。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "game_event_outbox", autoResultMap = true)
public class GameEventOutbox extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String eventKey;
    private String eventType;
    private String aggregateType;
    private Long aggregateId;
    private Long studentId;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String payload;
    private String deliveryStatus;
    private Integer retryCount;
    private OffsetDateTime nextRetryAt;
    private OffsetDateTime sentAt;
    private String lastError;
}
