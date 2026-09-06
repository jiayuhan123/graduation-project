package xyz.jiayuhan.teachingservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.teachingservice.entity.Assignment;
import xyz.jiayuhan.teachingservice.entity.AssignmentQuestion;
import xyz.jiayuhan.teachingservice.entity.AssignmentSubmission;
import xyz.jiayuhan.teachingservice.entity.ClassStudent;
import xyz.jiayuhan.teachingservice.entity.ClassTeacher;
import xyz.jiayuhan.teachingservice.entity.TeachingClass;
import xyz.jiayuhan.teachingservice.event.TeachingAuditActions;
import xyz.jiayuhan.teachingservice.event.TeachingEventOutboxService;
import xyz.jiayuhan.teachingservice.mapper.AssignmentMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentQuestionMapper;
import xyz.jiayuhan.teachingservice.mapper.AssignmentSubmissionMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassStudentMapper;
import xyz.jiayuhan.teachingservice.mapper.ClassTeacherMapper;
import xyz.jiayuhan.teachingservice.mapper.TeachingClassMapper;
import xyz.jiayuhan.teachingservice.model.request.AssignmentQuestionRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.CreateTeachingClassRequest;
import xyz.jiayuhan.teachingservice.model.request.PublishAssignmentRequest;
import xyz.jiayuhan.teachingservice.model.request.StudentLayerAssignmentRequest;
import xyz.jiayuhan.userapi.client.UserFeignClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证班级创建和分层作业核心不变量。 */
@ExtendWith(MockitoExtension.class)
class TeachingCommandServiceImplTest {

    @Mock
    private TeachingClassMapper teachingClassMapper;
    @Mock
    private ClassTeacherMapper classTeacherMapper;
    @Mock
    private ClassStudentMapper classStudentMapper;
    @Mock
    private AssignmentMapper assignmentMapper;
    @Mock
    private AssignmentQuestionMapper assignmentQuestionMapper;
    @Mock
    private AssignmentSubmissionMapper assignmentSubmissionMapper;
    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private ContentFeignClient contentFeignClient;
    @Mock
    private TeachingEventOutboxService outboxService;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private TeachingCommandServiceImpl commandService;

    @Test
    void createsClassWithOwnerMembershipAndGeneratedCode() {
        doAnswer(invocation -> {
            TeachingClass entity = invocation.getArgument(0);
            entity.setId(100L);
            return 1;
        }).when(teachingClassMapper).insert(any(TeachingClass.class));

        var result = commandService.createClass(42L, new CreateTeachingClassRequest(
                "一年级二班", "示范小学", "2026-2027", 1, 1));

        assertThat(result.classId()).isEqualTo(100L);
        assertThat(result.classCode()).hasSize(8);
        assertThat(result.membershipRole()).isEqualTo("OWNER");
        verify(classTeacherMapper).insert(any(ClassTeacher.class));
        verify(outboxService).appendAudit(
                42L, TeachingAuditActions.CLASS_CREATED, "TEACHING_CLASS", 100L,
                "classCode=" + result.classCode());
    }

    @Test
    void rejectsLayeredAssignmentWithOnlyOneLayerBeforeReadingContent() {
        TeachingClass teachingClass = new TeachingClass();
        teachingClass.setId(100L);
        teachingClass.setStatus("ACTIVE");
        ClassTeacher membership = new ClassTeacher();
        membership.setClassId(100L);
        membership.setTeacherId(42L);
        membership.setTeacherRole("OWNER");
        when(teachingClassMapper.selectById(100L)).thenReturn(teachingClass);
        when(classTeacherMapper.selectOne(any())).thenReturn(membership);
        OffsetDateTime startAt = OffsetDateTime.now(ZoneOffset.UTC);
        CreateAssignmentRequest request = new CreateAssignmentRequest(
                100L,
                "分层口算",
                null,
                "LAYERED",
                startAt,
                startAt.plusDays(1),
                null,
                List.of(new AssignmentQuestionRequest(
                        200L, "A", 1, BigDecimal.TEN)));

        assertThatThrownBy(() -> commandService.createAssignment(42L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.ASSIGNMENT_CONTENT_INVALID.getCode()));
    }

    @Test
    void requiresLayerAssignmentForEveryActiveStudentWhenPublishing() {
        TeachingClass teachingClass = new TeachingClass();
        teachingClass.setId(100L);
        teachingClass.setStatus("ACTIVE");
        ClassTeacher membership = new ClassTeacher();
        membership.setClassId(100L);
        membership.setTeacherId(42L);
        Assignment assignment = new Assignment();
        assignment.setId(300L);
        assignment.setClassId(100L);
        assignment.setTeacherId(42L);
        assignment.setAssignmentType("LAYERED");
        assignment.setStatus("DRAFT");
        assignment.setVersion(0);
        AssignmentQuestion layerA = assignmentQuestion(301L, 201L, "A");
        AssignmentQuestion layerB = assignmentQuestion(302L, 202L, "B");
        ClassStudent firstStudent = activeStudent(501L);
        ClassStudent secondStudent = activeStudent(502L);
        when(assignmentMapper.selectById(300L)).thenReturn(assignment);
        when(teachingClassMapper.selectById(100L)).thenReturn(teachingClass);
        when(classTeacherMapper.selectOne(any())).thenReturn(membership);
        when(assignmentQuestionMapper.selectList(any())).thenReturn(List.of(layerA, layerB));
        when(contentFeignClient.getQuestions(any())).thenReturn(Result.success(List.of(
                question(201L, "Q-A"), question(202L, "Q-B"))));
        when(objectMapper.writeValueAsString(any(QuestionResponse.class))).thenReturn("{}");
        when(classStudentMapper.selectList(any())).thenReturn(List.of(firstStudent, secondStudent));
        PublishAssignmentRequest request = new PublishAssignmentRequest(
                0, List.of(new StudentLayerAssignmentRequest(501L, "A")));

        assertThatThrownBy(() -> commandService.publishAssignment(42L, 300L, request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.ASSIGNMENT_CONTENT_INVALID.getCode()));
        verify(assignmentSubmissionMapper, never()).insert(any(AssignmentSubmission.class));
    }

    private AssignmentQuestion assignmentQuestion(Long id, Long questionId, String layerCode) {
        AssignmentQuestion question = new AssignmentQuestion();
        question.setId(id);
        question.setAssignmentId(300L);
        question.setQuestionId(questionId);
        question.setLayerCode(layerCode);
        question.setSequenceNo(1);
        question.setScore(BigDecimal.TEN);
        question.setQuestionSnapshot("{}");
        return question;
    }

    private ClassStudent activeStudent(Long studentId) {
        ClassStudent student = new ClassStudent();
        student.setClassId(100L);
        student.setStudentId(studentId);
        student.setStatus("ACTIVE");
        return student;
    }

    private QuestionResponse question(Long id, String code) {
        return new QuestionResponse(
                id, code, "FIXED", "ARITHMETIC", "1 + 1 = ?", null,
                "2", null, null, 1, 20, "ENABLED");
    }
}
