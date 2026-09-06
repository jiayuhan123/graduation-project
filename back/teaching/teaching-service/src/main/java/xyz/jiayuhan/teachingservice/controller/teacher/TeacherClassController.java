package xyz.jiayuhan.teachingservice.controller.teacher;

import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.teachingservice.model.request.AddClassStudentRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateTeachingClassStatusRequest;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.service.TeachingCommandService;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

/** 教师班级与学生成员管理。 */
@Tag(name = "教师班级管理")
@Validated
@RestController
@RequestMapping("/api/teaching/teacher/classes")
@RequiredArgsConstructor
public class TeacherClassController {

    private final TeachingQueryService queryService;
    private final TeachingCommandService commandService;

    @GetMapping
    public Result<PageResult<TeachingView.ClassMembership>> pageClasses(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.success(queryService.pageTeachingClasses(UserContext.requireUserId(), page, size));
    }

    @GetMapping("/{classId}")
    public Result<TeachingView.ClassDetail> getClassDetail(@PathVariable Long classId) {
        return Result.success(commandService.getClassDetail(UserContext.requireUserId(), classId));
    }

    @Operation(summary = "创建班级并生成班级码")
    @PostMapping
    public Result<TeachingView.ClassMembership> createClass(
            @Valid @RequestBody CreateTeachingClassRequest request) {
        return Result.success("班级创建成功",
                commandService.createClass(UserContext.requireUserId(), request));
    }

    @PutMapping("/{classId}")
    public Result<TeachingView.ClassMembership> updateClass(
            @PathVariable Long classId,
            @Valid @RequestBody UpdateTeachingClassRequest request) {
        return Result.success("班级修改成功",
                commandService.updateClass(UserContext.requireUserId(), classId, request));
    }

    @PutMapping("/{classId}/status")
    public Result<TeachingView.ClassMembership> updateClassStatus(
            @PathVariable Long classId,
            @Valid @RequestBody UpdateTeachingClassStatusRequest request) {
        return Result.success(commandService.updateClassStatus(
                UserContext.requireUserId(), classId, request));
    }

    @PostMapping("/{classId}/students")
    public Result<TeachingView.ClassStudent> addStudent(
            @PathVariable Long classId,
            @Valid @RequestBody AddClassStudentRequest request) {
        return Result.success("学生已加入班级",
                commandService.addStudent(UserContext.requireUserId(), classId, request));
    }

    @DeleteMapping("/{classId}/students/{studentId}")
    public Result<Void> removeStudent(
            @PathVariable Long classId,
            @PathVariable Long studentId) {
        commandService.removeStudent(UserContext.requireUserId(), classId, studentId);
        return Result.success();
    }
}
