package xyz.jiayuhan.teachingservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingapi.model.response.AssignmentLearningResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.teachingservice.entity.Assignment;
import xyz.jiayuhan.teachingservice.entity.AssignmentQuestion;
import xyz.jiayuhan.teachingservice.entity.AssignmentSubmission;
import xyz.jiayuhan.teachingservice.entity.ClassStudent;
import xyz.jiayuhan.teachingservice.entity.ClassTeacher;
import xyz.jiayuhan.teachingservice.entity.TeachingClass;
import xyz.jiayuhan.teachingservice.mapper.AssignmentMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentQuestionMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentSubmissionMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassStudentMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassTeacherMapper;
import xyz.jiayuhan.teachingservice.mapper.TeachingClassMapper;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.model.response.TeachingOperationsStatisticsResponse;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 根据教师或学生关联表执行归属校验的教学查询实现。 */
@Service
@RequiredArgsConstructor
public class TeachingQueryServiceImpl implements TeachingQueryService {

    private final TeachingClassMapper teachingClassMapper;
    private final ClassTeacherMapper classTeacherMapper;
    private final ClassStudentMapper classStudentMapper;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentQuestionMapper assignmentQuestionMapper;
    private final AssignmentSubmissionMapper assignmentSubmissionMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<TeachingView.ClassMembership> pageTeachingClasses(
            Long teacherId, long page, long size) {
        Page<ClassTeacher> memberships = classTeacherMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ClassTeacher>()
                        .eq(ClassTeacher::getTeacherId, teacherId)
                        .orderByDesc(ClassTeacher::getJoinedAt));
        Map<Long, TeachingClass> classes = classesById(
                memberships.getRecords().stream().map(ClassTeacher::getClassId).toList());
        List<TeachingView.ClassMembership> records = memberships.getRecords().stream()
                .filter(membership -> classes.containsKey(membership.getClassId()))
                .map(membership -> toView(classes.get(membership.getClassId()), membership))
                .toList();
        return pageResult(memberships, records);
    }

    @Override
    public PageResult<TeachingView.ClassMembership> pageAllClasses(
            long page, long size, String status) {
        Page<TeachingClass> classes = teachingClassMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<TeachingClass>()
                        .eq(status != null, TeachingClass::getStatus, status)
                        .orderByDesc(TeachingClass::getCreatedAt)
                        .orderByDesc(TeachingClass::getId));
        List<TeachingView.ClassMembership> records = classes.getRecords().stream()
                .map(value -> new TeachingView.ClassMembership(
                        value.getId(), value.getClassCode(), value.getName(), value.getSchoolName(),
                        value.getAcademicYear(), value.getGradeLevel(), value.getSemester(), value.getStatus(),
                        "ADMIN", "ACTIVE", value.getCreatedAt(), value.getCreatedBy(), value.getVersion()))
                .toList();
        return pageResult(classes, records);
    }

    @Override
    public PageResult<TeachingView.ClassMembership> pageEnrolledClasses(
            Long studentId, long page, long size, String status) {
        Page<ClassStudent> memberships = classStudentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getStudentId, studentId)
                        .eq(status != null, ClassStudent::getStatus, status)
                        .orderByDesc(ClassStudent::getJoinedAt));
        Map<Long, TeachingClass> classes = classesById(
                memberships.getRecords().stream().map(ClassStudent::getClassId).toList());
        List<TeachingView.ClassMembership> records = memberships.getRecords().stream()
                .filter(membership -> classes.containsKey(membership.getClassId()))
                .map(membership -> toView(classes.get(membership.getClassId()), membership))
                .toList();
        return pageResult(memberships, records);
    }

    @Override
    public PageResult<AssignmentSummaryResponse> pageCreatedAssignments(
            Long teacherId, long page, long size, String status) {
        Page<Assignment> result = assignmentMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Assignment>()
                        .eq(Assignment::getTeacherId, teacherId)
                        .eq(status != null, Assignment::getStatus, status)
                        .orderByDesc(Assignment::getCreatedAt)
                        .orderByDesc(Assignment::getId));
        return pageResult(result, result.getRecords().stream().map(this::toSummary).toList());
    }

    @Override
    public PageResult<TeachingView.Submission> pageMySubmissions(
            Long studentId, long page, long size, String status) {
        Page<AssignmentSubmission> result = assignmentSubmissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getStudentId, studentId)
                        .eq(status != null, AssignmentSubmission::getStatus, status)
                        .orderByDesc(AssignmentSubmission::getCreatedAt)
                        .orderByDesc(AssignmentSubmission::getId));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public PageResult<TeachingView.StudentAssignment> pageStudentAssignments(
            Long studentId, long page, long size, String status) {
        Page<AssignmentSubmission> result = assignmentSubmissionMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getStudentId, studentId)
                        .eq(status != null, AssignmentSubmission::getStatus, status)
                        .orderByDesc(AssignmentSubmission::getCreatedAt)
                        .orderByDesc(AssignmentSubmission::getId));
        Map<Long, Assignment> assignments = assignmentsById(result.getRecords().stream()
                .map(AssignmentSubmission::getAssignmentId)
                .toList());
        List<TeachingView.StudentAssignment> records = result.getRecords().stream()
                .filter(submission -> assignments.containsKey(submission.getAssignmentId()))
                .map(submission -> new TeachingView.StudentAssignment(
                        toSummary(assignments.get(submission.getAssignmentId())), toView(submission)))
                .toList();
        return pageResult(result, records);
    }

    @Override
    public TeachingView.AssignmentDetail getAssignmentForUser(Long userId, Long assignmentId) {
        Assignment assignment = requireAssignment(assignmentId);
        boolean ownsAssignment = userId.equals(assignment.getTeacherId());
        AssignmentSubmission submission = assignmentSubmissionMapper.selectOne(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                        .eq(AssignmentSubmission::getStudentId, userId));
        if (!ownsAssignment && submission == null) {
            throw new BusinessException(ResultCode.ASSIGNMENT_NOT_FOUND);
        }
        return detail(assignment, ownsAssignment, ownsAssignment ? null : submission.getLayerCode());
    }

    @Override
    public AssignmentSummaryResponse getAssignmentInternal(Long assignmentId) {
        return toSummary(requireAssignment(assignmentId));
    }

    @Override
    public AssignmentLearningResponse getStudentAssignmentInternal(
            Long assignmentId, Long studentId) {
        Assignment assignment = requireAssignment(assignmentId);
        AssignmentSubmission submission = assignmentSubmissionMapper.selectOne(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                        .eq(AssignmentSubmission::getStudentId, studentId));
        if (submission == null) {
            throw new BusinessException(ResultCode.ASSIGNMENT_SUBMISSION_NOT_FOUND);
        }
        List<AssignmentLearningResponse.Question> questions = assignmentQuestionMapper.selectList(
                        new LambdaQueryWrapper<AssignmentQuestion>()
                                .eq(AssignmentQuestion::getAssignmentId, assignmentId)
                                .eq(AssignmentQuestion::getLayerCode, submission.getLayerCode())
                                .orderByAsc(AssignmentQuestion::getSequenceNo))
                .stream()
                .map(question -> new AssignmentLearningResponse.Question(
                        question.getQuestionId(), question.getSequenceNo(), question.getScore(),
                        question.getQuestionSnapshot()))
                .toList();
        return new AssignmentLearningResponse(
                assignmentId, submission.getId(), studentId, assignment.getTitle(),
                assignment.getStatus(), submission.getStatus(), submission.getLayerCode(),
                submission.getPracticeTaskId(), assignment.getStartAt(), assignment.getDueAt(),
                questions);
    }

    @Override
    public StudentAccessResponse canTeacherViewStudent(Long teacherId, Long studentId) {
        List<Long> classIds = classTeacherMapper.selectList(
                        new LambdaQueryWrapper<ClassTeacher>()
                                .eq(ClassTeacher::getTeacherId, teacherId))
                .stream()
                .map(ClassTeacher::getClassId)
                .toList();
        boolean accessible = !classIds.isEmpty()
                && classStudentMapper.selectCount(new LambdaQueryWrapper<ClassStudent>()
                        .in(ClassStudent::getClassId, classIds)
                        .eq(ClassStudent::getStudentId, studentId)
                        .eq(ClassStudent::getStatus, "ACTIVE")) > 0;
        return new StudentAccessResponse(teacherId, studentId, accessible);
    }

    @Override
    public ClassRosterResponse getClassRosterInternal(Long classId) {
        TeachingClass teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null) {
            throw new BusinessException(ResultCode.TEACHING_CLASS_NOT_FOUND);
        }
        List<Long> studentIds = classStudentMapper.selectList(
                        new LambdaQueryWrapper<ClassStudent>()
                                .eq(ClassStudent::getClassId, classId)
                                .eq(ClassStudent::getStatus, "ACTIVE")
                                .orderByAsc(ClassStudent::getStudentId))
                .stream()
                .map(ClassStudent::getStudentId)
                .distinct()
                .toList();
        return new ClassRosterResponse(
                teachingClass.getId(), teachingClass.getName(), teachingClass.getSchoolName(),
                teachingClass.getGradeLevel(), teachingClass.getSemester(),
                teachingClass.getStatus(), studentIds);
    }

    @Override
    public ClassAccessResponse canTeacherViewClass(Long teacherId, Long classId) {
        boolean accessible = classTeacherMapper.exists(new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getTeacherId, teacherId)
                .eq(ClassTeacher::getClassId, classId));
        return new ClassAccessResponse(teacherId, classId, accessible);
    }

    @Override
    public TeachingOperationsStatisticsResponse operationsStatistics() {
        long totalClasses = teachingClassMapper.selectCount(null);
        long activeClasses = teachingClassMapper.selectCount(
                new LambdaQueryWrapper<TeachingClass>().eq(TeachingClass::getStatus, "ACTIVE"));
        long teacherMemberships = classTeacherMapper.selectCount(null);
        long activeStudentMemberships = classStudentMapper.selectCount(
                new LambdaQueryWrapper<ClassStudent>().eq(ClassStudent::getStatus, "ACTIVE"));
        long totalAssignments = assignmentMapper.selectCount(null);
        long publishedAssignments = assignmentMapper.selectCount(
                new LambdaQueryWrapper<Assignment>()
                        .in(Assignment::getStatus, List.of("PUBLISHED", "CLOSED")));
        long totalSubmissions = assignmentSubmissionMapper.selectCount(null);
        long completedSubmissions = assignmentSubmissionMapper.selectCount(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .in(AssignmentSubmission::getStatus, List.of("SUBMITTED", "GRADED")));
        return new TeachingOperationsStatisticsResponse(
                totalClasses, activeClasses, teacherMemberships, activeStudentMemberships,
                totalAssignments, publishedAssignments, totalSubmissions, completedSubmissions);
    }

    private TeachingView.AssignmentDetail detail(
            Assignment assignment, boolean includeAnswer, String layerCode) {
        List<TeachingView.AssignmentQuestion> questions = assignmentQuestionMapper.selectList(
                        new LambdaQueryWrapper<AssignmentQuestion>()
                                .eq(AssignmentQuestion::getAssignmentId, assignment.getId())
                                .eq(layerCode != null, AssignmentQuestion::getLayerCode, layerCode)
                                .orderByAsc(AssignmentQuestion::getLayerCode)
                                .orderByAsc(AssignmentQuestion::getSequenceNo))
                .stream()
                .map(question -> toView(question, includeAnswer))
                .toList();
        return new TeachingView.AssignmentDetail(toSummary(assignment), questions);
    }

    private Assignment requireAssignment(Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null) {
            throw new BusinessException(ResultCode.ASSIGNMENT_NOT_FOUND);
        }
        return assignment;
    }

    private Map<Long, TeachingClass> classesById(List<Long> classIds) {
        if (classIds.isEmpty()) {
            return Map.of();
        }
        return teachingClassMapper.selectByIds(classIds).stream()
                .collect(Collectors.toMap(TeachingClass::getId, Function.identity()));
    }

    private Map<Long, Assignment> assignmentsById(List<Long> assignmentIds) {
        if (assignmentIds.isEmpty()) {
            return Map.of();
        }
        return assignmentMapper.selectByIds(assignmentIds).stream()
                .collect(Collectors.toMap(Assignment::getId, Function.identity()));
    }

    private TeachingView.ClassMembership toView(TeachingClass value, ClassTeacher membership) {
        return new TeachingView.ClassMembership(
                value.getId(), value.getClassCode(), value.getName(), value.getSchoolName(),
                value.getAcademicYear(), value.getGradeLevel(), value.getSemester(), value.getStatus(),
                membership.getTeacherRole(), "ACTIVE", membership.getJoinedAt(),
                value.getCreatedBy(), value.getVersion());
    }

    private TeachingView.ClassMembership toView(TeachingClass value, ClassStudent membership) {
        return new TeachingView.ClassMembership(
                value.getId(), value.getClassCode(), value.getName(), value.getSchoolName(),
                value.getAcademicYear(), value.getGradeLevel(), value.getSemester(), value.getStatus(),
                "STUDENT", membership.getStatus(), membership.getJoinedAt(),
                value.getCreatedBy(), value.getVersion());
    }

    private AssignmentSummaryResponse toSummary(Assignment assignment) {
        return new AssignmentSummaryResponse(
                assignment.getId(), assignment.getClassId(), assignment.getTeacherId(),
                assignment.getTitle(), assignment.getDescription(), assignment.getAssignmentType(),
                assignment.getStatus(), assignment.getStartAt(), assignment.getDueAt(),
                assignment.getTotalScore(), assignment.getSettings(), assignment.getPublishedAt(),
                assignment.getVersion());
    }

    private TeachingView.AssignmentQuestion toView(
            AssignmentQuestion question, boolean includeAnswer) {
        QuestionResponse snapshot;
        try {
            snapshot = objectMapper.readValue(question.getQuestionSnapshot(), QuestionResponse.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "作业题目快照损坏");
        }
        return new TeachingView.AssignmentQuestion(
                question.getId(), question.getLayerCode(), question.getQuestionId(),
                question.getSequenceNo(), question.getScore(), snapshot.code(), snapshot.questionType(),
                snapshot.stem(), snapshot.options(), includeAnswer ? snapshot.standardAnswer() : null,
                includeAnswer ? snapshot.analysis() : null, snapshot.difficulty());
    }

    private TeachingView.Submission toView(AssignmentSubmission submission) {
        return new TeachingView.Submission(
                submission.getId(), submission.getAssignmentId(), submission.getStudentId(),
                submission.getLayerCode(),
                submission.getPracticeTaskId(), submission.getStatus(), submission.getScore(),
                submission.getCorrectCount(), submission.getTotalCount(), submission.getStartedAt(),
                submission.getSubmittedAt(), submission.getGradedAt(), submission.getTeacherComment(),
                submission.getVersion());
    }

    private <T> PageResult<T> pageResult(Page<?> page, List<T> records) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }
}
