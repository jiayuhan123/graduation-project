package xyz.jiayuhan.contentservice.controller.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentapi.ContentApiConstants;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.contentservice.service.ContentCatalogService;

import java.util.List;

/** 仅供受信任微服务调用的内容查询契约。 */
@RestController
@RequestMapping(ContentApiConstants.INTERNAL_PATH)
@RequiredArgsConstructor
public class InternalContentController {

    private final ContentCatalogService contentCatalogService;

    @GetMapping("/questions/{questionId}")
    public Result<QuestionResponse> getQuestion(@PathVariable Long questionId) {
        return Result.success(contentCatalogService.getQuestionInternal(questionId));
    }

    @PostMapping("/questions/batch")
    public Result<List<QuestionResponse>> getQuestions(
            @Valid @RequestBody BatchQuestionQueryRequest request) {
        return Result.success(contentCatalogService.getQuestions(request.questionIds()));
    }

    @GetMapping("/questions")
    public Result<PageResult<QuestionResponse>> pageQuestions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) Integer difficulty) {
        return Result.success(contentCatalogService.pageQuestionsInternal(
                page, size, keyword, knowledgePointId, questionType, difficulty));
    }

    @GetMapping("/knowledge-points/{knowledgePointId}")
    public Result<KnowledgePointResponse> getKnowledgePoint(@PathVariable Long knowledgePointId) {
        return Result.success(contentCatalogService.getKnowledgePoint(knowledgePointId, false));
    }
}
