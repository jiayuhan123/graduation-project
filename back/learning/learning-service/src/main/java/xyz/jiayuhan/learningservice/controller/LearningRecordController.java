package xyz.jiayuhan.learningservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.learningapi.model.response.PracticeTaskSummaryResponse;
import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;
import xyz.jiayuhan.learningservice.model.request.SubmitAnswerRequest;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.service.LearningCommandService;
import xyz.jiayuhan.learningservice.service.LearningQueryService;
import xyz.jiayuhan.learningservice.service.LearningReportService;

import java.util.List;

/** 当前学生的练习与学习档案查询接口。 */
@Tag(name = "学习档案")
@Validated
@RestController
@RequestMapping("/api/learning")
@RequiredArgsConstructor
public class LearningRecordController {

    private final LearningQueryService learningQueryService;
    private final LearningCommandService learningCommandService;
    private final LearningReportService learningReportService;

    @Operation(summary = "创建自主练习")
    @PostMapping("/practice-tasks")
    public Result<LearningView.PracticeDetail> createPracticeTask(
            @Valid @RequestBody CreatePracticeTaskRequest request) {
        return Result.success(learningCommandService.createSelfPractice(
                UserContext.requireUserId(), request));
    }

    @Operation(summary = "从教师作业创建或获取练习任务")
    @PostMapping("/assignment-tasks/{assignmentId}")
    public Result<LearningView.PracticeDetail> createAssignmentTask(
            @PathVariable Long assignmentId) {
        return Result.success(learningCommandService.createAssignmentPractice(
                UserContext.requireUserId(), assignmentId));
    }

    @Operation(summary = "分页查询我的练习任务")
    @GetMapping("/practice-tasks")
    public Result<PageResult<PracticeTaskSummaryResponse>> pagePracticeTasks(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(learningQueryService.pagePracticeTasks(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "查询我的练习任务详情")
    @GetMapping("/practice-tasks/{taskId}")
    public Result<LearningView.PracticeDetail> getPracticeTask(@PathVariable Long taskId) {
        return Result.success(learningQueryService.getPracticeTask(UserContext.requireUserId(), taskId));
    }

    @Operation(summary = "开始练习")
    @PostMapping("/practice-tasks/{taskId}/start")
    public Result<LearningView.PracticeDetail> startPractice(@PathVariable Long taskId) {
        return Result.success(learningCommandService.startPractice(
                UserContext.requireUserId(), taskId));
    }

    @Operation(summary = "提交或保存一道题的答案")
    @PostMapping("/practice-tasks/{taskId}/questions/{taskQuestionId}/answers")
    public Result<LearningView.AnswerFeedback> submitAnswer(
            @PathVariable Long taskId,
            @PathVariable Long taskQuestionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        return Result.success(learningCommandService.submitAnswer(
                UserContext.requireUserId(), taskId, taskQuestionId, request));
    }

    @Operation(summary = "提交并完成整次练习")
    @PostMapping("/practice-tasks/{taskId}/complete")
    public Result<LearningView.PracticeDetail> completePractice(@PathVariable Long taskId) {
        return Result.success(learningCommandService.completePractice(
                UserContext.requireUserId(), taskId));
    }

    @Operation(summary = "查询我的学习概览")
    @GetMapping("/overview")
    public Result<LearningView.Overview> getOverview() {
        return Result.success(learningQueryService.getOverview(UserContext.requireUserId()));
    }

    @Operation(summary = "查询我的完整学习报告")
    @GetMapping("/reports/me")
    public Result<LearningView.StudentReport> getOwnReport() {
        return Result.success(learningReportService.getOwnReport(UserContext.requireUserId()));
    }

    @Operation(summary = "教师或管理员查询学生学习报告")
    @GetMapping("/reports/students/{studentId}")
    public Result<LearningView.StudentReport> getStudentReport(@PathVariable Long studentId) {
        return Result.success(learningReportService.getStudentReport(
                UserContext.requireUserId(), studentId));
    }

    @Operation(summary = "分页查询我的答题记录")
    @GetMapping("/answers")
    public Result<PageResult<LearningView.Answer>> pageAnswers(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String gradingStatus) {
        return Result.success(learningQueryService.pageAnswers(
                UserContext.requireUserId(), page, size, gradingStatus));
    }

    @Operation(summary = "分页查询我的错题诊断")
    @GetMapping("/errors")
    public Result<PageResult<LearningView.Error>> pageErrors(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long knowledgePointId) {
        return Result.success(learningQueryService.pageErrors(
                UserContext.requireUserId(), page, size, status, knowledgePointId));
    }

    @Operation(summary = "查询我的知识点掌握情况")
    @GetMapping("/mastery")
    public Result<List<LearningView.Mastery>> listMastery(
            @RequestParam(required = false) Long knowledgePointId) {
        return Result.success(learningQueryService.listMastery(
                UserContext.requireUserId(), knowledgePointId));
    }

    @Operation(summary = "查询一个知识点的掌握度变化")
    @GetMapping("/mastery/{knowledgePointId}/changes")
    public Result<List<LearningView.MasteryChange>> listMasteryChanges(
            @PathVariable Long knowledgePointId) {
        return Result.success(learningQueryService.listMasteryChanges(
                UserContext.requireUserId(), knowledgePointId));
    }

    @Operation(summary = "分页查询我的个性化推荐")
    @GetMapping("/recommendations")
    public Result<PageResult<LearningView.RecommendationItem>> pageRecommendations(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(learningQueryService.pageRecommendations(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "接受个性化推荐并生成巩固任务")
    @PostMapping("/recommendations/{recommendationId}/accept")
    public Result<LearningView.PracticeDetail> acceptRecommendation(
            @PathVariable Long recommendationId) {
        return Result.success(learningCommandService.acceptRecommendation(
                UserContext.requireUserId(), recommendationId));
    }

    @Operation(summary = "忽略一条个性化推荐")
    @DeleteMapping("/recommendations/{recommendationId}")
    public Result<Void> dismissRecommendation(@PathVariable Long recommendationId) {
        learningCommandService.dismissRecommendation(UserContext.requireUserId(), recommendationId);
        return Result.success();
    }

    @Operation(summary = "分页查询我的拍照批改任务")
    @GetMapping("/photo-marking")
    public Result<PageResult<LearningView.PhotoTask>> pagePhotoTasks(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(learningQueryService.pagePhotoTasks(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "查询我的拍照批改任务详情")
    @GetMapping("/photo-marking/{markingTaskId}")
    public Result<LearningView.PhotoDetail> getPhotoTask(@PathVariable Long markingTaskId) {
        return Result.success(learningQueryService.getPhotoTask(
                UserContext.requireUserId(), markingTaskId));
    }
}
