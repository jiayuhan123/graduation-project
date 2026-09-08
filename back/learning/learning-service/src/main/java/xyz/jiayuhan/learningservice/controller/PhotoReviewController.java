package xyz.jiayuhan.learningservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.learningservice.model.request.BatchApprovePhotoRequest;
import xyz.jiayuhan.learningservice.model.request.ReviewPhotoItemRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.service.LearningCommandService;
import xyz.jiayuhan.learningservice.service.LearningQueryService;
import xyz.jiayuhan.learningservice.service.LearningReportService;
import xyz.jiayuhan.learningservice.service.PhotoReviewWorkbenchService;

import java.time.OffsetDateTime;

/** 教师与管理员在学情权限范围内复核低可信 OCR 结果。 */
@Tag(name = "拍照批改复核")
@Validated
@RestController
@RequestMapping("/api/learning/photo-reviews")
@RequiredArgsConstructor
public class PhotoReviewController {

    private final LearningQueryService learningQueryService;
    private final LearningReportService learningReportService;
    private final LearningCommandService learningCommandService;
    private final PhotoReviewWorkbenchService workbenchService;

    @Operation(summary = "按班级、学生、状态和时间分页查询复核任务")
    @GetMapping
    public Result<PageResult<LearningView.PhotoReviewTask>> pageWorkbench(
            @RequestParam(required = false) Long classId,
            @RequestParam(required = false) Long studentId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false)
            @Pattern(regexp = "UPLOADED|PROCESSING|REVIEW_REQUIRED|COMPLETED|FAILED", message = "任务状态不正确")
            String status,
            @RequestParam(required = false)
            @Pattern(regexp = "NOT_REQUIRED|PENDING|APPROVED|CORRECTED", message = "复核状态不正确")
            String reviewStatus,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime submittedTo) {
        return Result.success(workbenchService.pageTasks(
                UserContext.requireUserId(), classId, studentId, page, size, status,
                reviewStatus, submittedFrom, submittedTo));
    }

    @Operation(summary = "查询复核工作台任务详情和处理历史")
    @GetMapping("/{markingTaskId}")
    public Result<LearningView.PhotoReviewDetail> workbenchDetail(
            @PathVariable Long markingTaskId) {
        return Result.success(workbenchService.getDetail(
                UserContext.requireUserId(), markingTaskId));
    }

    @Operation(summary = "在复核工作台处理一道识别结果")
    @PostMapping("/{markingTaskId}/items/{markingItemId}")
    public Result<LearningView.PhotoReviewDetail> reviewWorkbenchItem(
            @PathVariable Long markingTaskId,
            @PathVariable Long markingItemId,
            @Valid @RequestBody ReviewPhotoItemRequest request) {
        return Result.success("复核结果已保存", workbenchService.reviewItem(
                UserContext.requireUserId(), markingTaskId, markingItemId, request));
    }

    @Operation(summary = "将失败的识别任务重新放回处理队列")
    @PostMapping("/{markingTaskId}/retry")
    public Result<LearningView.PhotoReviewDetail> retry(@PathVariable Long markingTaskId) {
        return Result.success("任务已重新进入识别队列", workbenchService.retry(
                UserContext.requireUserId(), markingTaskId));
    }

    @Operation(summary = "批量通过已匹配且可信度不低于80%的识别结果")
    @PostMapping("/batch-approve")
    public Result<LearningView.BatchPhotoReviewResult> batchApprove(
            @Valid @RequestBody BatchApprovePhotoRequest request) {
        return Result.success("高可信识别结果已批量通过",
                workbenchService.approveHighConfidence(
                        UserContext.requireUserId(), request.markingTaskIds()));
    }

    @Operation(summary = "查询一个有权访问学生的拍照任务")
    @GetMapping("/students/{studentId}")
    public Result<PageResult<LearningView.PhotoTask>> pageStudentTasks(
            @PathVariable Long studentId,
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        learningReportService.getStudentReport(UserContext.requireUserId(), studentId);
        return Result.success(learningQueryService.pagePhotoTasks(studentId, page, size, status));
    }

    @Operation(summary = "查询一个有权访问学生的拍照任务详情")
    @GetMapping("/students/{studentId}/{markingTaskId}")
    public Result<LearningView.PhotoDetail> getStudentTask(
            @PathVariable Long studentId,
            @PathVariable Long markingTaskId) {
        learningReportService.getStudentReport(UserContext.requireUserId(), studentId);
        return Result.success(learningQueryService.getPhotoTask(studentId, markingTaskId));
    }

    @Operation(summary = "复核一道低可信 OCR 结果")
    @PostMapping("/students/{studentId}/{markingTaskId}/items/{markingItemId}")
    public Result<LearningView.PhotoDetail> reviewItem(
            @PathVariable Long studentId,
            @PathVariable Long markingTaskId,
            @PathVariable Long markingItemId,
            @Valid @RequestBody ReviewPhotoItemRequest request) {
        Long reviewerId = UserContext.requireUserId();
        learningReportService.getStudentReport(reviewerId, studentId);
        return Result.success(learningCommandService.reviewPhotoItem(
                reviewerId, markingTaskId, markingItemId, request));
    }
}
