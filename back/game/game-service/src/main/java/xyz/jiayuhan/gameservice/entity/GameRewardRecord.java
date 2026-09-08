package xyz.jiayuhan.gameservice.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import xyz.jiayuhan.common.data.model.AuditableEntity;

/** 具有幂等键的积分或经验奖励流水。 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("game_reward_record")
public class GameRewardRecord extends AuditableEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private Long studentId;
    private Long sessionId;
    private String businessType;
    private String rewardType;
    private Long amount;
    private Long balanceAfter;
    private String reason;
    private String idempotencyKey;
}
