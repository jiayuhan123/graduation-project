package xyz.jiayuhan.teachingapi.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学习服务幂等回写一个学生的作业进度。 */
public record AssignmentProgressRequest(
        @NotNull Long practiceTaskId,
        @NotBlank
        @Pattern(regexp = "IN_PROGRESS|GRADED")
        String status,
        BigDecimal score,
        Integer correctCount,
        Integer totalCount,
        OffsetDateTime startedAt,
        OffsetDateTime submittedAt,
        OffsetDateTime gradedAt
) {
}
