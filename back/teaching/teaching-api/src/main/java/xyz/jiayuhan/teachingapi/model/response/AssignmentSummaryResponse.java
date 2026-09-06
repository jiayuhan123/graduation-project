package xyz.jiayuhan.teachingapi.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 学习服务读取的教师作业最小契约。 */
public record AssignmentSummaryResponse(
        Long id,
        Long classId,
        Long teacherId,
        String title,
        String description,
        String assignmentType,
        String status,
        OffsetDateTime startAt,
        OffsetDateTime dueAt,
        BigDecimal totalScore,
        String settings,
        OffsetDateTime publishedAt,
        Integer version
) {
}
