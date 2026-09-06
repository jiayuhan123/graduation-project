package xyz.jiayuhan.teachingservice.controller.student;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
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
import xyz.jiayuhan.teachingservice.model.request.JoinClassRequest;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.service.TeachingCommandService;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

/** 学生加入、退出和查看班级。 */
@Tag(name = "学生班级")
@Validated
@RestController
@RequestMapping("/api/teaching/student/classes")
@RequiredArgsConstructor
public class StudentClassController {

    private final TeachingQueryService queryService;
    private final TeachingCommandService commandService;

    @GetMapping
    public Result<PageResult<TeachingView.ClassMembership>> pageClasses(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(queryService.pageEnrolledClasses(
                UserContext.requireUserId(), page, size, status));
    }

    @PostMapping("/join")
    public Result<TeachingView.ClassMembership> joinClass(
            @Valid @RequestBody JoinClassRequest request) {
        return Result.success("加入班级成功",
                commandService.joinClass(UserContext.requireUserId(), request));
    }

    @DeleteMapping("/{classId}/membership")
    public Result<Void> leaveClass(@PathVariable Long classId) {
        commandService.leaveClass(UserContext.requireUserId(), classId);
        return Result.success();
    }
}
