package xyz.jiayuhan.teachingservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.teachingservice.model.request.AddClassStudentRequest;
import xyz.jiayuhan.teachingservice.model.request.ChangeAssignmentStatusRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.JoinClassRequest;
import xyz.jiayuhan.teachingservice.model.request.PublishAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateSubmissionCommentRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.UpdateTeachingClassStatusRequest;
import xyz.jiayuhan.teachingservice.model.response.TeachingView;
import xyz.jiayuhan.teachingapi.model.request.AssignmentProgressRequest;

import java.util.List;

/** 教学班、成员和作业命令应用服务。 */
public interface TeachingCommandService {

    TeachingView.ClassMembership createClass(Long teacherId, CreateTeachingClassRequest request);

    TeachingView.ClassMembership updateClass(
            Long teacherId, Long classId, UpdateTeachingClassRequest request);

    TeachingView.ClassMembership updateClassStatus(
            Long teacherId, Long classId, UpdateTeachingClassStatusRequest request);

    TeachingView.ClassDetail getClassDetail(Long teacherId, Long classId);

    TeachingView.ClassStudent addStudent(
            Long teacherId, Long classId, AddClassStudentRequest request);

    void removeStudent(Long teacherId, Long classId, Long studentId);

    TeachingView.ClassMembership joinClass(Long studentId, JoinClassRequest request);

    void leaveClass(Long studentId, Long classId);

    PageResult<TeachingView.QuestionCandidate> pageQuestionCandidates(
            long page,
            long size,
            String keyword,
            Long knowledgePointId,
            String questionType,
            Integer difficulty);

    TeachingView.AssignmentDetail createAssignment(Long teacherId, CreateAssignmentRequest request);

    TeachingView.AssignmentDetail updateAssignment(
            Long teacherId, Long assignmentId, UpdateAssignmentRequest request);

    TeachingView.AssignmentDetail publishAssignment(
            Long teacherId, Long assignmentId, PublishAssignmentRequest request);

    TeachingView.AssignmentDetail changeAssignmentStatus(
            Long teacherId, Long assignmentId, ChangeAssignmentStatusRequest request);

    List<TeachingView.SubmissionDetail> listAssignmentSubmissions(
            Long teacherId, Long assignmentId, String status);

    TeachingView.SubmissionDetail updateSubmissionComment(
            Long teacherId, Long submissionId, UpdateSubmissionCommentRequest request);

    void syncAssignmentProgress(
            Long assignmentId, Long studentId, AssignmentProgressRequest request);
}
