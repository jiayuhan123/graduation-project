package xyz.jiayuhan.monomer.adapter;

import org.springframework.stereotype.Component;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.request.AssignmentProgressRequest;
import xyz.jiayuhan.teachingapi.model.response.AssignmentLearningResponse;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.teachingservice.controller.internal.InternalTeachingController;

/** 用进程内调用实现原教学服务契约。 */
@Component
public class LocalTeachingClient implements TeachingFeignClient {

    private final InternalTeachingController controller;

    public LocalTeachingClient(InternalTeachingController controller) {
        this.controller = controller;
    }

    @Override
    public Result<AssignmentSummaryResponse> getAssignment(Long assignmentId) {
        return controller.getAssignment(assignmentId);
    }

    @Override
    public Result<AssignmentLearningResponse> getStudentAssignment(Long assignmentId, Long studentId) {
        return controller.getStudentAssignment(assignmentId, studentId);
    }

    @Override
    public Result<Void> syncAssignmentProgress(
            Long assignmentId,
            Long studentId,
            AssignmentProgressRequest request) {
        return controller.syncAssignmentProgress(assignmentId, studentId, request);
    }

    @Override
    public Result<StudentAccessResponse> canViewStudent(Long teacherId, Long studentId) {
        return controller.canViewStudent(teacherId, studentId);
    }

    @Override
    public Result<ClassRosterResponse> getClassRoster(Long classId) {
        return controller.getClassRoster(classId);
    }

    @Override
    public Result<ClassAccessResponse> canViewClass(Long teacherId, Long classId) {
        return controller.canViewClass(teacherId, classId);
    }
}
