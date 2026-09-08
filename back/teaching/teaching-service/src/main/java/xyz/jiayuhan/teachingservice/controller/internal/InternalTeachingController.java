package xyz.jiayuhan.teachingservice.controller.internal;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.TeachingApiConstants;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingapi.model.request.AssignmentProgressRequest;
import xyz.jiayuhan.teachingapi.model.response.AssignmentLearningResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.teachingservice.service.TeachingCommandService;
import xyz.jiayuhan.teachingservice.service.TeachingQueryService;

/** 仅供学习服务调用的作业查询契约。 */
@RestController
@RequestMapping(TeachingApiConstants.INTERNAL_PATH)
@RequiredArgsConstructor
public class InternalTeachingController {

    private final TeachingQueryService teachingQueryService;
    private final TeachingCommandService teachingCommandService;

    @GetMapping("/assignments/{assignmentId}")
    public Result<AssignmentSummaryResponse> getAssignment(@PathVariable Long assignmentId) {
        return Result.success(teachingQueryService.getAssignmentInternal(assignmentId));
    }

    @GetMapping("/assignments/{assignmentId}/students/{studentId}")
    public Result<AssignmentLearningResponse> getStudentAssignment(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId) {
        return Result.success(teachingQueryService.getStudentAssignmentInternal(
                assignmentId, studentId));
    }

    @PutMapping("/assignments/{assignmentId}/students/{studentId}/progress")
    public Result<Void> syncAssignmentProgress(
            @PathVariable Long assignmentId,
            @PathVariable Long studentId,
            @Valid @RequestBody AssignmentProgressRequest request) {
        teachingCommandService.syncAssignmentProgress(assignmentId, studentId, request);
        return Result.success();
    }

    @GetMapping("/teachers/{teacherId}/students/{studentId}/access")
    public Result<StudentAccessResponse> canViewStudent(
            @PathVariable Long teacherId,
            @PathVariable Long studentId) {
        return Result.success(teachingQueryService.canTeacherViewStudent(teacherId, studentId));
    }

    @GetMapping("/classes/{classId}/roster")
    public Result<ClassRosterResponse> getClassRoster(@PathVariable Long classId) {
        return Result.success(teachingQueryService.getClassRosterInternal(classId));
    }

    @GetMapping("/teachers/{teacherId}/classes/{classId}/access")
    public Result<ClassAccessResponse> canViewClass(
            @PathVariable Long teacherId,
            @PathVariable Long classId) {
        return Result.success(teachingQueryService.canTeacherViewClass(teacherId, classId));
    }
}
