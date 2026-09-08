package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.VersionedEntity;
import xyz.jiayuhan.common.data.typehandler.JsonbTypeHandler;

import java.math.BigDecimal;

/** 可配置的单人或团队闯关关卡。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName(value = "game_level", autoResultMap = true)
public class GameLevel extends VersionedEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String code;
    private String name;
    private String description;
    private Integer gradeLevel;
    private Long knowledgePointId;
    private Integer difficulty;
    private String questionSource;
    private String mode;
    private Integer questionCount;
    private Integer timeLimitSeconds;
    private BigDecimal passRate;
    private Integer minPlayers;
    private Integer maxPlayers;
    private Integer basePoints;
    private Integer baseExperience;
    private Long previousLevelId;
    private Integer sortOrder;
    private String status;
    @TableField(typeHandler = JsonbTypeHandler.class)
    private String ruleConfig;
    private Long createdBy;
}
