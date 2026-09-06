package xyz.jiayuhan.teachingservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingservice.entity.Assignment;
import xyz.jiayuhan.teachingservice.entity.AssignmentQuestion;
import xyz.jiayuhan.teachingservice.entity.AssignmentSubmission;
import xyz.jiayuhan.teachingservice.entity.ClassStudent;
import xyz.jiayuhan.teachingservice.entity.ClassTeacher;
import xyz.jiayuhan.teachingservice.entity.TeachingClass;
import xyz.jiayuhan.teachingservice.event.TeachingAuditActions;
import xyz.jiayuhan.teachingservice.event.TeachingEventOutboxService;
import xyz.jiayuhan.teachingservice.mapper.AssignmentMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentQuestionMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentSubmissionMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassStudentMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassTeacherMapper;
import xyz.jiayuhan.teachingservice.mapper.TeachingClassMapper;
import xyz.jiayuhan.teachingservice.model.request.AddClassStudentRequest;
import xyz.jiayuhan.teachingservice.model.request.AssignmentQuestionRequest;
import xyz.jiayuhan.teachingservice.model.request.ChangeAssignmentStatusRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.JoinClassRequest;
import xyz.jiayuhan.teachingservice.model.request.PublishAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.StudentLayerAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateSubmissionCommentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateTeachingClassStatusRequest;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.request.BatchUserQueryRequest;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 教学写模型实现；写入业务数据时同步写入可靠审计发件箱。 */
@Service
@RequiredArgsConstructor
public class TeachingCommandServiceImpl implements TeachingCommandService {

    private static final String ACTIVE = "ACTIVE";
    private static final String DRAFT = "DRAFT";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String ALL = "ALL";
    private static final char[] CLASS_CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TeachingClassMapper teachingClassMapper;
    private final ClassTeacherMapper classTeacherMapper;
    private final ClassStudentMapper classStudentMapper;
    private final AssignmentMapper assignmentMapper;
    private final AssignmentQuestionMapper assignmentQuestionMapper;
    private final AssignmentSubmissionMapper assignmentSubmissionMapper;
    private final UserFeignClient userFeignClient;
    private final ContentFeignClient contentFeignClient;
    private final TeachingEventOutboxService outboxService;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.ClassMembership createClass(
            Long teacherId, CreateTeachingClassRequest request) {
        validateAcademicYear(request.academicYear());
        TeachingClass teachingClass = new TeachingClass();
        teachingClass.setClassCode(generateClassCode());
        applyClass(teachingClass, request.name(), request.schoolName(), request.academicYear(),
                request.gradeLevel(), request.semester());
        teachingClass.setStatus(ACTIVE);
        teachingClass.setCreatedBy(teacherId);
        teachingClass.setVersion(0);
        try {
            teachingClassMapper.insert(teachingClass);
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.TEACHING_CLASS_CODE_EXISTS);
        }

        ClassTeacher membership = new ClassTeacher();
        membership.setClassId(teachingClass.getId());
        membership.setTeacherId(teacherId);
        membership.setTeacherRole("OWNER");
        membership.setJoinedAt(now());
        classTeacherMapper.insert(membership);
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.CLASS_CREATED, "TEACHING_CLASS",
                teachingClass.getId(), "classCode=" + teachingClass.getClassCode());
        return toMembership(teachingClass, membership);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.ClassMembership updateClass(
            Long teacherId, Long classId, UpdateTeachingClassRequest request) {
        ManagedClass managed = requireManagedClass(teacherId, classId);
        verifyVersion(managed.teachingClass(), request.version());
        validateAcademicYear(request.academicYear());
        applyClass(managed.teachingClass(), request.name(), request.schoolName(), request.academicYear(),
                request.gradeLevel(), request.semester());
        assertUpdated(teachingClassMapper.updateById(managed.teachingClass()));
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.CLASS_UPDATED, "TEACHING_CLASS", classId,
                "classCode=" + managed.teachingClass().getClassCode());
        return toMembership(managed.teachingClass(), managed.membership());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.ClassMembership updateClassStatus(
            Long teacherId, Long classId, UpdateTeachingClassStatusRequest request) {
        ManagedClass managed = requireManagedClass(teacherId, classId);
        verifyVersion(managed.teachingClass(), request.version());
        managed.teachingClass().setStatus(request.status());
        assertUpdated(teachingClassMapper.updateById(managed.teachingClass()));
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.CLASS_STATUS_CHANGED, "TEACHING_CLASS", classId,
                "status=" + request.status());
        return toMembership(managed.teachingClass(), managed.membership());
    }

    @Override
    public TeachingView.ClassDetail getClassDetail(Long teacherId, Long classId) {
        ManagedClass managed = requireManagedClass(teacherId, classId);
        List<ClassStudent> memberships = classStudentMapper.selectList(
                new LambdaQueryWrapper<ClassStudent>()
                        .eq(ClassStudent::getClassId, classId)
                        .orderByAsc(ClassStudent::getStatus)
                        .orderByAsc(ClassStudent::getStudentNoInClass)
                        .orderByAsc(ClassStudent::getStudentId));
        Map<Long, UserSummaryResponse> users = usersById(
                memberships.stream().map(ClassStudent::getStudentId).toList());
        List<TeachingView.ClassStudent> students = memberships.stream()
                .map(membership -> toClassStudent(membership, users.get(membership.getStudentId())))
                .toList();
        return new TeachingView.ClassDetail(
                toMembership(managed.teachingClass(), managed.membership()), students);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.ClassStudent addStudent(
            Long teacherId, Long classId, AddClassStudentRequest request) {
        ManagedClass managed = requireManagedClass(teacherId, classId);
        requireActiveClass(managed.teachingClass());
        UserSummaryResponse student = requireActiveStudent(request.studentId());
        ClassStudent membership = upsertStudentMembership(
                classId, request.studentId(), request.studentNoInClass());
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.STUDENT_JOINED, "CLASS_STUDENT",
                classId + ":" + request.studentId(), "source=TEACHER");
        return toClassStudent(membership, student);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeStudent(Long teacherId, Long classId, Long studentId) {
        requireManagedClass(teacherId, classId);
        leaveMembership(classId, studentId);
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.STUDENT_LEFT, "CLASS_STUDENT",
                classId + ":" + studentId, "source=TEACHER");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.ClassMembership joinClass(Long studentId, JoinClassRequest request) {
        TeachingClass teachingClass = teachingClassMapper.selectOne(
                new LambdaQueryWrapper<TeachingClass>()
                        .eq(TeachingClass::getClassCode, request.classCode().trim().toUpperCase()));
        if (teachingClass == null) {
            throw new BusinessException(ResultCode.TEACHING_CLASS_NOT_FOUND);
        }
        requireActiveClass(teachingClass);
        ClassStudent membership = upsertStudentMembership(
                teachingClass.getId(), studentId, request.studentNoInClass());
        outboxService.appendAudit(
                studentId, TeachingAuditActions.STUDENT_JOINED, "CLASS_STUDENT",
                teachingClass.getId() + ":" + studentId, "source=STUDENT");
        return toMembership(teachingClass, membership);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void leaveClass(Long studentId, Long classId) {
        leaveMembership(classId, studentId);
        outboxService.appendAudit(
                studentId, TeachingAuditActions.STUDENT_LEFT, "CLASS_STUDENT",
                classId + ":" + studentId, "source=STUDENT");
    }

    @Override
    public PageResult<TeachingView.QuestionCandidate> pageQuestionCandidates(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionType,
            Integer difficulty) {
        Result<PageResult<QuestionResponse>> result = contentFeignClient.pageQuestions(
                page, size, trimToNull(keyword), knowledgePointId, questionType, difficulty);
        PageResult<QuestionResponse> source = requireRemoteData(result, "暂时无法读取已发布题目");
        return new PageResult<>(source.page(), source.size(), source.total(), source.pages(),
                source.records().stream().map(this::toCandidate).toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.AssignmentDetail createAssignment(
            Long teacherId, CreateAssignmentRequest request) {
        ManagedClass managed = requireManagedClass(teacherId, request.classId());
        requireActiveClass(managed.teachingClass());
        validateTimeRange(request.startAt(), request.dueAt());
        List<PreparedQuestion> questions = prepareQuestions(request.assignmentType(), request.questions());

        Assignment assignment = new Assignment();
        assignment.setClassId(request.classId());
        assignment.setTeacherId(teacherId);
        applyAssignment(assignment, request.title(), request.description(), request.assignmentType(),
                request.startAt(), request.dueAt(), request.settings(), totalScore(questions));
        assignment.setStatus(DRAFT);
        assignment.setVersion(0);
        assignmentMapper.insert(assignment);
        replaceQuestions(assignment.getId(), questions);
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.ASSIGNMENT_CREATED, "ASSIGNMENT",
                assignment.getId(), "classId=" + assignment.getClassId());
        return detail(assignment, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.AssignmentDetail updateAssignment(
            Long teacherId, Long assignmentId, UpdateAssignmentRequest request) {
        Assignment assignment = requireOwnedAssignment(teacherId, assignmentId);
        requireDraft(assignment);
        verifyVersion(assignment, request.version());
        validateTimeRange(request.startAt(), request.dueAt());
        List<PreparedQuestion> questions = prepareQuestions(request.assignmentType(), request.questions());
        applyAssignment(assignment, request.title(), request.description(), request.assignmentType(),
                request.startAt(), request.dueAt(), request.settings(), totalScore(questions));
        assertUpdated(assignmentMapper.updateById(assignment));
        replaceQuestions(assignmentId, questions);
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.ASSIGNMENT_UPDATED, "ASSIGNMENT",
                assignmentId, "classId=" + assignment.getClassId());
        return detail(assignment, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.AssignmentDetail publishAssignment(
            Long teacherId, Long assignmentId, PublishAssignmentRequest request) {
        Assignment assignment = requireOwnedAssignment(teacherId, assignmentId);
        requireDraft(assignment);
        verifyVersion(assignment, request.version());
        requireActiveClass(requireClass(assignment.getClassId()));
        List<AssignmentQuestion> existingQuestions = assignmentQuestions(assignmentId);
        if (existingQuestions.isEmpty()) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "作业没有题目");
        }
        List<AssignmentQuestionRequest> inputs = existingQuestions.stream()
                .map(item -> new AssignmentQuestionRequest(
                        item.getQuestionId(), item.getLayerCode(), item.getSequenceNo(), item.getScore()))
                .toList();
        List<PreparedQuestion> refreshed = prepareQuestions(assignment.getAssignmentType(), inputs);
        replaceQuestions(assignmentId, refreshed);

        List<ClassStudent> students = activeStudents(assignment.getClassId());
        if (students.isEmpty()) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "班级暂无在班学生，不能发布作业");
        }
        Map<Long, String> studentLayers = resolveStudentLayers(
                assignment.getAssignmentType(), refreshed, students, request.studentLayers());
        for (ClassStudent student : students) {
            AssignmentSubmission submission = new AssignmentSubmission();
            submission.setAssignmentId(assignmentId);
            submission.setStudentId(student.getStudentId());
            String layerCode = studentLayers.get(student.getStudentId());
            submission.setLayerCode(layerCode);
            submission.setStatus("ASSIGNED");
            submission.setCorrectCount(0);
            submission.setTotalCount((int) refreshed.stream()
                    .filter(item -> item.input().layerCode().equals(layerCode))
                    .count());
            submission.setVersion(0);
            assignmentSubmissionMapper.insert(submission);
        }

        assignment.setStatus(PUBLISHED);
        assignment.setPublishedAt(now());
        assertUpdated(assignmentMapper.updateById(assignment));
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.ASSIGNMENT_PUBLISHED, "ASSIGNMENT", assignmentId,
                "studentCount=" + students.size() + ",type=" + assignment.getAssignmentType());
        return detail(assignment, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.AssignmentDetail changeAssignmentStatus(
            Long teacherId, Long assignmentId, ChangeAssignmentStatusRequest request) {
        Assignment assignment = requireOwnedAssignment(teacherId, assignmentId);
        verifyVersion(assignment, request.version());
        String nextStatus = switch (request.action()) {
            case "CLOSE" -> {
                if (!PUBLISHED.equals(assignment.getStatus())) {
                    throw new BusinessException(ResultCode.ASSIGNMENT_STATE_INVALID);
                }
                yield "CLOSED";
            }
            case "CANCEL" -> {
                if (!DRAFT.equals(assignment.getStatus()) && !PUBLISHED.equals(assignment.getStatus())) {
                    throw new BusinessException(ResultCode.ASSIGNMENT_STATE_INVALID);
                }
                yield "CANCELLED";
            }
            default -> throw new BusinessException(ResultCode.BAD_REQUEST, "作业操作不正确");
        };
        assignment.setStatus(nextStatus);
        assertUpdated(assignmentMapper.updateById(assignment));
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.ASSIGNMENT_STATUS_CHANGED, "ASSIGNMENT",
                assignmentId, "status=" + nextStatus);
        return detail(assignment, true);
    }

    @Override
    public List<TeachingView.SubmissionDetail> listAssignmentSubmissions(
            Long teacherId, Long assignmentId, String status) {
        requireOwnedAssignment(teacherId, assignmentId);
        List<AssignmentSubmission> submissions = assignmentSubmissionMapper.selectList(
                new LambdaQueryWrapper<AssignmentSubmission>()
                        .eq(AssignmentSubmission::getAssignmentId, assignmentId)
                        .eq(StringUtils.hasText(status), AssignmentSubmission::getStatus, status)
                        .orderByAsc(AssignmentSubmission::getLayerCode)
                        .orderByAsc(AssignmentSubmission::getStudentId));
        Map<Long, UserSummaryResponse> users = usersById(
                submissions.stream().map(AssignmentSubmission::getStudentId).toList());
        return submissions.stream()
                .map(submission -> toSubmissionDetail(
                        submission, users.get(submission.getStudentId())))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeachingView.SubmissionDetail updateSubmissionComment(
            Long teacherId, Long submissionId, UpdateSubmissionCommentRequest request) {
        AssignmentSubmission submission = assignmentSubmissionMapper.selectById(submissionId);
        if (submission == null) {
            throw new BusinessException(ResultCode.ASSIGNMENT_SUBMISSION_NOT_FOUND);
        }
        requireOwnedAssignment(teacherId, submission.getAssignmentId());
        verifyVersion(submission, request.version());
        submission.setTeacherComment(trimToNull(request.teacherComment()));
        assertUpdated(assignmentSubmissionMapper.updateById(submission));
        outboxService.appendAudit(
                teacherId, TeachingAuditActions.SUBMISSION_COMMENTED, "ASSIGNMENT_SUBMISSION",
                submissionId, "assignmentId=" + submission.getAssignmentId());
        return toSubmissionDetail(submission, userById(submission.getStudentId()));
    }

    private void applyClass(
            TeachingClass teachingClass,
            String name,
            String schoolName,
            String academicYear,
            Integer gradeLevel,
            Integer semester) {
        teachingClass.setName(name.trim());
        teachingClass.setSchoolName(trimToNull(schoolName));
        teachingClass.setAcademicYear(academicYear.trim());
        teachingClass.setGradeLevel(gradeLevel);
        teachingClass.setSemester(semester);
    }

    private void applyAssignment(
            Assignment assignment,
            String title,
            String description,
            String assignmentType,
            OffsetDateTime startAt,
            OffsetDateTime dueAt,
            String settings,
            BigDecimal totalScore) {
        assignment.setTitle(title.trim());
        assignment.setDescription(trimToNull(description));
        assignment.setAssignmentType(assignmentType);
        assignment.setStartAt(startAt);
        assignment.setDueAt(dueAt);
        assignment.setSettings(normalizeJson(settings, "作业设置"));
        assignment.setTotalScore(totalScore);
    }

    private List<PreparedQuestion> prepareQuestions(
            String assignmentType, List<AssignmentQuestionRequest> inputs) {
        Set<String> sequenceKeys = new HashSet<>();
        Set<String> questionKeys = new HashSet<>();
        Set<String> layers = new HashSet<>();
        for (AssignmentQuestionRequest input : inputs) {
            String layerCode = input.layerCode().trim().toUpperCase();
            if (!sequenceKeys.add(layerCode + ":" + input.sequenceNo())) {
                throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "同一层级内题目顺序不能重复");
            }
            if (!questionKeys.add(layerCode + ":" + input.questionId())) {
                throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "同一层级内不能重复选择题目");
            }
            layers.add(layerCode);
        }
        if ("UNIFIED".equals(assignmentType) && !layers.equals(Set.of(ALL))) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "统一作业的层级必须全部为ALL");
        }
        if ("LAYERED".equals(assignmentType) && (layers.contains(ALL) || layers.size() < 2)) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "分层作业至少需要两个非ALL层级");
        }

        Map<Long, QuestionResponse> questions = enabledQuestions(
                inputs.stream().map(AssignmentQuestionRequest::questionId).distinct().toList());
        List<PreparedQuestion> prepared = new ArrayList<>();
        for (AssignmentQuestionRequest input : inputs) {
            QuestionResponse question = questions.get(input.questionId());
            AssignmentQuestionRequest normalized = new AssignmentQuestionRequest(
                    input.questionId(), input.layerCode().trim().toUpperCase(),
                    input.sequenceNo(), input.score());
            prepared.add(new PreparedQuestion(
                    normalized, question, objectMapper.writeValueAsString(question)));
        }
        validateLayerScores(prepared);
        return prepared;
    }

    private Map<Long, QuestionResponse> enabledQuestions(List<Long> questionIds) {
        Result<List<QuestionResponse>> result = contentFeignClient.getQuestions(
                new BatchQuestionQueryRequest(questionIds));
        List<QuestionResponse> questions = requireRemoteData(result, "暂时无法校验作业题目");
        Map<Long, QuestionResponse> byId = questions.stream()
                .collect(Collectors.toMap(QuestionResponse::id, Function.identity()));
        if (byId.size() != questionIds.size()
                || byId.values().stream().anyMatch(question -> !"ENABLED".equals(question.status()))) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "作业只能选择已发布题目");
        }
        return byId;
    }

    private void validateLayerScores(List<PreparedQuestion> questions) {
        Map<String, BigDecimal> scores = new LinkedHashMap<>();
        for (PreparedQuestion question : questions) {
            scores.merge(question.input().layerCode(), question.input().score(), BigDecimal::add);
        }
        BigDecimal expected = scores.values().iterator().next();
        if (scores.values().stream().anyMatch(score -> score.compareTo(expected) != 0)) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "各作业层级总分必须一致");
        }
    }

    private BigDecimal totalScore(List<PreparedQuestion> questions) {
        String firstLayer = questions.getFirst().input().layerCode();
        return questions.stream()
                .filter(question -> question.input().layerCode().equals(firstLayer))
                .map(question -> question.input().score())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void replaceQuestions(Long assignmentId, List<PreparedQuestion> questions) {
        assignmentQuestionMapper.delete(new LambdaQueryWrapper<AssignmentQuestion>()
                .eq(AssignmentQuestion::getAssignmentId, assignmentId));
        for (PreparedQuestion prepared : questions) {
            AssignmentQuestion entity = new AssignmentQuestion();
            entity.setAssignmentId(assignmentId);
            entity.setLayerCode(prepared.input().layerCode());
            entity.setQuestionId(prepared.input().questionId());
            entity.setSequenceNo(prepared.input().sequenceNo());
            entity.setScore(prepared.input().score());
            entity.setQuestionSnapshot(prepared.snapshot());
            assignmentQuestionMapper.insert(entity);
        }
    }

    private Map<Long, String> resolveStudentLayers(
            String assignmentType,
            List<PreparedQuestion> questions,
            List<ClassStudent> students,
            List<StudentLayerAssignmentRequest> requestedLayers) {
        if ("UNIFIED".equals(assignmentType)) {
            if (!requestedLayers.isEmpty()) {
                throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID,
                        "统一作业不需要学生分层配置");
            }
            return students.stream().collect(Collectors.toMap(ClassStudent::getStudentId, ignored -> ALL));
        }
        Set<String> availableLayers = questions.stream()
                .map(question -> question.input().layerCode())
                .collect(Collectors.toSet());
        Set<Long> activeStudentIds = students.stream()
                .map(ClassStudent::getStudentId)
                .collect(Collectors.toSet());
        Map<Long, String> result = new HashMap<>();
        for (StudentLayerAssignmentRequest requested : requestedLayers) {
            String layerCode = requested.layerCode().trim().toUpperCase();
            if (!activeStudentIds.contains(requested.studentId()) || !availableLayers.contains(layerCode)
                    || result.putIfAbsent(requested.studentId(), layerCode) != null) {
                throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID,
                        "学生分层包含重复、非在班学生或不存在的层级");
            }
        }
        if (!result.keySet().equals(activeStudentIds)) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID,
                    "分层作业必须为每名在班学生指定层级");
        }
        return result;
    }

    private ClassStudent upsertStudentMembership(
            Long classId, Long studentId, String studentNoInClass) {
        ClassStudent existing = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId));
        if (existing != null && ACTIVE.equals(existing.getStatus())) {
            throw new BusinessException(ResultCode.TEACHING_ALREADY_JOINED);
        }
        String normalizedNo = trimToNull(studentNoInClass);
        OffsetDateTime joinedAt = now();
        try {
            if (existing == null) {
                ClassStudent membership = new ClassStudent();
                membership.setClassId(classId);
                membership.setStudentId(studentId);
                membership.setStudentNoInClass(normalizedNo);
                membership.setStatus(ACTIVE);
                membership.setJoinedAt(joinedAt);
                classStudentMapper.insert(membership);
                return membership;
            }
            existing.setStudentNoInClass(normalizedNo);
            existing.setStatus(ACTIVE);
            existing.setJoinedAt(joinedAt);
            existing.setLeftAt(null);
            classStudentMapper.update(null, new LambdaUpdateWrapper<ClassStudent>()
                    .eq(ClassStudent::getClassId, classId)
                    .eq(ClassStudent::getStudentId, studentId)
                    .set(ClassStudent::getStudentNoInClass, normalizedNo)
                    .set(ClassStudent::getStatus, ACTIVE)
                    .set(ClassStudent::getJoinedAt, joinedAt)
                    .set(ClassStudent::getLeftAt, null)
                    .set(ClassStudent::getUpdatedAt, joinedAt));
            return existing;
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(ResultCode.CONFLICT, "班内学号已被使用");
        }
    }

    private void leaveMembership(Long classId, Long studentId) {
        ClassStudent membership = classStudentMapper.selectOne(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId)
                .eq(ClassStudent::getStatus, ACTIVE));
        if (membership == null) {
            throw new BusinessException(ResultCode.TEACHING_MEMBERSHIP_NOT_FOUND);
        }
        OffsetDateTime leftAt = now();
        int updated = classStudentMapper.update(null, new LambdaUpdateWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStudentId, studentId)
                .eq(ClassStudent::getStatus, ACTIVE)
                .set(ClassStudent::getStatus, "LEFT")
                .set(ClassStudent::getLeftAt, leftAt)
                .set(ClassStudent::getUpdatedAt, leftAt));
        if (updated != 1) {
            throw new BusinessException(ResultCode.TEACHING_MEMBERSHIP_NOT_FOUND);
        }
    }

    private ManagedClass requireManagedClass(Long teacherId, Long classId) {
        TeachingClass teachingClass = requireClass(classId);
        ClassTeacher membership = classTeacherMapper.selectOne(new LambdaQueryWrapper<ClassTeacher>()
                .eq(ClassTeacher::getClassId, classId)
                .eq(ClassTeacher::getTeacherId, teacherId));
        if (membership == null) {
            throw new BusinessException(ResultCode.TEACHING_CLASS_NOT_FOUND);
        }
        return new ManagedClass(teachingClass, membership);
    }

    private TeachingClass requireClass(Long classId) {
        TeachingClass teachingClass = teachingClassMapper.selectById(classId);
        if (teachingClass == null) {
            throw new BusinessException(ResultCode.TEACHING_CLASS_NOT_FOUND);
        }
        return teachingClass;
    }

    private Assignment requireOwnedAssignment(Long teacherId, Long assignmentId) {
        Assignment assignment = assignmentMapper.selectById(assignmentId);
        if (assignment == null || !teacherId.equals(assignment.getTeacherId())) {
            throw new BusinessException(ResultCode.ASSIGNMENT_NOT_FOUND);
        }
        requireManagedClass(teacherId, assignment.getClassId());
        return assignment;
    }

    private UserSummaryResponse requireActiveStudent(Long studentId) {
        Result<UserValidationResponse> validationResult = userFeignClient.validateUser(studentId, "student");
        UserValidationResponse validation = requireRemoteData(
                validationResult, "暂时无法校验学生账号");
        if (!validation.exists() || !validation.active() || !validation.roleMatched()) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "学生账号不存在或不可用");
        }
        return userById(studentId);
    }

    private UserSummaryResponse userById(Long userId) {
        Result<UserSummaryResponse> result = userFeignClient.getUserSummary(userId);
        return requireRemoteData(result, "暂时无法读取用户资料");
    }

    private Map<Long, UserSummaryResponse> usersById(List<Long> userIds) {
        List<Long> distinct = userIds.stream().distinct().toList();
        if (distinct.isEmpty()) {
            return Map.of();
        }
        Map<Long, UserSummaryResponse> result = new HashMap<>();
        for (int start = 0; start < distinct.size(); start += 100) {
            List<Long> batch = distinct.subList(start, Math.min(start + 100, distinct.size()));
            Result<List<UserSummaryResponse>> response = userFeignClient.getUserSummaries(
                    new BatchUserQueryRequest(batch));
            requireRemoteData(response, "暂时无法读取班级成员").forEach(user -> result.put(user.id(), user));
        }
        return result;
    }

    private List<ClassStudent> activeStudents(Long classId) {
        return classStudentMapper.selectList(new LambdaQueryWrapper<ClassStudent>()
                .eq(ClassStudent::getClassId, classId)
                .eq(ClassStudent::getStatus, ACTIVE)
                .orderByAsc(ClassStudent::getStudentId));
    }

    private List<AssignmentQuestion> assignmentQuestions(Long assignmentId) {
        return assignmentQuestionMapper.selectList(new LambdaQueryWrapper<AssignmentQuestion>()
                .eq(AssignmentQuestion::getAssignmentId, assignmentId)
                .orderByAsc(AssignmentQuestion::getLayerCode)
                .orderByAsc(AssignmentQuestion::getSequenceNo));
    }

    private TeachingView.AssignmentDetail detail(Assignment assignment, boolean includeAnswer) {
        List<TeachingView.AssignmentQuestion> questions = assignmentQuestions(assignment.getId()).stream()
                .map(question -> toQuestionView(question, includeAnswer))
                .toList();
        return new TeachingView.AssignmentDetail(toSummary(assignment), questions);
    }

    private TeachingView.AssignmentQuestion toQuestionView(
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

    private AssignmentSummaryResponse toSummary(Assignment assignment) {
        return new AssignmentSummaryResponse(
                assignment.getId(), assignment.getClassId(), assignment.getTeacherId(),
                assignment.getTitle(), assignment.getDescription(), assignment.getAssignmentType(),
                assignment.getStatus(), assignment.getStartAt(), assignment.getDueAt(),
                assignment.getTotalScore(), assignment.getSettings(), assignment.getPublishedAt(),
                assignment.getVersion());
    }

    private TeachingView.ClassMembership toMembership(
            TeachingClass teachingClass, ClassTeacher membership) {
        return new TeachingView.ClassMembership(
                teachingClass.getId(), teachingClass.getClassCode(), teachingClass.getName(),
                teachingClass.getSchoolName(), teachingClass.getAcademicYear(),
                teachingClass.getGradeLevel(), teachingClass.getSemester(), teachingClass.getStatus(),
                membership.getTeacherRole(), ACTIVE, membership.getJoinedAt(),
                teachingClass.getCreatedBy(), teachingClass.getVersion());
    }

    private TeachingView.ClassMembership toMembership(
            TeachingClass teachingClass, ClassStudent membership) {
        return new TeachingView.ClassMembership(
                teachingClass.getId(), teachingClass.getClassCode(), teachingClass.getName(),
                teachingClass.getSchoolName(), teachingClass.getAcademicYear(),
                teachingClass.getGradeLevel(), teachingClass.getSemester(), teachingClass.getStatus(),
                "STUDENT", membership.getStatus(), membership.getJoinedAt(),
                teachingClass.getCreatedBy(), teachingClass.getVersion());
    }

    private TeachingView.ClassStudent toClassStudent(
            ClassStudent membership, UserSummaryResponse user) {
        return new TeachingView.ClassStudent(
                membership.getStudentId(), user == null ? null : user.username(),
                displayName(user), user == null ? null : user.avatarUrl(),
                membership.getStudentNoInClass(), membership.getStatus(),
                membership.getJoinedAt(), membership.getLeftAt());
    }

    private TeachingView.SubmissionDetail toSubmissionDetail(
            AssignmentSubmission submission, UserSummaryResponse user) {
        return new TeachingView.SubmissionDetail(
                toSubmission(submission), user == null ? null : user.username(),
                displayName(user), user == null ? null : user.avatarUrl());
    }

    private TeachingView.Submission toSubmission(AssignmentSubmission submission) {
        return new TeachingView.Submission(
                submission.getId(), submission.getAssignmentId(), submission.getStudentId(),
                submission.getLayerCode(), submission.getPracticeTaskId(), submission.getStatus(),
                submission.getScore(), submission.getCorrectCount(), submission.getTotalCount(),
                submission.getStartedAt(), submission.getSubmittedAt(), submission.getGradedAt(),
                submission.getTeacherComment(), submission.getVersion());
    }

    private TeachingView.QuestionCandidate toCandidate(QuestionResponse question) {
        return new TeachingView.QuestionCandidate(
                question.id(), question.code(), question.questionType(), question.stem(),
                question.options(), question.standardAnswer(), question.analysis(),
                question.difficulty(), question.estimatedSeconds());
    }

    private String displayName(UserSummaryResponse user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.realName())) {
            return user.realName();
        }
        if (StringUtils.hasText(user.nickname())) {
            return user.nickname();
        }
        return user.username();
    }

    private String generateClassCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(8);
            for (int index = 0; index < 8; index++) {
                code.append(CLASS_CODE_ALPHABET[RANDOM.nextInt(CLASS_CODE_ALPHABET.length)]);
            }
            String candidate = code.toString();
            if (!teachingClassMapper.exists(new LambdaQueryWrapper<TeachingClass>()
                    .eq(TeachingClass::getClassCode, candidate))) {
                return candidate;
            }
        }
        throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法生成班级码");
    }

    private void validateAcademicYear(String academicYear) {
        int startYear = Integer.parseInt(academicYear.substring(0, 4));
        int endYear = Integer.parseInt(academicYear.substring(5));
        if (endYear != startYear + 1) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "学年必须由连续两个年份组成");
        }
    }

    private void validateTimeRange(OffsetDateTime startAt, OffsetDateTime dueAt) {
        if (!dueAt.isAfter(startAt)) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "截止时间必须晚于开始时间");
        }
    }

    private String normalizeJson(String value, String fieldName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            objectMapper.readTree(normalized);
            return normalized;
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.BAD_REQUEST, fieldName + "不是合法 JSON");
        }
    }

    private void requireActiveClass(TeachingClass teachingClass) {
        if (!ACTIVE.equals(teachingClass.getStatus())) {
            throw new BusinessException(ResultCode.TEACHING_CLASS_INACTIVE);
        }
    }

    private void requireDraft(Assignment assignment) {
        if (!DRAFT.equals(assignment.getStatus())) {
            throw new BusinessException(ResultCode.ASSIGNMENT_STATE_INVALID);
        }
    }

    private void verifyVersion(xyz.jiayuhan.common.data.model.VersionedEntity entity, Integer version) {
        if (!version.equals(entity.getVersion())) {
            throw new BusinessException(ResultCode.TEACHING_VERSION_CONFLICT);
        }
    }

    private void assertUpdated(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ResultCode.TEACHING_VERSION_CONFLICT);
        }
    }

    private <T> T requireRemoteData(Result<T> result, String message) {
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        return result.getData();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private record ManagedClass(TeachingClass teachingClass, ClassTeacher membership) {
    }

    private record PreparedQuestion(
            AssignmentQuestionRequest input,
            QuestionResponse question,
            String snapshot) {
    }
}
