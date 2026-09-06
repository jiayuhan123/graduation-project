package xyz.jiayuhan.contentservice.controller.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentservice.model.request.CreateContentModuleRequest;
import xyz.jiayuhan.contentservice.model.request.CreateErrorTypeRequest;
import xyz.jiayuhan.contentservice.model.request.CreateKnowledgePointRequest;
import xyz.jiayuhan.contentservice.model.request.CreateQuestionRequest;
import xyz.jiayuhan.contentservice.model.request.ReviewQuestionRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateContentModuleRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateEnabledStatusRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateErrorTypeRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateKnowledgePointRequest;
import xyz.jiayuhan.contentservice.model.request.UpdateQuestionRequest;
import xyz.jiayuhan.contentservice.model.response.AdminContentModuleResponse;
import xyz.jiayuhan.contentservice.model.response.AdminErrorTypeResponse;
import xyz.jiayuhan.contentservice.model.response.AdminKnowledgePointResponse;
import xyz.jiayuhan.contentservice.model.response.AdminQuestionDetailResponse;
import xyz.jiayuhan.contentservice.model.response.AdminQuestionSummaryResponse;
import xyz.jiayuhan.contentservice.service.ContentAdminService;

import java.util.List;

/** 受 content:manage 权限保护的内容管理接口。 */
@Tag(name = "内容管理")
@Validated
@RestController
@RequestMapping("/api/content/admin")
@RequiredArgsConstructor
public class ContentAdminController {

    private final ContentAdminService contentAdminService;

    @Operation(summary = "分页查询全部内容模块")
    @GetMapping("/modules")
    public Result<PageResult<AdminContentModuleResponse>> pageModules(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(1) @Max(6) Integer gradeLevel,
            @RequestParam(required = false) @Min(1) @Max(2) Integer semester,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(contentAdminService.pageModules(
                page, size, keyword, gradeLevel, semester, status));
    }

    @GetMapping("/modules/{moduleId}")
    public Result<AdminContentModuleResponse> getModule(@PathVariable Long moduleId) {
        return Result.success(contentAdminService.getModule(moduleId));
    }

    @PostMapping("/modules")
    public Result<AdminContentModuleResponse> createModule(
            @Valid @RequestBody CreateContentModuleRequest request) {
        return Result.success("内容模块创建成功", contentAdminService.createModule(request));
    }

    @PutMapping("/modules/{moduleId}")
    public Result<AdminContentModuleResponse> updateModule(
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateContentModuleRequest request) {
        return Result.success("内容模块修改成功", contentAdminService.updateModule(moduleId, request));
    }

    @PutMapping("/modules/{moduleId}/status")
    public Result<AdminContentModuleResponse> updateModuleStatus(
            @PathVariable Long moduleId,
            @Valid @RequestBody UpdateEnabledStatusRequest request) {
        return Result.success(contentAdminService.updateModuleStatus(moduleId, request));
    }

    @DeleteMapping("/modules/{moduleId}")
    public Result<Void> deleteModule(
            @PathVariable Long moduleId,
            @RequestParam @Min(0) Integer version) {
        contentAdminService.deleteModule(moduleId, version);
        return Result.success();
    }

    @Operation(summary = "查询知识点")
    @GetMapping("/knowledge-points")
    public Result<List<AdminKnowledgePointResponse>> listKnowledgePoints(
            @RequestParam(required = false) Long moduleId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(contentAdminService.listKnowledgePoints(moduleId, keyword, status));
    }

    @GetMapping("/knowledge-points/{knowledgePointId}")
    public Result<AdminKnowledgePointResponse> getKnowledgePoint(@PathVariable Long knowledgePointId) {
        return Result.success(contentAdminService.getKnowledgePoint(knowledgePointId));
    }

    @PostMapping("/knowledge-points")
    public Result<AdminKnowledgePointResponse> createKnowledgePoint(
            @Valid @RequestBody CreateKnowledgePointRequest request) {
        return Result.success("知识点创建成功", contentAdminService.createKnowledgePoint(request));
    }

    @PutMapping("/knowledge-points/{knowledgePointId}")
    public Result<AdminKnowledgePointResponse> updateKnowledgePoint(
            @PathVariable Long knowledgePointId,
            @Valid @RequestBody UpdateKnowledgePointRequest request) {
        return Result.success(
                "知识点修改成功", contentAdminService.updateKnowledgePoint(knowledgePointId, request));
    }

    @PutMapping("/knowledge-points/{knowledgePointId}/status")
    public Result<AdminKnowledgePointResponse> updateKnowledgePointStatus(
            @PathVariable Long knowledgePointId,
            @Valid @RequestBody UpdateEnabledStatusRequest request) {
        return Result.success(contentAdminService.updateKnowledgePointStatus(knowledgePointId, request));
    }

    @DeleteMapping("/knowledge-points/{knowledgePointId}")
    public Result<Void> deleteKnowledgePoint(
            @PathVariable Long knowledgePointId,
            @RequestParam @Min(0) Integer version) {
        contentAdminService.deleteKnowledgePoint(knowledgePointId, version);
        return Result.success();
    }

    @Operation(summary = "分页查询管理端题目")
    @GetMapping("/questions")
    public Result<PageResult<AdminQuestionSummaryResponse>> pageQuestions(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false)
            @Pattern(regexp = "FIXED|TEMPLATE", message = "题目模式不正确") String questionMode,
            @RequestParam(required = false)
            @Pattern(regexp = "ARITHMETIC|FILL_BLANK|VERTICAL|CHOICE|JUDGEMENT", message = "题型不正确")
            String questionType,
            @RequestParam(required = false) @Min(1) @Max(5) Integer difficulty,
            @RequestParam(required = false)
            @Pattern(regexp = "DRAFT|ENABLED|DISABLED", message = "题目状态不正确") String status) {
        return Result.success(contentAdminService.pageQuestions(
                page, size, keyword, knowledgePointId, questionMode, questionType, difficulty, status));
    }

    @GetMapping("/questions/{questionId}")
    public Result<AdminQuestionDetailResponse> getQuestion(@PathVariable Long questionId) {
        return Result.success(contentAdminService.getQuestion(questionId));
    }

    @PostMapping("/questions")
    public Result<AdminQuestionDetailResponse> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request) {
        return Result.success("题目创建成功", contentAdminService.createQuestion(request));
    }

    @PutMapping("/questions/{questionId}")
    public Result<AdminQuestionDetailResponse> updateQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody UpdateQuestionRequest request) {
        return Result.success("题目修改成功", contentAdminService.updateQuestion(questionId, request));
    }

    @PostMapping("/questions/{questionId}/review")
    public Result<AdminQuestionDetailResponse> reviewQuestion(
            @PathVariable Long questionId,
            @Valid @RequestBody ReviewQuestionRequest request) {
        return Result.success("题目状态更新成功", contentAdminService.reviewQuestion(questionId, request));
    }

    @DeleteMapping("/questions/{questionId}")
    public Result<Void> deleteQuestion(
            @PathVariable Long questionId,
            @RequestParam @Min(0) Integer version) {
        contentAdminService.deleteQuestion(questionId, version);
        return Result.success();
    }

    @Operation(summary = "查询管理端错因字典")
    @GetMapping("/error-types")
    public Result<List<AdminErrorTypeResponse>> listErrorTypes(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false)
            @Pattern(regexp = "CALCULATION|PLACE_VALUE|CARRY|BORROW|FORMAT|CONCEPT|OTHER", message = "错因分类不正确")
            String category,
            @RequestParam(required = false)
            @Pattern(regexp = "ARITHMETIC|FILL_BLANK|VERTICAL|CHOICE|JUDGEMENT", message = "适用题型不正确")
            String applicableQuestionType,
            @RequestParam(required = false) @Min(0) @Max(1) Integer status) {
        return Result.success(contentAdminService.listErrorTypes(
                keyword, category, applicableQuestionType, status));
    }

    @GetMapping("/error-types/{errorTypeId}")
    public Result<AdminErrorTypeResponse> getErrorType(@PathVariable Long errorTypeId) {
        return Result.success(contentAdminService.getErrorType(errorTypeId));
    }

    @PostMapping("/error-types")
    public Result<AdminErrorTypeResponse> createErrorType(
            @Valid @RequestBody CreateErrorTypeRequest request) {
        return Result.success("错因类型创建成功", contentAdminService.createErrorType(request));
    }

    @PutMapping("/error-types/{errorTypeId}")
    public Result<AdminErrorTypeResponse> updateErrorType(
            @PathVariable Long errorTypeId,
            @Valid @RequestBody UpdateErrorTypeRequest request) {
        return Result.success("错因类型修改成功", contentAdminService.updateErrorType(errorTypeId, request));
    }

    @PutMapping("/error-types/{errorTypeId}/status")
    public Result<AdminErrorTypeResponse> updateErrorTypeStatus(
            @PathVariable Long errorTypeId,
            @Valid @RequestBody UpdateEnabledStatusRequest request) {
        return Result.success(contentAdminService.updateErrorTypeStatus(errorTypeId, request));
    }

    @DeleteMapping("/error-types/{errorTypeId}")
    public Result<Void> deleteErrorType(
            @PathVariable Long errorTypeId,
            @RequestParam @Min(0) Integer version) {
        contentAdminService.deleteErrorType(errorTypeId, version);
        return Result.success();
    }
}
