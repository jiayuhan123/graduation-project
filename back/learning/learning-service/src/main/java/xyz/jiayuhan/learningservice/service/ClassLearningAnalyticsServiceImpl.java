package xyz.jiayuhan.learningservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.ErrorTypeResponse;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.learningservice.entity.AnswerRecord;
import xyz.jiayuhan.learningservice.entity.ErrorRecord;
import xyz.jiayuhan.learningservice.entity.MasteryRecord;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.mapper.AnswerRecordMapper;
import xyz.jiayuhan.learningservice.mapper.ErrorRecordMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskMapper;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.request.BatchUserQueryRequest;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 基于统一学习事实表生成班级看板，统计窗口限制为 7 至 90 天。 */
@Service
@RequiredArgsConstructor
public class ClassLearningAnalyticsServiceImpl implements ClassLearningAnalyticsService {

    private static final String REPORT_PERMISSION = "teaching:report:query";
    private static final BigDecimal WEAK_THRESHOLD = new BigDecimal("0.6000");

    private final PracticeTaskMapper practiceTaskMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final ErrorRecordMapper errorRecordMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final TeachingFeignClient teachingFeignClient;
    private final UserFeignClient userFeignClient;
    private final ContentFeignClient contentFeignClient;
    private final LearningCommandService learningCommandService;

    @Override
    public LearningView.ClassDashboard getDashboard(Long requesterId, Long classId, int days) {
        authorize(requesterId, classId);
        ClassRosterResponse roster = requireRemoteData(
                () -> teachingFeignClient.getClassRoster(classId), "暂时无法读取班级花名册");
        List<Long> studentIds = roster.studentIds() == null
                ? List.of() : roster.studentIds().stream().distinct().toList();
        OffsetDateTime rangeEnd = OffsetDateTime.now(ZoneOffset.UTC);
        OffsetDateTime rangeStart = rangeEnd.minusDays(days);
        if (studentIds.isEmpty()) {
            return emptyDashboard(roster, days, rangeStart, rangeEnd);
        }

        List<PracticeTask> tasks = practiceTaskMapper.selectList(
                new LambdaQueryWrapper<PracticeTask>()
                        .in(PracticeTask::getStudentId, studentIds)
                        .ge(PracticeTask::getCreatedAt, rangeStart)
                        .le(PracticeTask::getCreatedAt, rangeEnd));
        List<AnswerRecord> answers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .in(AnswerRecord::getStudentId, studentIds)
                        .eq(AnswerRecord::getGradingStatus, "GRADED")
                        .ge(AnswerRecord::getSubmittedAt, rangeStart)
                        .le(AnswerRecord::getSubmittedAt, rangeEnd));
        List<ErrorRecord> errors = errorRecordMapper.selectList(
                new LambdaQueryWrapper<ErrorRecord>()
                        .in(ErrorRecord::getStudentId, studentIds)
                        .ge(ErrorRecord::getCreatedAt, rangeStart)
                        .le(ErrorRecord::getCreatedAt, rangeEnd));
        List<ErrorRecord> openErrors = errorRecordMapper.selectList(
                new LambdaQueryWrapper<ErrorRecord>()
                        .in(ErrorRecord::getStudentId, studentIds)
                        .ne(ErrorRecord::getStatus, "RESOLVED"));
        List<MasteryRecord> mastery = masteryRecordMapper.selectList(
                new LambdaQueryWrapper<MasteryRecord>()
                        .in(MasteryRecord::getStudentId, studentIds));
        Map<Long, UserSummaryResponse> users = usersById(studentIds);

        long completedTasks = tasks.stream()
                .filter(task -> "COMPLETED".equals(task.getStatus()))
                .count();
        long correctAnswers = answers.stream()
                .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                .count();
        LearningView.ClassMetrics metrics = new LearningView.ClassMetrics(
                studentIds.size(), tasks.size(), completedTasks, ratio(completedTasks, tasks.size()),
                answers.size(), correctAnswers, ratio(correctAnswers, answers.size()), openErrors.size());
        List<LearningView.StudentSnapshot> students = studentSnapshots(
                studentIds, users, tasks, answers, openErrors, mastery);
        return new LearningView.ClassDashboard(
                roster.classId(), roster.className(), roster.schoolName(), roster.gradeLevel(),
                roster.semester(), days, rangeStart, rangeEnd, metrics,
                weakKnowledgePoints(mastery), frequentQuestions(errors), frequentErrorTypes(errors),
                dailyTrends(answers, days, rangeEnd.toLocalDate()), students,
                studentAlerts(students, rangeEnd));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.ClassPracticeCreationResult createTargetedPractice(
            Long requesterId, Long classId, CreatePracticeTaskRequest request) {
        authorize(requesterId, classId);
        ClassRosterResponse roster = requireRemoteData(
                () -> teachingFeignClient.getClassRoster(classId), "暂时无法读取班级花名册");
        List<Long> taskIds = new ArrayList<>();
        List<Long> studentIds = roster.studentIds() == null ? List.of() : roster.studentIds();
        for (Long studentId : studentIds.stream().distinct().toList()) {
            taskIds.add(learningCommandService.createSelfPractice(studentId, request).task().id());
        }
        return new LearningView.ClassPracticeCreationResult(taskIds.size(), taskIds);
    }

    private void authorize(Long requesterId, Long classId) {
        UserSummaryResponse requester = requireRemoteData(
                () -> userFeignClient.getUserSummary(requesterId), "暂时无法读取当前用户资料");
        UserPermissionResponse permission = requireRemoteData(
                () -> userFeignClient.authorizeUser(requesterId, REPORT_PERMISSION),
                "暂时无法校验学情查询权限");
        if (!permission.exists() || !permission.active() || !permission.granted()) {
            throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
        }
        if (requester.roles() != null && requester.roles().contains("admin")) {
            return;
        }
        ClassAccessResponse access = requireRemoteData(
                () -> teachingFeignClient.canViewClass(requesterId, classId),
                "暂时无法校验班级管理关系");
        if (!access.accessible()) {
            throw new BusinessException(ResultCode.LEARNING_REPORT_FORBIDDEN);
        }
    }

    private LearningView.ClassDashboard emptyDashboard(
            ClassRosterResponse roster,
            int days,
            OffsetDateTime rangeStart,
            OffsetDateTime rangeEnd) {
        LearningView.ClassMetrics metrics = new LearningView.ClassMetrics(
                0, 0, 0, zeroRate(), 0, 0, zeroRate(), 0);
        return new LearningView.ClassDashboard(
                roster.classId(), roster.className(), roster.schoolName(), roster.gradeLevel(),
                roster.semester(), days, rangeStart, rangeEnd, metrics, List.of(), List.of(),
                List.of(), dailyTrends(List.of(), days, rangeEnd.toLocalDate()), List.of(), List.of());
    }

    private List<LearningView.WeakKnowledgePoint> weakKnowledgePoints(
            List<MasteryRecord> mastery) {
        Map<Long, List<MasteryRecord>> grouped = mastery.stream()
                .collect(Collectors.groupingBy(MasteryRecord::getKnowledgePointId));
        return grouped.entrySet().stream()
                .map(entry -> {
                    List<MasteryRecord> values = entry.getValue();
                    BigDecimal averageMastery = average(values.stream()
                            .map(MasteryRecord::getMasteryScore).toList());
                    BigDecimal averageAccuracy = average(values.stream()
                            .map(MasteryRecord::getAccuracyRate).toList());
                    int affected = (int) values.stream()
                            .filter(value -> value.getMasteryScore().compareTo(WEAK_THRESHOLD) < 0)
                            .count();
                    return new LearningView.WeakKnowledgePoint(
                            entry.getKey(), knowledgePointName(entry.getKey()), averageMastery,
                            averageAccuracy, affected, values.size());
                })
                .filter(item -> item.affectedStudentCount() > 0)
                .sorted(Comparator.comparingInt(LearningView.WeakKnowledgePoint::affectedStudentCount)
                        .reversed()
                        .thenComparing(LearningView.WeakKnowledgePoint::averageMastery))
                .limit(10)
                .toList();
    }

    private List<LearningView.FrequentQuestion> frequentQuestions(List<ErrorRecord> errors) {
        Map<Long, List<ErrorRecord>> grouped = errors.stream()
                .filter(error -> error.getQuestionId() != null)
                .collect(Collectors.groupingBy(ErrorRecord::getQuestionId));
        List<Long> questionIds = grouped.entrySet().stream()
                .sorted(Map.Entry.<Long, List<ErrorRecord>>comparingByValue(
                        Comparator.comparingInt(List::size)).reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
        Map<Long, QuestionResponse> questions = questionsById(questionIds);
        return questionIds.stream().map(questionId -> {
            List<ErrorRecord> values = grouped.get(questionId);
            QuestionResponse question = questions.get(questionId);
            return new LearningView.FrequentQuestion(
                    questionId, question == null ? null : question.code(),
                    question == null ? "题目 #" + questionId : question.stem(),
                    values.size(), distinctStudents(values));
        }).toList();
    }

    private List<LearningView.FrequentErrorType> frequentErrorTypes(List<ErrorRecord> errors) {
        Map<Long, List<ErrorRecord>> grouped = errors.stream()
                .filter(error -> error.getErrorTypeId() != null)
                .collect(Collectors.groupingBy(ErrorRecord::getErrorTypeId));
        Map<Long, ErrorTypeResponse> types = errorTypesById();
        return grouped.entrySet().stream()
                .sorted(Map.Entry.<Long, List<ErrorRecord>>comparingByValue(
                        Comparator.comparingInt(List::size)).reversed())
                .limit(10)
                .map(entry -> {
                    ErrorTypeResponse type = types.get(entry.getKey());
                    return new LearningView.FrequentErrorType(
                            entry.getKey(), type == null ? "错因 #" + entry.getKey() : type.name(),
                            type == null ? null : type.category(), entry.getValue().size(),
                            distinctStudents(entry.getValue()));
                })
                .toList();
    }

    private List<LearningView.DailyTrend> dailyTrends(
            List<AnswerRecord> answers, int days, LocalDate endDate) {
        Map<LocalDate, List<AnswerRecord>> grouped = answers.stream()
                .collect(Collectors.groupingBy(answer -> answer.getSubmittedAt().toLocalDate()));
        List<LearningView.DailyTrend> result = new ArrayList<>(days);
        for (int index = days - 1; index >= 0; index--) {
            LocalDate date = endDate.minusDays(index);
            List<AnswerRecord> daily = grouped.getOrDefault(date, List.of());
            long correct = daily.stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                    .count();
            result.add(new LearningView.DailyTrend(
                    date, daily.size(), correct, ratio(correct, daily.size())));
        }
        return result;
    }

    private List<LearningView.StudentSnapshot> studentSnapshots(
            List<Long> studentIds,
            Map<Long, UserSummaryResponse> users,
            List<PracticeTask> tasks,
            List<AnswerRecord> answers,
            List<ErrorRecord> openErrors,
            List<MasteryRecord> mastery) {
        Map<Long, List<PracticeTask>> tasksByStudent = group(tasks, PracticeTask::getStudentId);
        Map<Long, List<AnswerRecord>> answersByStudent = group(answers, AnswerRecord::getStudentId);
        Map<Long, List<ErrorRecord>> errorsByStudent = group(openErrors, ErrorRecord::getStudentId);
        Map<Long, List<MasteryRecord>> masteryByStudent = group(mastery, MasteryRecord::getStudentId);
        return studentIds.stream().map(studentId -> {
            UserSummaryResponse user = users.get(studentId);
            List<PracticeTask> studentTasks = tasksByStudent.getOrDefault(studentId, List.of());
            List<AnswerRecord> studentAnswers = answersByStudent.getOrDefault(studentId, List.of());
            long completed = studentTasks.stream()
                    .filter(task -> "COMPLETED".equals(task.getStatus()))
                    .count();
            long correct = studentAnswers.stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                    .count();
            long weakCount = masteryByStudent.getOrDefault(studentId, List.of()).stream()
                    .filter(value -> value.getMasteryScore().compareTo(WEAK_THRESHOLD) < 0)
                    .count();
            OffsetDateTime lastActivity = studentAnswers.stream()
                    .map(AnswerRecord::getSubmittedAt)
                    .max(Comparator.naturalOrder())
                    .orElse(null);
            return new LearningView.StudentSnapshot(
                    studentId, user == null ? null : user.username(), displayName(user, studentId),
                    user == null ? null : user.avatarUrl(), studentTasks.size(), completed,
                    studentAnswers.size(), correct, ratio(correct, studentAnswers.size()),
                    errorsByStudent.getOrDefault(studentId, List.of()).size(), weakCount,
                    lastActivity);
        }).sorted(Comparator.comparing(LearningView.StudentSnapshot::displayName)).toList();
    }

    private List<LearningView.StudentAlert> studentAlerts(
            List<LearningView.StudentSnapshot> students, OffsetDateTime rangeEnd) {
        List<LearningView.StudentAlert> alerts = new ArrayList<>();
        for (LearningView.StudentSnapshot student : students) {
            if (student.answerCount() >= 5
                    && student.accuracyRate().compareTo(WEAK_THRESHOLD) < 0) {
                alerts.add(new LearningView.StudentAlert(
                        student.studentId(), student.displayName(), "LOW_ACCURACY",
                        "近期正确率低于 60%", "HIGH"));
            }
            if (student.openErrorCount() >= 5) {
                alerts.add(new LearningView.StudentAlert(
                        student.studentId(), student.displayName(), "ERROR_BACKLOG",
                        "有 " + student.openErrorCount() + " 条错题尚未巩固", "HIGH"));
            }
            if (student.lastActivityAt() == null
                    || student.lastActivityAt().isBefore(rangeEnd.minusDays(7))) {
                alerts.add(new LearningView.StudentAlert(
                        student.studentId(), student.displayName(), "INACTIVE",
                        "最近 7 天没有学习记录", "MEDIUM"));
            }
            if (student.weakKnowledgePointCount() >= 3) {
                alerts.add(new LearningView.StudentAlert(
                        student.studentId(), student.displayName(), "MULTIPLE_WEAK_POINTS",
                        "有 " + student.weakKnowledgePointCount() + " 个薄弱知识点", "MEDIUM"));
            }
        }
        return alerts.stream()
                .sorted(Comparator.comparing((LearningView.StudentAlert alert) ->
                                "HIGH".equals(alert.severity()) ? 0 : 1)
                        .thenComparing(LearningView.StudentAlert::displayName))
                .toList();
    }

    private Map<Long, UserSummaryResponse> usersById(List<Long> studentIds) {
        Map<Long, UserSummaryResponse> users = new HashMap<>();
        for (int start = 0; start < studentIds.size(); start += 100) {
            List<Long> part = studentIds.subList(start, Math.min(start + 100, studentIds.size()));
            List<UserSummaryResponse> result = requireRemoteData(
                    () -> userFeignClient.getUserSummaries(new BatchUserQueryRequest(part)),
                    "暂时无法读取班级学生资料");
            result.forEach(user -> users.put(user.id(), user));
        }
        return users;
    }

    private Map<Long, QuestionResponse> questionsById(List<Long> questionIds) {
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        try {
            Result<List<QuestionResponse>> result = contentFeignClient.getQuestions(
                    new BatchQuestionQueryRequest(questionIds));
            if (result == null || !Integer.valueOf(200).equals(result.getCode())
                    || result.getData() == null) {
                return Map.of();
            }
            return result.getData().stream()
                    .collect(Collectors.toMap(QuestionResponse::id, Function.identity()));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private Map<Long, ErrorTypeResponse> errorTypesById() {
        try {
            Result<List<ErrorTypeResponse>> result = contentFeignClient.listErrorTypes();
            if (result == null || !Integer.valueOf(200).equals(result.getCode())
                    || result.getData() == null) {
                return Map.of();
            }
            return result.getData().stream()
                    .collect(Collectors.toMap(ErrorTypeResponse::id, Function.identity()));
        } catch (RuntimeException exception) {
            return Map.of();
        }
    }

    private String knowledgePointName(Long knowledgePointId) {
        try {
            Result<KnowledgePointResponse> result = contentFeignClient.getKnowledgePoint(knowledgePointId);
            if (result != null && Integer.valueOf(200).equals(result.getCode())
                    && result.getData() != null) {
                return result.getData().name();
            }
        } catch (RuntimeException ignored) {
            // 聚合看板保留业务 ID，内容服务短暂不可用时不阻断主体数据。
        }
        return "知识点 #" + knowledgePointId;
    }

    private int distinctStudents(List<ErrorRecord> errors) {
        return (int) errors.stream().map(ErrorRecord::getStudentId).distinct().count();
    }

    private <T> Map<Long, List<T>> group(List<T> values, Function<T, Long> key) {
        return values.stream().collect(Collectors.groupingBy(key));
    }

    private BigDecimal average(List<BigDecimal> values) {
        if (values.isEmpty()) {
            return zeroRate();
        }
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return denominator == 0
                ? zeroRate()
                : BigDecimal.valueOf(numerator)
                        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal zeroRate() {
        return BigDecimal.ZERO.setScale(4);
    }

    private String displayName(UserSummaryResponse user, Long userId) {
        if (user == null) {
            return "学生 #" + userId;
        }
        if (user.realName() != null && !user.realName().isBlank()) {
            return user.realName();
        }
        if (user.nickname() != null && !user.nickname().isBlank()) {
            return user.nickname();
        }
        return user.username();
    }

    private <T> T requireRemoteData(
            java.util.function.Supplier<Result<T>> request, String message) {
        Result<T> result;
        try {
            result = request.get();
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        return result.getData();
    }
}
