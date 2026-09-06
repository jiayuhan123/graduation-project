package xyz.jiayuhan.learningservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;
import xyz.jiayuhan.learningservice.model.response.LearningView;

import java.util.List;

/** 学习档案只读应用服务；写入流程由后续用例服务按事务边界实现。 */
public interface LearningQueryService {

    PageResult<PracticeTaskSummaryResponse> pagePracticeTasks(
            Long studentId, long page, long size, String status);

    LearningView.PracticeDetail getPracticeTask(Long studentId, Long taskId);

    PracticeTaskSummaryResponse getPracticeTaskInternal(Long taskId);

    PageResult<LearningView.Answer> pageAnswers(
            Long studentId, long page, long size, String gradingStatus);

    PageResult<LearningView.Error> pageErrors(
            Long studentId, long page, long size, String status, Long knowledgePointId);

    List<LearningView.Mastery> listMastery(Long studentId, Long knowledgePointId);

    List<LearningView.MasteryChange> listMasteryChanges(
            Long studentId, Long knowledgePointId);

    PageResult<LearningView.RecommendationItem> pageRecommendations(
            Long studentId, long page, long size, String status);

    PageResult<LearningView.PhotoTask> pagePhotoTasks(
            Long studentId, long page, long size, String status);

    LearningView.PhotoDetail getPhotoTask(Long studentId, Long markingTaskId);
}
