package xyz.jiayuhan.learningservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
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
import xyz.jiayuhan.learningservice.model.request.ReviewPhotoItemRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.service.LearningCommandService;
import xyz.jiayuhan.learningservice.service.LearningQueryService;
import xyz.jiayuhan.learningservice.service.LearningReportService;

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
