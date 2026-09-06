package xyz.jiayuhan.teachingservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

/** 当前用户可见的班级、作业和提交记录。 */
@Tag(name = "教学查询")
@Validated
@RestController
@RequestMapping("/api/teaching")
@RequiredArgsConstructor
public class TeachingQueryController {

    private final TeachingQueryService teachingQueryService;

    @Operation(summary = "分页查询我任教的班级")
    @GetMapping("/classes/teaching")
    public Result<PageResult<TeachingView.ClassMembership>> pageTeachingClasses(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.success(teachingQueryService.pageTeachingClasses(
                UserContext.requireUserId(), page, size));
    }

    @Operation(summary = "分页查询我加入的班级")
    @GetMapping("/classes/enrolled")
    public Result<PageResult<TeachingView.ClassMembership>> pageEnrolledClasses(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(teachingQueryService.pageEnrolledClasses(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "分页查询我创建的作业")
    @GetMapping("/assignments/created")
    public Result<PageResult<AssignmentSummaryResponse>> pageCreatedAssignments(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(teachingQueryService.pageCreatedAssignments(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "查询我有权访问的作业详情")
    @GetMapping("/assignments/{assignmentId}")
    public Result<TeachingView.AssignmentDetail> getAssignment(@PathVariable Long assignmentId) {
        return Result.success(teachingQueryService.getAssignmentForUser(
                UserContext.requireUserId(), assignmentId));
    }

    @Operation(summary = "分页查询我的作业提交")
    @GetMapping("/submissions/mine")
    public Result<PageResult<TeachingView.Submission>> pageMySubmissions(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(teachingQueryService.pageMySubmissions(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "分页查询分配给我的作业")
    @GetMapping("/student/assignments")
    public Result<PageResult<TeachingView.StudentAssignment>> pageStudentAssignments(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(teachingQueryService.pageStudentAssignments(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "查询分配给我的作业详情")
    @GetMapping("/student/assignments/{assignmentId}")
    public Result<TeachingView.AssignmentDetail> getStudentAssignment(
            @PathVariable Long assignmentId) {
        return Result.success(teachingQueryService.getAssignmentForUser(
                UserContext.requireUserId(), assignmentId));
    }
}
