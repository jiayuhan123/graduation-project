package xyz.jiayuhan.notificationservice.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/** 用户站内通知实体。 */
@Data
@TableName("notification")
public class Notification {

    @TableId(type = IdType.INPUT)
    private UUID id;

    /** 接收通知的用户 ID。 */
    private Long userId;

    /** 通知类型，例如 SYSTEM。 */
    private String type;

    private String title;
    private String content;

    /** 是否已读。 */
    private Boolean read;

    private LocalDateTime readAt;

    /** 触发通知的领域事件 ID，用唯一约束保证消息重复投递时不重复建通知。 */
    private UUID sourceEventId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
