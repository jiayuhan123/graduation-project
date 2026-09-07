package xyz.jiayuhan.learningservice.service;

import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;
import xyz.jiayuhan.learningservice.model.request.SubmitAnswerRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;

/** 练习生成、作答、结算和推荐接受的写模型。 */
public interface LearningCommandService {

    LearningView.PracticeDetail createSelfPractice(
            Long studentId, CreatePracticeTaskRequest request);

    LearningView.PracticeDetail createAssignmentPractice(Long studentId, Long assignmentId);

    LearningView.PracticeDetail startPractice(Long studentId, Long taskId);

    LearningView.AnswerFeedback submitAnswer(
            Long studentId, Long taskId, Long taskQuestionId, SubmitAnswerRequest request);

    LearningView.PracticeDetail completePractice(Long studentId, Long taskId);

    LearningView.PracticeDetail acceptRecommendation(Long studentId, Long recommendationId);

    void dismissRecommendation(Long studentId, Long recommendationId);
}
