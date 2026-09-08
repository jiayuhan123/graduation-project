package xyz.jiayuhan.learningservice.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.learningservice.model.response.LearningView;
import xyz.jiayuhan.learningservice.model.request.CreatePracticeTaskRequest;
import xyz.jiayuhan.learningservice.service.ClassLearningAnalyticsService;

/** 教师按真实班级关系查看聚合学情。 */
@Validated
@RestController
@RequestMapping("/api/learning/class-reports")
@RequiredArgsConstructor
public class ClassLearningReportController {

    private final ClassLearningAnalyticsService analyticsService;

    @GetMapping("/{classId}")
    public Result<LearningView.ClassDashboard> dashboard(
            @PathVariable Long classId,
            @RequestParam(defaultValue = "30") @Min(7) @Max(90) int days) {
        return Result.success(analyticsService.getDashboard(
                UserContext.requireUserId(), classId, days));
    }

    @PostMapping("/{classId}/practice-tasks")
    public Result<LearningView.ClassPracticeCreationResult> createTargetedPractice(
            @PathVariable Long classId,
            @Valid @RequestBody CreatePracticeTaskRequest request) {
        return Result.success("已为班级学生生成针对练习",
                analyticsService.createTargetedPractice(
                        UserContext.requireUserId(), classId, request));
    }
}
