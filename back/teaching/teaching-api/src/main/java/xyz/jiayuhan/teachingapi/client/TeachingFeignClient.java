package xyz.jiayuhan.teachingapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.TeachingApiConstants;
import xyz.jiayuhan.teachingapi.model.request.AssignmentProgressRequest;
import xyz.jiayuhan.teachingapi.model.response.AssignmentLearningResponse;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;

/** 教学服务内部只读客户端。 */
@FeignClient(
        name = TeachingApiConstants.SERVICE_NAME,
        contextId = "teachingFeignClient",
        path = TeachingApiConstants.INTERNAL_PATH,
        configuration = TeachingFeignConfiguration.class
)
public interface TeachingFeignClient {

    @GetMapping("/assignments/{assignmentId}")
    Result<AssignmentSummaryResponse> getAssignment(@PathVariable("assignmentId") Long assignmentId);

    @GetMapping("/assignments/{assignmentId}/students/{studentId}")
    Result<AssignmentLearningResponse> getStudentAssignment(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("studentId") Long studentId);

    @PutMapping("/assignments/{assignmentId}/students/{studentId}/progress")
    Result<Void> syncAssignmentProgress(
            @PathVariable("assignmentId") Long assignmentId,
            @PathVariable("studentId") Long studentId,
            @RequestBody AssignmentProgressRequest request);

    @GetMapping("/teachers/{teacherId}/students/{studentId}/access")
    Result<StudentAccessResponse> canViewStudent(
            @PathVariable("teacherId") Long teacherId,
            @PathVariable("studentId") Long studentId);

    @GetMapping("/classes/{classId}/roster")
    Result<ClassRosterResponse> getClassRoster(@PathVariable("classId") Long classId);

    @GetMapping("/teachers/{teacherId}/classes/{classId}/access")
    Result<ClassAccessResponse> canViewClass(
            @PathVariable("teacherId") Long teacherId,
            @PathVariable("classId") Long classId);
}
