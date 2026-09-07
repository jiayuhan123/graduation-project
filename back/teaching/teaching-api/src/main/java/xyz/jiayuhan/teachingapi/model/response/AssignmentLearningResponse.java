package xyz.jiayuhan.teachingapi.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** 学习服务创建作业任务所需的学生专属快照。 */
public record AssignmentLearningResponse(
        Long assignmentId,
        Long submissionId,
        Long studentId,
        String title,
        String assignmentStatus,
        String submissionStatus,
        String layerCode,
        Long practiceTaskId,
        OffsetDateTime startAt,
        OffsetDateTime dueAt,
        List<Question> questions
) {

    public record Question(
            Long questionId,
            Integer sequenceNo,
            BigDecimal score,
            String questionSnapshot
    ) {
    }
}
