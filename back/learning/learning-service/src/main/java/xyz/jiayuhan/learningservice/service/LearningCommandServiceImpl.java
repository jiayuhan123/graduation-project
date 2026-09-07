package xyz.jiayuhan.learningservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.learningservice.entity.AnswerRecord;
import xyz.jiayuhan.learningservice.entity.ErrorRecord;
import xyz.jiayuhan.learningservice.entity.MasteryChangeLog;
import xyz.jiayuhan.learningservice.entity.MasteryRecord;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.entity.PracticeTaskKnowledgePoint;
import xyz.jiayuhan.learningservice.entity.PracticeTaskQuestion;
import xyz.jiayuhan.learningservice.entity.Recommendation;
import xyz.jiayuhan.learningservice.event.AssignmentProgressOutboxService;
import xyz.jiayuhan.learningservice.mapper.AnswerRecordMapper;
import xyz.jiayuhan.learningservice.mapper.ErrorRecordMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryChangeLogMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskKnowledgePointMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskQuestionMapper;
import xyz.jiayuhan.learningservice.mapper.RecommendationMapper;
import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;
import xyz.jiayuhan.learningservice.model.request.SubmitAnswerRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.support.AnswerGrader;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.response.AssignmentLearningResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

/** 固定题库上的自适应组题、可信判题及学习信号更新。 */
@Service
@RequiredArgsConstructor
public class LearningCommandServiceImpl implements LearningCommandService {

    private static final String SELF = "SELF";
    private static final String ASSIGNMENT = "ASSIGNMENT";
    private static final String RECOMMENDATION = "RECOMMENDATION";
    private static final String PENDING = "PENDING";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final long DEFAULT_ERROR_TYPE_ID = 94000001L;
    private static final BigDecimal MASTERY_BASELINE = new BigDecimal("0.5000");
    private static final BigDecimal MASTERY_HISTORY_WEIGHT = new BigDecimal("0.7000");
    private static final BigDecimal MASTERY_RESULT_WEIGHT = new BigDecimal("0.3000");

    private final PracticeTaskMapper practiceTaskMapper;
    private final PracticeTaskKnowledgePointMapper taskKnowledgePointMapper;
    private final PracticeTaskQuestionMapper taskQuestionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final ErrorRecordMapper errorRecordMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final MasteryChangeLogMapper masteryChangeLogMapper;
    private final RecommendationMapper recommendationMapper;
    private final ContentFeignClient contentFeignClient;
    private final TeachingFeignClient teachingFeignClient;
    private final UserFeignClient userFeignClient;
    private final LearningQueryService learningQueryService;
    private final AssignmentProgressOutboxService assignmentProgressOutboxService;
    private final ObjectMapper objectMapper;
    private final AnswerGrader answerGrader;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PracticeDetail createSelfPractice(
            Long studentId, CreatePracticeTaskRequest request) {
        requireActiveStudent(studentId);
        KnowledgePointResponse knowledgePoint = requireRemoteData(
                () -> contentFeignClient.getKnowledgePoint(request.knowledgePointId()),
                "暂时无法读取知识点");
        if (!Integer.valueOf(1).equals(knowledgePoint.status())) {
            throw new BusinessException(ResultCode.KNOWLEDGE_POINT_NOT_FOUND);
        }
        int requestedCount = request.questionCount() == null ? 10 : request.questionCount();
        List<QuestionResponse> selected = selectQuestions(
                studentId, request.knowledgePointId(), request.difficulty(), requestedCount);
        List<PreparedQuestion> questions = selected.stream()
                .map(question -> new PreparedQuestion(question, BigDecimal.ONE))
                .toList();
        String mode = request.mode() == null ? "NORMAL" : request.mode();
        Integer timeLimit = "TIMED".equals(mode)
                ? (request.timeLimitSeconds() == null ? 600 : request.timeLimitSeconds())
                : null;
        PracticeTask task = createTask(
                studentId, SELF, null, knowledgePoint.name() + "专项练习", mode,
                timeLimit, request.targetAccuracy(), null,
                List.of(request.knowledgePointId()), questions);
        return learningQueryService.getPracticeTask(studentId, task.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PracticeDetail createAssignmentPractice(
            Long studentId, Long assignmentId) {
        PracticeTask existing = findSourceTask(studentId, ASSIGNMENT, String.valueOf(assignmentId));
        if (existing != null) {
            return learningQueryService.getPracticeTask(studentId, existing.getId());
        }

        AssignmentLearningResponse assignment = requireRemoteData(
                () -> teachingFeignClient.getStudentAssignment(assignmentId, studentId),
                "暂时无法读取教师作业");
        OffsetDateTime now = now();
        if (!"PUBLISHED".equals(assignment.assignmentStatus())) {
            throw new BusinessException(ResultCode.ASSIGNMENT_STATE_INVALID, "作业尚未发布或已经结束");
        }
        if (assignment.startAt() != null && now.isBefore(assignment.startAt())) {
            throw new BusinessException(ResultCode.ASSIGNMENT_STATE_INVALID, "作业还未到开始时间");
        }
        if (assignment.dueAt() != null && now.isAfter(assignment.dueAt())) {
            throw new BusinessException(ResultCode.ASSIGNMENT_STATE_INVALID, "作业已经截止");
        }
        if (assignment.questions() == null || assignment.questions().isEmpty()) {
            throw new BusinessException(ResultCode.ASSIGNMENT_CONTENT_INVALID, "作业没有可答题目");
        }

        Set<Long> knowledgePointIds = new LinkedHashSet<>();
        List<PreparedQuestion> questions = assignment.questions().stream()
                .map(question -> {
                    QuestionResponse snapshot = enrichKnowledgePoints(
                            readSnapshot(question.questionSnapshot()));
                    if (snapshot.knowledgePointIds() != null) {
                        knowledgePointIds.addAll(snapshot.knowledgePointIds());
                    }
                    return new PreparedQuestion(snapshot, question.score());
                })
                .toList();
        PracticeTask task = createTask(
                studentId, ASSIGNMENT, String.valueOf(assignmentId), assignment.title(),
                "NORMAL", null, null, assignment.dueAt(),
                List.copyOf(knowledgePointIds), questions);
        return learningQueryService.getPracticeTask(studentId, task.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PracticeDetail startPractice(Long studentId, Long taskId) {
        PracticeTask task = requireTaskForUpdate(taskId, studentId);
        startTaskIfNecessary(task);
        return learningQueryService.getPracticeTask(studentId, taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.AnswerFeedback submitAnswer(
            Long studentId,
            Long taskId,
            Long taskQuestionId,
            SubmitAnswerRequest request) {
        PracticeTask task = requireTaskForUpdate(taskId, studentId);
        startTaskIfNecessary(task);
        requireAnswerable(task);
        PracticeTaskQuestion taskQuestion = taskQuestionMapper.selectOne(
                new LambdaQueryWrapper<PracticeTaskQuestion>()
                        .eq(PracticeTaskQuestion::getId, taskQuestionId)
                        .eq(PracticeTaskQuestion::getTaskId, taskId));
        if (taskQuestion == null) {
            throw new BusinessException(ResultCode.PRACTICE_QUESTION_NOT_FOUND);
        }
        QuestionResponse snapshot = readSnapshot(taskQuestion.getQuestionSnapshot());
        if (ASSIGNMENT.equals(task.getSourceType())) {
            AnswerRecord answer = saveAssignmentAnswer(
                    task, taskQuestion, request.answer(), request.responseMs());
            return new LearningView.AnswerFeedback(
                    taskId, taskQuestionId, toView(answer), false, null, null);
        }

        boolean correct = answerGrader.isCorrect(snapshot.standardAnswer(), request.answer());
        AnswerRecord answer = newAnswer(
                task, taskQuestion, nextAttempt(taskQuestionId, studentId), request,
                "GRADED", correct, correct ? taskQuestion.getMaxScore() : BigDecimal.ZERO);
        answer.setGradedAt(now());
        answerRecordMapper.insert(answer);
        updateLearningSignals(task, taskQuestion, snapshot, answer);
        return new LearningView.AnswerFeedback(
                taskId, taskQuestionId, toView(answer), true,
                snapshot.standardAnswer(), snapshot.analysis());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PracticeDetail completePractice(Long studentId, Long taskId) {
        PracticeTask task = requireTaskForUpdate(taskId, studentId);
        startTaskIfNecessary(task);
        if (COMPLETED.equals(task.getStatus())) {
            return learningQueryService.getPracticeTask(studentId, taskId);
        }
        requireAnswerable(task);
        List<PracticeTaskQuestion> questions = taskQuestionMapper.selectList(
                new LambdaQueryWrapper<PracticeTaskQuestion>()
                        .eq(PracticeTaskQuestion::getTaskId, taskId)
                        .orderByAsc(PracticeTaskQuestion::getSequenceNo));
        List<AnswerRecord> latestAnswers = new ArrayList<>();
        for (PracticeTaskQuestion question : questions) {
            AnswerRecord answer = latestAnswer(question.getId(), studentId);
            if (answer == null) {
                throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "请先完成全部题目");
            }
            if (ASSIGNMENT.equals(task.getSourceType()) && PENDING.equals(answer.getGradingStatus())) {
                QuestionResponse snapshot = readSnapshot(question.getQuestionSnapshot());
                boolean correct = answerGrader.isCorrect(
                        snapshot.standardAnswer(), extractStudentAnswer(answer));
                answer.setGradingStatus("GRADED");
                answer.setCorrect(correct);
                answer.setScore(correct ? question.getMaxScore() : BigDecimal.ZERO);
                answer.setGradedAt(now());
                answerRecordMapper.updateById(answer);
                updateLearningSignals(task, question, snapshot, answer);
            }
            latestAnswers.add(answer);
        }

        OffsetDateTime completedAt = now();
        task.setStatus(COMPLETED);
        task.setCompletedAt(completedAt);
        assertUpdated(practiceTaskMapper.updateById(task));
        completeRecommendation(task, completedAt);
        if (ASSIGNMENT.equals(task.getSourceType())) {
            int correctCount = (int) latestAnswers.stream()
                    .filter(answer -> Boolean.TRUE.equals(answer.getCorrect()))
                    .count();
            BigDecimal score = latestAnswers.stream()
                    .map(AnswerRecord::getScore)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            assignmentProgressOutboxService.append(
                    task, "GRADED", score, correctCount, questions.size(), completedAt);
        }
        return learningQueryService.getPracticeTask(studentId, taskId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearningView.PracticeDetail acceptRecommendation(
            Long studentId, Long recommendationId) {
        Recommendation recommendation = requireRecommendation(studentId, recommendationId);
        if (("ACCEPTED".equals(recommendation.getStatus())
                || COMPLETED.equals(recommendation.getStatus()))
                && recommendation.getGeneratedTaskId() != null) {
            return learningQueryService.getPracticeTask(
                    studentId, recommendation.getGeneratedTaskId());
        }
        if (!PENDING.equals(recommendation.getStatus())) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "推荐已经处理");
        }
        KnowledgePointResponse point = requireRemoteData(
                () -> contentFeignClient.getKnowledgePoint(recommendation.getKnowledgePointId()),
                "暂时无法读取推荐知识点");
        List<QuestionResponse> selected = selectQuestions(
                studentId, point.id(), null, recommendation.getRecommendedQuestionCount());
        PracticeTask task = createTask(
                studentId, RECOMMENDATION, String.valueOf(recommendationId),
                point.name() + "巩固练习", "REVIEW", null,
                new BigDecimal("0.8000"), recommendation.getExpiresAt(), List.of(point.id()),
                selected.stream().map(question -> new PreparedQuestion(question, BigDecimal.ONE)).toList());
        OffsetDateTime acceptedAt = now();
        assertUpdated(recommendationMapper.update(null, new LambdaUpdateWrapper<Recommendation>()
                .eq(Recommendation::getId, recommendationId)
                .eq(Recommendation::getStudentId, studentId)
                .eq(Recommendation::getStatus, PENDING)
                .set(Recommendation::getStatus, "ACCEPTED")
                .set(Recommendation::getGeneratedTaskId, task.getId())
                .set(Recommendation::getAcceptedAt, acceptedAt)
                .set(Recommendation::getUpdatedAt, acceptedAt)));
        return learningQueryService.getPracticeTask(studentId, task.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissRecommendation(Long studentId, Long recommendationId) {
        Recommendation recommendation = requireRecommendation(studentId, recommendationId);
        if ("DISMISSED".equals(recommendation.getStatus())) {
            return;
        }
        if (!PENDING.equals(recommendation.getStatus())) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "推荐已经处理");
        }
        OffsetDateTime dismissedAt = now();
        assertUpdated(recommendationMapper.update(null, new LambdaUpdateWrapper<Recommendation>()
                .eq(Recommendation::getId, recommendationId)
                .eq(Recommendation::getStudentId, studentId)
                .eq(Recommendation::getStatus, PENDING)
                .set(Recommendation::getStatus, "DISMISSED")
                .set(Recommendation::getUpdatedAt, dismissedAt)));
    }

    private List<QuestionResponse> selectQuestions(
            Long studentId, Long knowledgePointId, Integer difficulty, int requestedCount) {
        Map<Long, QuestionResponse> candidates = new LinkedHashMap<>();
        if (difficulty != null) {
            PageResult<QuestionResponse> exact = requireRemoteData(
                    () -> contentFeignClient.pageQuestions(
                            1, 100, null, knowledgePointId, null, difficulty),
                    "暂时无法读取题库");
            exact.records().stream()
                    .filter(this::isFixedQuestion)
                    .forEach(question -> candidates.put(question.id(), question));
        }
        if (candidates.size() < requestedCount) {
            PageResult<QuestionResponse> fallback = requireRemoteData(
                    () -> contentFeignClient.pageQuestions(
                            1, 100, null, knowledgePointId, null, null),
                    "暂时无法读取题库");
            fallback.records().stream()
                    .filter(this::isFixedQuestion)
                    .forEach(question -> candidates.putIfAbsent(question.id(), question));
        }
        if (candidates.isEmpty()) {
            throw new BusinessException(ResultCode.PRACTICE_QUESTION_POOL_EMPTY);
        }
        Set<Long> wrongQuestionIds = errorRecordMapper.selectList(
                        new LambdaQueryWrapper<ErrorRecord>()
                                .eq(ErrorRecord::getStudentId, studentId)
                                .eq(ErrorRecord::getKnowledgePointId, knowledgePointId)
                                .ne(ErrorRecord::getStatus, "RESOLVED")
                                .isNotNull(ErrorRecord::getQuestionId))
                .stream()
                .map(ErrorRecord::getQuestionId)
                .collect(java.util.stream.Collectors.toSet());
        Set<Long> seenQuestionIds = recentQuestionIds(studentId);
        int dailyOffset = Math.floorMod(
                java.util.Objects.hash(studentId, knowledgePointId, LocalDate.now(ZoneOffset.UTC)),
                Math.max(1, candidates.size()));
        List<QuestionResponse> ordered = new ArrayList<>(candidates.values());
        ordered.sort(Comparator
                .comparingInt((QuestionResponse question) -> questionPriority(
                        question.id(), wrongQuestionIds, seenQuestionIds))
                .thenComparingInt(question -> Math.floorMod(
                        Long.hashCode(question.id()) - dailyOffset, Math.max(1, candidates.size())))
                .thenComparing(QuestionResponse::id));
        return ordered.stream().limit(Math.min(requestedCount, ordered.size())).toList();
    }

    private Set<Long> recentQuestionIds(Long studentId) {
        List<AnswerRecord> answers = answerRecordMapper.selectList(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getStudentId, studentId)
                        .orderByDesc(AnswerRecord::getSubmittedAt)
                        .last("LIMIT 200"));
        if (answers.isEmpty()) {
            return Set.of();
        }
        return taskQuestionMapper.selectByIds(answers.stream()
                        .map(AnswerRecord::getTaskQuestionId)
                        .distinct()
                        .toList())
                .stream()
                .map(PracticeTaskQuestion::getQuestionId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
    }

    private int questionPriority(Long questionId, Set<Long> wrongIds, Set<Long> seenIds) {
        if (wrongIds.contains(questionId)) {
            return 0;
        }
        return seenIds.contains(questionId) ? 2 : 1;
    }

    private PracticeTask createTask(
            Long studentId,
            String sourceType,
            String sourceId,
            String title,
            String mode,
            Integer timeLimitSeconds,
            BigDecimal targetAccuracy,
            OffsetDateTime expiresAt,
            List<Long> knowledgePointIds,
            List<PreparedQuestion> questions) {
        PracticeTask task = new PracticeTask();
        task.setStudentId(studentId);
        task.setSourceType(sourceType);
        task.setSourceId(sourceId);
        task.setTitle(title);
        task.setMode(mode);
        task.setStatus(PENDING);
        task.setQuestionCount(questions.size());
        task.setTimeLimitSeconds(timeLimitSeconds);
        task.setTargetAccuracy(targetAccuracy);
        task.setExpiresAt(expiresAt);
        task.setVersion(0);
        if (ASSIGNMENT.equals(sourceType)) {
            OffsetDateTime createdAt = now();
            task.setId(IdWorker.getId());
            task.setCreatedAt(createdAt);
            task.setUpdatedAt(createdAt);
            if (practiceTaskMapper.insertAssignmentIfAbsent(task) == 0) {
                PracticeTask existing = findSourceTask(studentId, sourceType, sourceId);
                if (existing == null) {
                    throw new BusinessException(ResultCode.CONFLICT, "作业练习正在创建，请稍后重试");
                }
                return existing;
            }
        } else {
            practiceTaskMapper.insert(task);
        }

        for (Long knowledgePointId : knowledgePointIds.stream().distinct().toList()) {
            PracticeTaskKnowledgePoint relation = new PracticeTaskKnowledgePoint();
            relation.setTaskId(task.getId());
            relation.setKnowledgePointId(knowledgePointId);
            relation.setTargetQuestionCount(questions.size());
            taskKnowledgePointMapper.insert(relation);
        }
        int sequence = 1;
        for (PreparedQuestion prepared : questions) {
            PracticeTaskQuestion question = new PracticeTaskQuestion();
            question.setTaskId(task.getId());
            question.setQuestionId(prepared.question().id());
            question.setSequenceNo(sequence++);
            question.setQuestionSnapshot(objectMapper.writeValueAsString(prepared.question()));
            question.setMaxScore(prepared.maxScore());
            question.setSourceDetail(objectMapper.writeValueAsString(Map.of(
                    "sourceType", sourceType,
                    "adaptive", !ASSIGNMENT.equals(sourceType))));
            taskQuestionMapper.insert(question);
        }
        return task;
    }

    private void startTaskIfNecessary(PracticeTask task) {
        if (PENDING.equals(task.getStatus())) {
            OffsetDateTime startedAt = now();
            if (task.getExpiresAt() != null && startedAt.isAfter(task.getExpiresAt())) {
                task.setStatus("EXPIRED");
                assertUpdated(practiceTaskMapper.updateById(task));
                throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "练习任务已经过期");
            }
            task.setStatus(IN_PROGRESS);
            task.setStartedAt(startedAt);
            assertUpdated(practiceTaskMapper.updateById(task));
            if (ASSIGNMENT.equals(task.getSourceType())) {
                assignmentProgressOutboxService.append(
                        task, IN_PROGRESS, null, null, task.getQuestionCount(), null);
            }
            return;
        }
        if (!IN_PROGRESS.equals(task.getStatus()) && !COMPLETED.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID);
        }
    }

    private void requireAnswerable(PracticeTask task) {
        if (!IN_PROGRESS.equals(task.getStatus())) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID);
        }
        OffsetDateTime now = now();
        if (task.getExpiresAt() != null && now.isAfter(task.getExpiresAt())) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "练习任务已经过期");
        }
        if ("TIMED".equals(task.getMode()) && task.getTimeLimitSeconds() != null
                && task.getStartedAt() != null
                && now.isAfter(task.getStartedAt().plusSeconds(task.getTimeLimitSeconds()))) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "限时练习已经结束");
        }
    }

    private AnswerRecord saveAssignmentAnswer(
            PracticeTask task,
            PracticeTaskQuestion question,
            String answerValue,
            Integer responseMs) {
        AnswerRecord answer = answerRecordMapper.selectOne(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getTaskQuestionId, question.getId())
                .eq(AnswerRecord::getStudentId, task.getStudentId())
                .eq(AnswerRecord::getAttemptNo, 1));
        if (answer == null) {
            answer = newAnswer(
                    task, question, 1, new SubmitAnswerRequest(answerValue, responseMs),
                    PENDING, null, null);
            answerRecordMapper.insert(answer);
            return answer;
        }
        if (!PENDING.equals(answer.getGradingStatus())) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_STATE_INVALID, "作业已经提交");
        }
        answer.setStudentAnswer(answerJson(answerValue));
        answer.setResponseMs(responseMs);
        answer.setSubmittedAt(now());
        assertUpdated(answerRecordMapper.updateById(answer));
        return answer;
    }

    private AnswerRecord newAnswer(
            PracticeTask task,
            PracticeTaskQuestion question,
            int attemptNo,
            SubmitAnswerRequest request,
            String gradingStatus,
            Boolean correct,
            BigDecimal score) {
        AnswerRecord answer = new AnswerRecord();
        answer.setTaskQuestionId(question.getId());
        answer.setStudentId(task.getStudentId());
        answer.setAttemptNo(attemptNo);
        answer.setStudentAnswer(answerJson(request.answer()));
        answer.setGradingStatus(gradingStatus);
        answer.setCorrect(correct);
        answer.setScore(score);
        answer.setResponseMs(request.responseMs());
        answer.setAnswerSource("MANUAL");
        answer.setSubmittedAt(now());
        return answer;
    }

    private void updateLearningSignals(
            PracticeTask task,
            PracticeTaskQuestion taskQuestion,
            QuestionResponse snapshot,
            AnswerRecord answer) {
        List<Long> knowledgePointIds = snapshot.knowledgePointIds();
        if (knowledgePointIds == null || knowledgePointIds.isEmpty()) {
            knowledgePointIds = taskKnowledgePointMapper.selectList(
                            new LambdaQueryWrapper<PracticeTaskKnowledgePoint>()
                                    .eq(PracticeTaskKnowledgePoint::getTaskId, task.getId()))
                    .stream()
                    .map(PracticeTaskKnowledgePoint::getKnowledgePointId)
                    .toList();
        }
        for (Long knowledgePointId : knowledgePointIds) {
            MasteryRecord mastery = updateMastery(task.getStudentId(), knowledgePointId, answer);
            if (Boolean.TRUE.equals(answer.getCorrect())) {
                resolveErrors(task.getStudentId(), taskQuestion.getQuestionId(), knowledgePointId, answer.getId());
            } else {
                createError(task, taskQuestion, knowledgePointId, answer);
                createRecommendation(task.getStudentId(), knowledgePointId, answer.getId(), mastery);
            }
        }
    }

    private MasteryRecord updateMastery(Long studentId, Long knowledgePointId, AnswerRecord answer) {
        masteryRecordMapper.acquireUpdateLock(studentId, knowledgePointId);
        MasteryRecord mastery = masteryRecordMapper.selectOne(new LambdaQueryWrapper<MasteryRecord>()
                .eq(MasteryRecord::getStudentId, studentId)
                .eq(MasteryRecord::getKnowledgePointId, knowledgePointId));
        BigDecimal before = mastery == null ? MASTERY_BASELINE : mastery.getMasteryScore();
        int previousTotal = mastery == null ? 0 : mastery.getTotalAnswerCount();
        int previousCorrect = mastery == null ? 0 : mastery.getCorrectAnswerCount();
        int total = previousTotal + 1;
        int correct = previousCorrect + (Boolean.TRUE.equals(answer.getCorrect()) ? 1 : 0);
        BigDecimal observation = Boolean.TRUE.equals(answer.getCorrect()) ? BigDecimal.ONE : BigDecimal.ZERO;
        BigDecimal after = before.multiply(MASTERY_HISTORY_WEIGHT)
                .add(observation.multiply(MASTERY_RESULT_WEIGHT))
                .setScale(4, RoundingMode.HALF_UP);
        OffsetDateTime practicedAt = answer.getGradedAt();

        if (mastery == null) {
            mastery = new MasteryRecord();
            mastery.setStudentId(studentId);
            mastery.setKnowledgePointId(knowledgePointId);
            mastery.setVersion(0);
            mastery.setConsecutiveCorrectCount(0);
        }
        mastery.setMasteryScore(after);
        mastery.setAccuracyRate(BigDecimal.valueOf(correct)
                .divide(BigDecimal.valueOf(total), 4, RoundingMode.HALF_UP));
        mastery.setAvgResponseMs(nextAverageResponse(
                mastery.getAvgResponseMs(), previousTotal, answer.getResponseMs(), total));
        mastery.setTotalAnswerCount(total);
        mastery.setCorrectAnswerCount(correct);
        mastery.setConsecutiveCorrectCount(Boolean.TRUE.equals(answer.getCorrect())
                ? mastery.getConsecutiveCorrectCount() + 1 : 0);
        mastery.setLastPracticedAt(practicedAt);
        mastery.setNextReviewAt(nextReviewAt(practicedAt, after));
        if (mastery.getId() == null) {
            masteryRecordMapper.insert(mastery);
        } else {
            assertUpdated(masteryRecordMapper.updateById(mastery));
        }

        MasteryChangeLog change = new MasteryChangeLog();
        change.setMasteryId(mastery.getId());
        change.setAnswerRecordId(answer.getId());
        change.setBeforeScore(before);
        change.setAfterScore(after);
        change.setDeltaScore(after.subtract(before));
        change.setReason("ANSWER");
        change.setOccurredAt(practicedAt);
        masteryChangeLogMapper.insert(change);
        return mastery;
    }

    private Integer nextAverageResponse(
            Integer previousAverage, int previousTotal, Integer current, int total) {
        if (current == null) {
            return previousAverage;
        }
        if (previousAverage == null || previousTotal == 0) {
            return current;
        }
        long sum = (long) previousAverage * previousTotal + current;
        return Math.toIntExact(Math.round((double) sum / total));
    }

    private OffsetDateTime nextReviewAt(OffsetDateTime base, BigDecimal masteryScore) {
        if (masteryScore.compareTo(new BigDecimal("0.8000")) >= 0) {
            return base.plusDays(7);
        }
        if (masteryScore.compareTo(new BigDecimal("0.6000")) >= 0) {
            return base.plusDays(3);
        }
        return base.plusDays(1);
    }

    private void createError(
            PracticeTask task,
            PracticeTaskQuestion question,
            Long knowledgePointId,
            AnswerRecord answer) {
        ErrorRecord error = new ErrorRecord();
        error.setAnswerRecordId(answer.getId());
        error.setStudentId(task.getStudentId());
        error.setQuestionId(question.getQuestionId());
        error.setKnowledgePointId(knowledgePointId);
        error.setErrorTypeId(DEFAULT_ERROR_TYPE_ID);
        error.setDiagnosis("本次答案与标准答案不一致，建议检查计算过程后再次练习。");
        error.setConfidence(BigDecimal.ONE);
        error.setStatus("OPEN");
        errorRecordMapper.insert(error);
    }

    private void resolveErrors(
            Long studentId, Long questionId, Long knowledgePointId, Long answerId) {
        if (questionId == null) {
            return;
        }
        OffsetDateTime resolvedAt = now();
        errorRecordMapper.update(null, new LambdaUpdateWrapper<ErrorRecord>()
                .eq(ErrorRecord::getStudentId, studentId)
                .eq(ErrorRecord::getQuestionId, questionId)
                .eq(ErrorRecord::getKnowledgePointId, knowledgePointId)
                .ne(ErrorRecord::getStatus, "RESOLVED")
                .set(ErrorRecord::getStatus, "RESOLVED")
                .set(ErrorRecord::getResolvedByAnswerId, answerId)
                .set(ErrorRecord::getResolvedAt, resolvedAt)
                .set(ErrorRecord::getUpdatedAt, resolvedAt));
    }

    private void createRecommendation(
            Long studentId, Long knowledgePointId, Long answerId, MasteryRecord mastery) {
        Recommendation existing = recommendationMapper.selectOne(
                new LambdaQueryWrapper<Recommendation>()
                        .eq(Recommendation::getStudentId, studentId)
                        .eq(Recommendation::getKnowledgePointId, knowledgePointId)
                        .eq(Recommendation::getSourceType, "ERROR")
                        .eq(Recommendation::getStatus, PENDING));
        if (existing != null) {
            return;
        }
        Recommendation recommendation = new Recommendation();
        recommendation.setStudentId(studentId);
        recommendation.setKnowledgePointId(knowledgePointId);
        recommendation.setSourceType("ERROR");
        recommendation.setSourceId(answerId);
        recommendation.setReasonText("该知识点刚刚出现错题，建议进行一次针对性巩固。");
        recommendation.setPriority(mastery.getMasteryScore().compareTo(new BigDecimal("0.4000")) < 0 ? 5 : 4);
        recommendation.setRecommendedQuestionCount(10);
        recommendation.setStatus(PENDING);
        recommendation.setGeneratedAt(now());
        recommendation.setExpiresAt(now().plusDays(7));
        recommendationMapper.insert(recommendation);
    }

    private void completeRecommendation(PracticeTask task, OffsetDateTime completedAt) {
        if (!RECOMMENDATION.equals(task.getSourceType()) || task.getSourceId() == null) {
            return;
        }
        recommendationMapper.update(null, new LambdaUpdateWrapper<Recommendation>()
                .eq(Recommendation::getId, Long.valueOf(task.getSourceId()))
                .eq(Recommendation::getStudentId, task.getStudentId())
                .eq(Recommendation::getGeneratedTaskId, task.getId())
                .set(Recommendation::getStatus, COMPLETED)
                .set(Recommendation::getCompletedAt, completedAt)
                .set(Recommendation::getUpdatedAt, completedAt));
    }

    private PracticeTask requireTask(Long taskId, Long studentId) {
        PracticeTask task = practiceTaskMapper.selectOne(new LambdaQueryWrapper<PracticeTask>()
                .eq(PracticeTask::getId, taskId)
                .eq(PracticeTask::getStudentId, studentId));
        if (task == null) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_NOT_FOUND);
        }
        return task;
    }

    private PracticeTask requireTaskForUpdate(Long taskId, Long studentId) {
        PracticeTask task = practiceTaskMapper.selectForUpdate(taskId, studentId);
        if (task == null) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_NOT_FOUND);
        }
        return task;
    }

    private PracticeTask findSourceTask(Long studentId, String sourceType, String sourceId) {
        return practiceTaskMapper.selectOne(new LambdaQueryWrapper<PracticeTask>()
                .eq(PracticeTask::getStudentId, studentId)
                .eq(PracticeTask::getSourceType, sourceType)
                .eq(PracticeTask::getSourceId, sourceId)
                .orderByDesc(PracticeTask::getCreatedAt)
                .last("LIMIT 1"));
    }

    private Recommendation requireRecommendation(Long studentId, Long recommendationId) {
        Recommendation recommendation = recommendationMapper.selectOne(
                new LambdaQueryWrapper<Recommendation>()
                        .eq(Recommendation::getId, recommendationId)
                        .eq(Recommendation::getStudentId, studentId));
        if (recommendation == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "推荐记录不存在");
        }
        return recommendation;
    }

    private AnswerRecord latestAnswer(Long taskQuestionId, Long studentId) {
        return answerRecordMapper.selectOne(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getTaskQuestionId, taskQuestionId)
                .eq(AnswerRecord::getStudentId, studentId)
                .orderByDesc(AnswerRecord::getAttemptNo)
                .last("LIMIT 1"));
    }

    private int nextAttempt(Long taskQuestionId, Long studentId) {
        return Math.toIntExact(answerRecordMapper.selectCount(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getTaskQuestionId, taskQuestionId)
                .eq(AnswerRecord::getStudentId, studentId)) + 1);
    }

    private QuestionResponse readSnapshot(String json) {
        try {
            return objectMapper.readValue(json, QuestionResponse.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "题目快照损坏");
        }
    }

    private QuestionResponse enrichKnowledgePoints(QuestionResponse snapshot) {
        if (snapshot.knowledgePointIds() != null && !snapshot.knowledgePointIds().isEmpty()) {
            return snapshot;
        }
        QuestionResponse latest = requireRemoteData(
                () -> contentFeignClient.getQuestion(snapshot.id()), "暂时无法补全题目知识点");
        return new QuestionResponse(
                snapshot.id(), snapshot.code(), snapshot.questionMode(), snapshot.questionType(),
                snapshot.stem(), snapshot.options(), snapshot.standardAnswer(), snapshot.analysis(),
                snapshot.generationRule(), snapshot.difficulty(), snapshot.estimatedSeconds(),
                snapshot.status(), latest.knowledgePointIds());
    }

    private String extractStudentAnswer(AnswerRecord answer) {
        return answerGrader.extractValue(answer.getStudentAnswer());
    }

    private String answerJson(String answer) {
        return objectMapper.writeValueAsString(Map.of("value", answer.trim()));
    }

    private LearningView.Answer toView(AnswerRecord answer) {
        return new LearningView.Answer(
                answer.getId(), answer.getTaskQuestionId(), answer.getAttemptNo(),
                answer.getStudentAnswer(), answer.getGradingStatus(), answer.getCorrect(),
                answer.getScore(), answer.getResponseMs(), answer.getAnswerSource(),
                answer.getRecognitionConfidence(), answer.getSubmittedAt(), answer.getGradedAt());
    }

    private void requireActiveStudent(Long studentId) {
        UserValidationResponse validation = requireRemoteData(
                () -> userFeignClient.validateUser(studentId, "student"), "暂时无法校验学生身份");
        if (!validation.exists() || !validation.active() || !validation.roleMatched()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private <T> T requireRemoteData(Supplier<Result<T>> request, String message) {
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

    private boolean isFixedQuestion(QuestionResponse question) {
        return question != null
                && "FIXED".equals(question.questionMode())
                && "ENABLED".equals(question.status())
                && question.standardAnswer() != null
                && !question.standardAnswer().isBlank();
    }

    private void assertUpdated(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "学习数据已发生变化，请刷新后重试");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    private record PreparedQuestion(QuestionResponse question, BigDecimal maxScore) {
    }

}
