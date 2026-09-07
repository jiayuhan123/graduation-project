package xyz.jiayuhan.learningservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;
import xyz.jiayuhan.learningservice.entity.AnswerRecord;
import xyz.jiayuhan.learningservice.entity.ErrorRecord;
import xyz.jiayuhan.learningservice.entity.MasteryChangeLog;
import xyz.jiayuhan.learningservice.entity.MasteryRecord;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingItem;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.entity.PracticeTaskKnowledgePoint;
import xyz.jiayuhan.learningservice.entity.PracticeTaskQuestion;
import xyz.jiayuhan.learningservice.entity.Recommendation;
import xyz.jiayuhan.learningservice.mapper.AnswerRecordMapper;
import xyz.jiayuhan.learningservice.mapper.ErrorRecordMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryChangeLogMapper;
import xyz.jiayuhan.learningservice.mapper.MasteryRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingItemMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskKnowledgePointMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskQuestionMapper;
import xyz.jiayuhan.learningservice.mapper.RecommendationMapper;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/** 所有面向学生的查询都强制带学生 ID 条件，避免横向越权。 */
@Service
@RequiredArgsConstructor
public class LearningQueryServiceImpl implements LearningQueryService {

    private final PracticeTaskMapper practiceTaskMapper;
    private final PracticeTaskKnowledgePointMapper taskKnowledgePointMapper;
    private final PracticeTaskQuestionMapper taskQuestionMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final ErrorRecordMapper errorRecordMapper;
    private final MasteryRecordMapper masteryRecordMapper;
    private final MasteryChangeLogMapper masteryChangeLogMapper;
    private final RecommendationMapper recommendationMapper;
    private final PhotoMarkingTaskMapper photoMarkingTaskMapper;
    private final PhotoMarkingItemMapper photoMarkingItemMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<PracticeTaskSummaryResponse> pagePracticeTasks(
            Long studentId, long page, long size, String status) {
        Page<PracticeTask> result = practiceTaskMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PracticeTask>()
                        .eq(PracticeTask::getStudentId, studentId)
                        .eq(status != null, PracticeTask::getStatus, status)
                        .orderByDesc(PracticeTask::getCreatedAt)
                        .orderByDesc(PracticeTask::getId));
        return pageResult(result, result.getRecords().stream().map(this::toSummary).toList());
    }

    @Override
    public LearningView.PracticeDetail getPracticeTask(Long studentId, Long taskId) {
        PracticeTask task = requireTask(taskId, studentId);
        List<Long> knowledgePointIds = taskKnowledgePointMapper.selectList(
                        new LambdaQueryWrapper<PracticeTaskKnowledgePoint>()
                                .eq(PracticeTaskKnowledgePoint::getTaskId, taskId))
                .stream()
                .map(PracticeTaskKnowledgePoint::getKnowledgePointId)
                .toList();
        List<LearningView.PracticeQuestion> questions = taskQuestionMapper.selectList(
                        new LambdaQueryWrapper<PracticeTaskQuestion>()
                                .eq(PracticeTaskQuestion::getTaskId, taskId)
                                .orderByAsc(PracticeTaskQuestion::getSequenceNo))
                .stream()
                .map(question -> toView(task, question))
                .toList();
        return new LearningView.PracticeDetail(toSummary(task), knowledgePointIds, questions);
    }

    @Override
    public PracticeTaskSummaryResponse getPracticeTaskInternal(Long taskId) {
        return toSummary(requireTask(taskId, null));
    }

    @Override
    public PageResult<LearningView.Answer> pageAnswers(
            Long studentId, long page, long size, String gradingStatus) {
        Page<AnswerRecord> result = answerRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getStudentId, studentId)
                        .eq(gradingStatus != null, AnswerRecord::getGradingStatus, gradingStatus)
                        .orderByDesc(AnswerRecord::getSubmittedAt)
                        .orderByDesc(AnswerRecord::getId));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public PageResult<LearningView.Error> pageErrors(
            Long studentId, long page, long size, String status, Long knowledgePointId) {
        Page<ErrorRecord> result = errorRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<ErrorRecord>()
                        .eq(ErrorRecord::getStudentId, studentId)
                        .eq(status != null, ErrorRecord::getStatus, status)
                        .eq(knowledgePointId != null, ErrorRecord::getKnowledgePointId, knowledgePointId)
                        .orderByDesc(ErrorRecord::getCreatedAt)
                        .orderByDesc(ErrorRecord::getId));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public List<LearningView.Mastery> listMastery(Long studentId, Long knowledgePointId) {
        return masteryRecordMapper.selectList(new LambdaQueryWrapper<MasteryRecord>()
                        .eq(MasteryRecord::getStudentId, studentId)
                        .eq(knowledgePointId != null, MasteryRecord::getKnowledgePointId, knowledgePointId)
                        .orderByAsc(MasteryRecord::getMasteryScore)
                        .orderByAsc(MasteryRecord::getKnowledgePointId))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public List<LearningView.MasteryChange> listMasteryChanges(
            Long studentId, Long knowledgePointId) {
        MasteryRecord mastery = masteryRecordMapper.selectOne(new LambdaQueryWrapper<MasteryRecord>()
                .eq(MasteryRecord::getStudentId, studentId)
                .eq(MasteryRecord::getKnowledgePointId, knowledgePointId));
        if (mastery == null) {
            return List.of();
        }
        return masteryChangeLogMapper.selectList(new LambdaQueryWrapper<MasteryChangeLog>()
                        .eq(MasteryChangeLog::getMasteryId, mastery.getId())
                        .orderByDesc(MasteryChangeLog::getOccurredAt)
                        .orderByDesc(MasteryChangeLog::getId))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public PageResult<LearningView.RecommendationItem> pageRecommendations(
            Long studentId, long page, long size, String status) {
        Page<Recommendation> result = recommendationMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<Recommendation>()
                        .eq(Recommendation::getStudentId, studentId)
                        .eq(status != null, Recommendation::getStatus, status)
                        .orderByDesc(Recommendation::getPriority)
                        .orderByDesc(Recommendation::getGeneratedAt));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public PageResult<LearningView.PhotoTask> pagePhotoTasks(
            Long studentId, long page, long size, String status) {
        Page<PhotoMarkingTask> result = photoMarkingTaskMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<PhotoMarkingTask>()
                        .eq(PhotoMarkingTask::getStudentId, studentId)
                        .eq(status != null, PhotoMarkingTask::getStatus, status)
                        .orderByDesc(PhotoMarkingTask::getSubmittedAt)
                        .orderByDesc(PhotoMarkingTask::getId));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public LearningView.PhotoDetail getPhotoTask(Long studentId, Long markingTaskId) {
        PhotoMarkingTask task = photoMarkingTaskMapper.selectOne(
                new LambdaQueryWrapper<PhotoMarkingTask>()
                        .eq(PhotoMarkingTask::getId, markingTaskId)
                        .eq(PhotoMarkingTask::getStudentId, studentId));
        if (task == null) {
            throw new BusinessException(ResultCode.PHOTO_MARKING_TASK_NOT_FOUND);
        }
        List<LearningView.PhotoItem> items = photoMarkingItemMapper.selectList(
                        new LambdaQueryWrapper<PhotoMarkingItem>()
                                .eq(PhotoMarkingItem::getMarkingTaskId, markingTaskId)
                                .orderByAsc(PhotoMarkingItem::getItemNo))
                .stream()
                .map(this::toView)
                .toList();
        return new LearningView.PhotoDetail(toView(task), items);
    }

    @Override
    public LearningView.Overview getOverview(Long studentId) {
        long taskCount = practiceTaskMapper.selectCount(new LambdaQueryWrapper<PracticeTask>()
                .eq(PracticeTask::getStudentId, studentId));
        long completedTaskCount = practiceTaskMapper.selectCount(new LambdaQueryWrapper<PracticeTask>()
                .eq(PracticeTask::getStudentId, studentId)
                .eq(PracticeTask::getStatus, "COMPLETED"));
        long answerCount = answerRecordMapper.selectCount(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getStudentId, studentId)
                .eq(AnswerRecord::getGradingStatus, "GRADED"));
        long correctAnswerCount = answerRecordMapper.selectCount(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getStudentId, studentId)
                .eq(AnswerRecord::getGradingStatus, "GRADED")
                .eq(AnswerRecord::getCorrect, true));
        BigDecimal accuracy = answerCount == 0
                ? BigDecimal.ZERO.setScale(4)
                : BigDecimal.valueOf(correctAnswerCount)
                        .divide(BigDecimal.valueOf(answerCount), 4, RoundingMode.HALF_UP);
        long openErrorCount = errorRecordMapper.selectCount(new LambdaQueryWrapper<ErrorRecord>()
                .eq(ErrorRecord::getStudentId, studentId)
                .ne(ErrorRecord::getStatus, "RESOLVED"));
        long weakPointCount = masteryRecordMapper.selectCount(new LambdaQueryWrapper<MasteryRecord>()
                .eq(MasteryRecord::getStudentId, studentId)
                .lt(MasteryRecord::getMasteryScore, new BigDecimal("0.6000")));
        long recommendationCount = recommendationMapper.selectCount(
                new LambdaQueryWrapper<Recommendation>()
                        .eq(Recommendation::getStudentId, studentId)
                        .eq(Recommendation::getStatus, "PENDING"));
        return new LearningView.Overview(
                studentId, taskCount, completedTaskCount, answerCount, correctAnswerCount,
                accuracy, openErrorCount, weakPointCount, recommendationCount);
    }

    private PracticeTask requireTask(Long taskId, Long studentId) {
        PracticeTask task = practiceTaskMapper.selectOne(new LambdaQueryWrapper<PracticeTask>()
                .eq(PracticeTask::getId, taskId)
                .eq(studentId != null, PracticeTask::getStudentId, studentId));
        if (task == null) {
            throw new BusinessException(ResultCode.PRACTICE_TASK_NOT_FOUND);
        }
        return task;
    }

    private PracticeTaskSummaryResponse toSummary(PracticeTask task) {
        return new PracticeTaskSummaryResponse(
                task.getId(), task.getStudentId(), task.getSourceType(), task.getSourceId(),
                task.getTitle(), task.getMode(), task.getStatus(), task.getQuestionCount(),
                task.getTimeLimitSeconds(), task.getTargetAccuracy(), task.getStartedAt(),
                task.getCompletedAt());
    }

    private LearningView.PracticeQuestion toView(
            PracticeTask task, PracticeTaskQuestion question) {
        QuestionResponse snapshot;
        try {
            snapshot = objectMapper.readValue(question.getQuestionSnapshot(), QuestionResponse.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "练习题目快照损坏");
        }
        AnswerRecord latest = answerRecordMapper.selectOne(new LambdaQueryWrapper<AnswerRecord>()
                .eq(AnswerRecord::getTaskQuestionId, question.getId())
                .eq(AnswerRecord::getStudentId, task.getStudentId())
                .orderByDesc(AnswerRecord::getAttemptNo)
                .last("LIMIT 1"));
        boolean solutionVisible = "COMPLETED".equals(task.getStatus())
                || (!"ASSIGNMENT".equals(task.getSourceType())
                && latest != null && "GRADED".equals(latest.getGradingStatus()));
        return new LearningView.PracticeQuestion(
                question.getId(), question.getQuestionId(), question.getSequenceNo(),
                snapshot.questionType(), snapshot.stem(), snapshot.options(), snapshot.difficulty(),
                snapshot.estimatedSeconds(), question.getMaxScore(),
                latest == null ? null : toView(latest), solutionVisible,
                solutionVisible ? snapshot.standardAnswer() : null,
                solutionVisible ? snapshot.analysis() : null);
    }

    private LearningView.Answer toView(AnswerRecord answer) {
        return new LearningView.Answer(
                answer.getId(), answer.getTaskQuestionId(), answer.getAttemptNo(),
                answer.getStudentAnswer(), answer.getGradingStatus(), answer.getCorrect(),
                answer.getScore(), answer.getResponseMs(), answer.getAnswerSource(),
                answer.getRecognitionConfidence(), answer.getSubmittedAt(), answer.getGradedAt());
    }

    private LearningView.Error toView(ErrorRecord error) {
        return new LearningView.Error(
                error.getId(), error.getAnswerRecordId(), error.getQuestionId(),
                error.getKnowledgePointId(), error.getErrorTypeId(), error.getErrorPosition(),
                error.getDiagnosis(), error.getConfidence(), error.getStatus(),
                error.getResolvedByAnswerId(), error.getResolvedAt());
    }

    private LearningView.Mastery toView(MasteryRecord mastery) {
        return new LearningView.Mastery(
                mastery.getId(), mastery.getKnowledgePointId(), mastery.getMasteryScore(),
                mastery.getAccuracyRate(), mastery.getAvgResponseMs(), mastery.getTotalAnswerCount(),
                mastery.getCorrectAnswerCount(), mastery.getConsecutiveCorrectCount(),
                mastery.getLastPracticedAt(), mastery.getNextReviewAt());
    }

    private LearningView.MasteryChange toView(MasteryChangeLog change) {
        return new LearningView.MasteryChange(
                change.getId(), change.getAnswerRecordId(), change.getBeforeScore(),
                change.getAfterScore(), change.getDeltaScore(), change.getReason(),
                change.getOccurredAt());
    }

    private LearningView.RecommendationItem toView(Recommendation recommendation) {
        return new LearningView.RecommendationItem(
                recommendation.getId(), recommendation.getKnowledgePointId(),
                recommendation.getSourceType(), recommendation.getSourceId(),
                recommendation.getReasonText(), recommendation.getPriority(),
                recommendation.getRecommendedQuestionCount(), recommendation.getStatus(),
                recommendation.getGeneratedTaskId(), recommendation.getGeneratedAt(),
                recommendation.getExpiresAt(), recommendation.getAcceptedAt(),
                recommendation.getCompletedAt());
    }

    private LearningView.PhotoTask toView(PhotoMarkingTask task) {
        return new LearningView.PhotoTask(
                task.getId(), task.getOriginalFileId(), task.getPracticeTaskId(), task.getStatus(),
                task.getPageCount(), task.getOverallConfidence(), task.getRecognizedItemCount(),
                task.getReviewStatus(), task.getSubmittedAt(), task.getProcessedAt(),
                task.getReviewedAt(), task.getFailureReason());
    }

    private LearningView.PhotoItem toView(PhotoMarkingItem item) {
        return new LearningView.PhotoItem(
                item.getId(), item.getItemNo(), item.getCropFileId(), item.getBoundingBox(),
                item.getRecognizedStem(), item.getRecognizedAnswer(), item.getMatchedQuestionId(),
                item.getRecognitionConfidence(), item.getTaskQuestionId(), item.getAnswerRecordId(),
                item.getStatus());
    }

    private <T> PageResult<T> pageResult(Page<?> page, List<T> records) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }
}
