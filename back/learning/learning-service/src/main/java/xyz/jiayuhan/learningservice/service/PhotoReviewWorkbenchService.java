package xyz.jiayuhan.learningservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.learningservice.model.request.ReviewPhotoItemRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;

import java.time.OffsetDateTime;
import java.util.List;

/** 教师和管理员 OCR 复核工作台。 */
public interface PhotoReviewWorkbenchService {

    PageResult<LearningView.PhotoReviewTask> pageTasks(
            Long requesterId,
            Long classId,
            Long studentId,
            long page,
            long size,
            String status,
            String reviewStatus,
            OffsetDateTime submittedFrom,
            OffsetDateTime submittedTo);

    LearningView.PhotoReviewDetail getDetail(Long requesterId, Long markingTaskId);

    LearningView.PhotoReviewDetail retry(Long requesterId, Long markingTaskId);

    LearningView.PhotoReviewDetail reviewItem(
            Long requesterId,
            Long markingTaskId,
            Long markingItemId,
            ReviewPhotoItemRequest request);

    LearningView.BatchPhotoReviewResult approveHighConfidence(
            Long requesterId, List<Long> markingTaskIds);
}
