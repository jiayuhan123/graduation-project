package xyz.jiayuhan.contentservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentservice.model.request.CreateQuestionFeedbackRequest;
import xyz.jiayuhan.contentservice.model.request.HandleQuestionFeedbackRequest;
import xyz.jiayuhan.contentservice.model.response.QuestionFeedbackResponse;

/** 题目质量反馈提交与处置流程。 */
public interface QuestionFeedbackService {

    QuestionFeedbackResponse create(
            Long reporterId, Long questionId, CreateQuestionFeedbackRequest request);

    PageResult<QuestionFeedbackResponse> page(
            long page, long size, String status, String category, Long questionId);

    QuestionFeedbackResponse handle(
            Long handlerId, Long feedbackId, HandleQuestionFeedbackRequest request);
}
