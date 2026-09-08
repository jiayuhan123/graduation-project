package xyz.jiayuhan.teachingservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.teachingapi.model.response.AssignmentSummaryResponse;
import xyz.jiayuhan.teachingapi.model.response.AssignmentLearningResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassAccessResponse;
import xyz.jiayuhan.teachingapi.model.response.ClassRosterResponse;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingservice.model.response.TeachingOperationsStatisticsResponse;

/** 班级、作业和提交记录的只读应用服务。 */
public interface TeachingQueryService {

    PageResult<TeachingView.ClassMembership> pageTeachingClasses(
            Long teacherId, long page, long size);

    PageResult<TeachingView.ClassMembership> pageAllClasses(
            long page, long size, String status);

    PageResult<TeachingView.ClassMembership> pageEnrolledClasses(
            Long studentId, long page, long size, String status);

    PageResult<AssignmentSummaryResponse> pageCreatedAssignments(
            Long teacherId, long page, long size, String status);

    PageResult<TeachingView.Submission> pageMySubmissions(
            Long studentId, long page, long size, String status);

    PageResult<TeachingView.StudentAssignment> pageStudentAssignments(
            Long studentId, long page, long size, String status);

    TeachingView.AssignmentDetail getAssignmentForUser(Long userId, Long assignmentId);

    AssignmentSummaryResponse getAssignmentInternal(Long assignmentId);

    AssignmentLearningResponse getStudentAssignmentInternal(Long assignmentId, Long studentId);

    StudentAccessResponse canTeacherViewStudent(Long teacherId, Long studentId);

    ClassRosterResponse getClassRosterInternal(Long classId);

    ClassAccessResponse canTeacherViewClass(Long teacherId, Long classId);

    TeachingOperationsStatisticsResponse operationsStatistics();
}
