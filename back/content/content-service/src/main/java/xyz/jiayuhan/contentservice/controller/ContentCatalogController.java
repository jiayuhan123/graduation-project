package xyz.jiayuhan.contentservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.model.response.ContentModuleResponse;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentservice.service.ContentCatalogService;
import xyz.jiayuhan.contentservice.service.QuestionFeedbackService;
import xyz.jiayuhan.contentservice.model.request.CreateQuestionFeedbackRequest;
import xyz.jiayuhan.contentservice.model.response.QuestionFeedbackResponse;
import xyz.jiayuhan.contentservice.model.response.PublishedQuestionResponse;
import xyz.jiayuhan.common.security.context.UserContext;

import java.util.List;

/** 学习端可读取的已发布内容目录接口。 */
@Tag(name = "内容目录")
@Validated
@RestController
@RequestMapping("/api/content")
@RequiredArgsConstructor
public class ContentCatalogController {

    private final ContentCatalogService contentCatalogService;
    private final QuestionFeedbackService questionFeedbackService;

    @Operation(summary = "反馈题目内容或答案问题")
    @PostMapping("/questions/{questionId}/feedback")
    public Result<QuestionFeedbackResponse> createQuestionFeedback(
            @PathVariable Long questionId,
            @Valid @RequestBody CreateQuestionFeedbackRequest request) {
        return Result.success("题目反馈已提交", questionFeedbackService.create(
                UserContext.requireUserId(), questionId, request));
    }

    @Operation(summary = "分页查询可用于练习和复核匹配的已发布题目")
    @GetMapping("/questions")
    public Result<PageResult<PublishedQuestionResponse>> pageQuestions(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) String questionType,
            @RequestParam(required = false) @Min(1) @Max(5) Integer difficulty) {
        return Result.success(contentCatalogService.pagePublishedQuestions(
                page, size, knowledgePointId, questionType, difficulty, "ENABLED"));
    }

    @Operation(summary = "分页查询已启用内容模块")
    @GetMapping("/modules")
    public Result<PageResult<ContentModuleResponse>> pageModules(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Min(1) @Max(6) Integer gradeLevel,
            @RequestParam(required = false) @Min(1) @Max(2) Integer semester) {
        return Result.success(contentCatalogService.pageModules(page, size, gradeLevel, semester, 1));
    }

    @Operation(summary = "查询内容模块详情")
    @GetMapping("/modules/{moduleId}")
    public Result<ContentModuleResponse> getModule(@PathVariable Long moduleId) {
        return Result.success(contentCatalogService.getModule(moduleId, true));
    }

    @Operation(summary = "查询已启用知识点")
    @GetMapping("/knowledge-points")
    public Result<List<KnowledgePointResponse>> listKnowledgePoints(
            @RequestParam Long moduleId,
            @RequestParam(required = false) Long parentId) {
        return Result.success(contentCatalogService.listKnowledgePoints(moduleId, parentId, 1));
    }

    @Operation(summary = "查询已启用知识点详情")
    @GetMapping("/knowledge-points/{knowledgePointId}")
    public Result<KnowledgePointResponse> getKnowledgePoint(@PathVariable Long knowledgePointId) {
        return Result.success(contentCatalogService.getKnowledgePoint(knowledgePointId, true));
    }

}
