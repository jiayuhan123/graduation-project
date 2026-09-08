package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

import java.math.BigDecimal;

/** 关卡使用的固定题目池配置。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_level_question")
public class GameLevelQuestion extends AuditableEntity {

    private Long levelId;
    private Long questionId;
    private Integer sequenceNo;
    private BigDecimal weight;
    private Boolean required;
}
