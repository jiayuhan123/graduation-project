package xyz.jiayuhan.learningservice.service;

import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;

/** 教师班级学情聚合。 */
public interface ClassLearningAnalyticsService {

    LearningView.ClassDashboard getDashboard(Long requesterId, Long classId, int days);

    LearningView.ClassPracticeCreationResult createTargetedPractice(
            Long requesterId, Long classId, CreatePracticeTaskRequest request);
}
