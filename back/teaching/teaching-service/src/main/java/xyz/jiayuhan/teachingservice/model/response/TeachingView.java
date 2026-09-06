package xyz.jiayuhan.teachingservice.model.response;

import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/** 教学端查询接口使用的不可变响应模型集合。 */
public final class TeachingView {

    private TeachingView() {
    }

    public record ClassMembership(
            Long classId,
            String classCode,
            String name,
            String schoolName,
            String academicYear,
            Integer gradeLevel,
            Integer semester,
            String classStatus,
            String membershipRole,
            String membershipStatus,
            OffsetDateTime joinedAt,
            Long createdBy,
            Integer version
    ) {
    }

    public record ClassStudent(
            Long studentId,
            String username,
            String displayName,
            String avatarUrl,
            String studentNoInClass,
            String status,
            OffsetDateTime joinedAt,
            OffsetDateTime leftAt
    ) {
    }

    public record ClassDetail(
            ClassMembership classInfo,
            List<ClassStudent> students
    ) {
    }

    public record QuestionCandidate(
            Long id,
            String code,
            String questionType,
            String stem,
            String options,
            String standardAnswer,
            String analysis,
            Integer difficulty,
            Integer estimatedSeconds
    ) {
    }

    public record AssignmentQuestion(
            Long id,
            String layerCode,
            Long questionId,
            Integer sequenceNo,
            BigDecimal score,
            String questionCode,
            String questionType,
            String stem,
            String options,
            String standardAnswer,
            String analysis,
            Integer difficulty
    ) {
    }

    public record AssignmentDetail(
            AssignmentSummaryResponse assignment,
            List<AssignmentQuestion> questions
    ) {
    }

    public record Submission(
            Long id,
            Long assignmentId,
            Long studentId,
            String layerCode,
            Long practiceTaskId,
            String status,
            BigDecimal score,
            Integer correctCount,
            Integer totalCount,
            OffsetDateTime startedAt,
            OffsetDateTime submittedAt,
            OffsetDateTime gradedAt,
            String teacherComment,
            Integer version
    ) {
    }

    public record SubmissionDetail(
            Submission submission,
            String username,
            String displayName,
            String avatarUrl
    ) {
    }

    public record StudentAssignment(
            AssignmentSummaryResponse assignment,
            Submission submission
    ) {
    }
}
