package xyz.jiayuhan.learningservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.learningservice.entity.AnswerRecord;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;
import xyz.jiayuhan.learningservice.entity.PhotoReviewRecord;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.mapper.AnswerRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingItemMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoMarkingTaskMapper;
import xyz.jiayuhan.learningservice.mapper.PhotoReviewRecordMapper;
import xyz.jiayuhan.learningservice.mapper.PracticeTaskMapper;
import xyz.jiayuhan.learningservice.model.response.LearningOperationsStatisticsResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** 仅管理员可读取的全局学习与 OCR 质量指标。 */
@Service
@RequiredArgsConstructor
public class LearningOperationsServiceImpl implements LearningOperationsService {

    private final PracticeTaskMapper practiceTaskMapper;
    private final AnswerRecordMapper answerRecordMapper;
    private final PhotoMarkingTaskMapper photoMarkingTaskMapper;
    private final PhotoMarkingItemMapper photoMarkingItemMapper;
    private final PhotoReviewRecordMapper photoReviewRecordMapper;
    private final UserFeignClient userFeignClient;

    @Override
    public LearningOperationsStatisticsResponse statistics(Long requesterId) {
        requireAdmin(requesterId);
        long totalTasks = practiceTaskMapper.selectCount(null);
        long completedTasks = practiceTaskMapper.selectCount(
                new LambdaQueryWrapper<PracticeTask>().eq(PracticeTask::getStatus, "COMPLETED"));
        long totalAnswers = answerRecordMapper.selectCount(
                new LambdaQueryWrapper<AnswerRecord>().eq(AnswerRecord::getGradingStatus, "GRADED"));
        long correctAnswers = answerRecordMapper.selectCount(
                new LambdaQueryWrapper<AnswerRecord>()
                        .eq(AnswerRecord::getGradingStatus, "GRADED")
                        .eq(AnswerRecord::getCorrect, true));
        long totalOcrTasks = photoMarkingTaskMapper.selectCount(null);
        long completedOcrTasks = photoMarkingTaskMapper.selectCount(
                new LambdaQueryWrapper<PhotoMarkingTask>()
                        .eq(PhotoMarkingTask::getStatus, "COMPLETED"));
        long failedOcrTasks = photoMarkingTaskMapper.selectCount(
                new LambdaQueryWrapper<PhotoMarkingTask>()
                        .eq(PhotoMarkingTask::getStatus, "FAILED"));
        long pendingReviewTasks = photoMarkingTaskMapper.selectCount(
                new LambdaQueryWrapper<PhotoMarkingTask>()
                        .eq(PhotoMarkingTask::getReviewStatus, "PENDING"));
        long totalItems = photoMarkingItemMapper.selectCount(null);
        long reviewedItems = photoReviewRecordMapper.selectCount(null);
        long approvedItems = reviewCount("APPROVE");
        long correctedItems = reviewCount("CORRECT");
        long rejectedItems = reviewCount("REJECT");
        return new LearningOperationsStatisticsResponse(
                totalTasks, completedTasks, totalAnswers, correctAnswers,
                ratio(correctAnswers, totalAnswers), totalOcrTasks, completedOcrTasks,
                failedOcrTasks, pendingReviewTasks, totalItems, reviewedItems, approvedItems,
                correctedItems, rejectedItems, ratio(failedOcrTasks, totalOcrTasks),
                ratio(reviewedItems, totalItems), ratio(approvedItems, reviewedItems));
    }

    private long reviewCount(String decision) {
        return photoReviewRecordMapper.selectCount(
                new LambdaQueryWrapper<PhotoReviewRecord>()
                        .eq(PhotoReviewRecord::getDecision, decision));
    }

    private void requireAdmin(Long requesterId) {
        Result<UserSummaryResponse> result;
        try {
            result = userFeignClient.getUserSummary(requesterId);
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验管理员身份");
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null || result.getData().roles() == null
                || !result.getData().roles().contains("admin")
                || !Integer.valueOf(1).equals(result.getData().status())) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private BigDecimal ratio(long numerator, long denominator) {
        return denominator == 0
                ? BigDecimal.ZERO.setScale(4)
                : BigDecimal.valueOf(numerator)
                        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP);
    }
}
