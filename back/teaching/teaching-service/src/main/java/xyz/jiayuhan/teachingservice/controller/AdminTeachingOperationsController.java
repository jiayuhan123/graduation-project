package xyz.jiayuhan.teachingservice.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingservice.model.response.TeachingOperationsStatisticsResponse;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.service.TeachingCommandService;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

/** 管理员教学运营统计。 */
@RestController
@Validated
@RequestMapping("/api/teaching/admin")
@RequiredArgsConstructor
public class AdminTeachingOperationsController {

    private final TeachingQueryService teachingQueryService;
    private final TeachingCommandService teachingCommandService;

    @GetMapping("/statistics")
    public Result<TeachingOperationsStatisticsResponse> statistics() {
        return Result.success(teachingQueryService.operationsStatistics());
    }

    @GetMapping("/classes")
    public Result<PageResult<TeachingView.ClassMembership>> pageClasses(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false)
            @Pattern(regexp = "ACTIVE|ARCHIVED", message = "班级状态不正确") String status) {
        return Result.success(teachingQueryService.pageAllClasses(page, size, status));
    }

    @GetMapping("/classes/{classId}")
    public Result<TeachingView.ClassDetail> getClass(@PathVariable Long classId) {
        return Result.success(teachingCommandService.getClassDetailAsAdmin(classId));
    }
}
