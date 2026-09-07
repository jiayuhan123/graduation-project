package xyz.jiayuhan.learningservice.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningReportServiceImplTest {

    @Mock private LearningQueryService learningQueryService;
    @Mock private UserFeignClient userFeignClient;
    @Mock private TeachingFeignClient teachingFeignClient;
    @InjectMocks private LearningReportServiceImpl service;

    @Test
    void rejectsTeacherWithoutActiveClassRelationship() {
        long teacherId = 21L;
        long studentId = 31L;
        when(userFeignClient.getUserSummary(studentId)).thenReturn(Result.success(
                new UserSummaryResponse(
                        studentId, "student", "学生", null, null, 1,
                        List.of("student"), 4, 1)));
        when(userFeignClient.getUserSummary(teacherId)).thenReturn(Result.success(
                new UserSummaryResponse(
                        teacherId, "teacher", "教师", null, null, 1,
                        List.of("teacher"), null, null)));
        when(userFeignClient.authorizeUser(teacherId, "teaching:report:query"))
                .thenReturn(Result.success(new UserPermissionResponse(
                        teacherId, "teaching:report:query", true, true, true)));
        when(teachingFeignClient.canViewStudent(teacherId, studentId))
                .thenReturn(Result.success(new StudentAccessResponse(teacherId, studentId, false)));

        assertThatThrownBy(() -> service.getStudentReport(teacherId, studentId))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        org.assertj.core.api.Assertions.assertThat(exception.getCode())
                                .isEqualTo(ResultCode.LEARNING_REPORT_FORBIDDEN.getCode()));
    }
}
