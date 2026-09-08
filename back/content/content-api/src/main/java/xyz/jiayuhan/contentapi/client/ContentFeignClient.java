package xyz.jiayuhan.contentapi.client;

import jakarta.validation.Valid;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentapi.ContentApiConstants;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.ErrorTypeResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;

import java.util.List;

/** 内容题库内部只读客户端。 */
@FeignClient(
        name = ContentApiConstants.SERVICE_NAME,
        contextId = "contentFeignClient",
        path = ContentApiConstants.INTERNAL_PATH,
        configuration = ContentFeignConfiguration.class
)
public interface ContentFeignClient {

    @GetMapping("/questions/{questionId}")
    Result<QuestionResponse> getQuestion(@PathVariable("questionId") Long questionId);

    @PostMapping("/questions/batch")
    Result<List<QuestionResponse>> getQuestions(
            @Valid @RequestBody BatchQuestionQueryRequest request);

    @GetMapping("/questions")
    Result<PageResult<QuestionResponse>> pageQuestions(
            @RequestParam(name = "page", defaultValue = "1") long page,
            @RequestParam(name = "size", defaultValue = "20") long size,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "knowledgePointId", required = false) Long knowledgePointId,
            @RequestParam(name = "questionMode", required = false) String questionMode,
            @RequestParam(name = "questionType", required = false) String questionType,
            @RequestParam(name = "difficulty", required = false) Integer difficulty);

    @GetMapping("/knowledge-points/{knowledgePointId}")
    Result<KnowledgePointResponse> getKnowledgePoint(
            @PathVariable("knowledgePointId") Long knowledgePointId);

    @GetMapping("/error-types")
    Result<List<ErrorTypeResponse>> listErrorTypes();
}
