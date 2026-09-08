package xyz.jiayuhan.gameservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 创建房间或发起随机匹配时使用的双人 PK 规则。 */
public record CreatePkSessionRequest(
        @NotNull(message = "年级不能为空")
        @Min(value = 1, message = "年级必须在1到6之间")
        @Max(value = 6, message = "年级必须在1到6之间")
        Integer gradeLevel,

        @NotNull(message = "知识点不能为空")
        Long knowledgePointId,

        @NotNull(message = "难度不能为空")
        @Min(value = 1, message = "难度必须在1到5之间")
        @Max(value = 5, message = "难度必须在1到5之间")
        Integer difficulty,

        @Min(value = 1, message = "题量至少为1")
        @Max(value = 20, message = "PK题量最多为20")
        Integer questionCount,

        @Min(value = 30, message = "PK时限不能少于30秒")
        @Max(value = 1800, message = "PK时限不能超过1800秒")
        Integer timeLimitSeconds
) {
}
