package xyz.jiayuhan.monomer.adapter;

import org.springframework.stereotype.Component;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.ErrorTypeResponse;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.contentservice.controller.internal.InternalContentController;

import java.util.List;

/** 用进程内调用实现原内容服务契约。 */
@Component
public class LocalContentClient implements ContentFeignClient {

    private final InternalContentController controller;

    public LocalContentClient(InternalContentController controller) {
        this.controller = controller;
    }

    @Override
    public Result<QuestionResponse> getQuestion(Long questionId) {
        return controller.getQuestion(questionId);
    }

    @Override
    public Result<List<QuestionResponse>> getQuestions(BatchQuestionQueryRequest request) {
        return controller.getQuestions(request);
    }

    @Override
    public Result<PageResult<QuestionResponse>> pageQuestions(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionMode,
            String questionType,
            Integer difficulty) {
        return controller.pageQuestions(
                page, size, keyword, knowledgePointId, questionMode, questionType, difficulty);
    }

    @Override
    public Result<KnowledgePointResponse> getKnowledgePoint(Long knowledgePointId) {
        return controller.getKnowledgePoint(knowledgePointId);
    }

    @Override
    public Result<List<ErrorTypeResponse>> listErrorTypes() {
        return controller.listErrorTypes();
    }
}
