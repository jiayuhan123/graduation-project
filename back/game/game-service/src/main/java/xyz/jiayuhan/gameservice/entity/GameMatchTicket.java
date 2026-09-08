package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;

import java.time.OffsetDateTime;

/** 双人 PK 随机匹配票据。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_match_ticket")
public class GameMatchTicket extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long studentId;
    private String status;
    private Integer gradeLevel;
    private Long knowledgePointId;
    private Integer difficulty;
    private Integer questionCount;
    private Integer timeLimitSeconds;
    private Long matchedSessionId;
    private OffsetDateTime expiresAt;
    private OffsetDateTime matchedAt;
}
