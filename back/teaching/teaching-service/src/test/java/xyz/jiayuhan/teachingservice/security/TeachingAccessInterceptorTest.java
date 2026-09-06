package xyz.jiayuhan.teachingservice.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 验证教学接口按学生身份、班级权限和作业权限分别鉴权。 */
@ExtendWith(MockitoExtension.class)
class TeachingAccessInterceptorTest {

    @Mock
    private UserFeignClient userFeignClient;
    @Mock
    private HttpServletRequest request;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void validatesStudentRoleForStudentEndpoints() {
        UserContext.setUserId(42L);
        when(request.getRequestURI()).thenReturn("/api/teaching/student/classes");
        when(userFeignClient.validateUser(42L, "student"))
                .thenReturn(Result.success(new UserValidationResponse(42L, true, true, true)));

        boolean allowed = new TeachingAccessInterceptor(userFeignClient)
                .preHandle(request, null, null);

        assertThat(allowed).isTrue();
        verify(userFeignClient).validateUser(42L, "student");
    }

    @Test
    void usesClassPermissionForTeacherClassEndpoints() {
        UserContext.setUserId(42L);
        when(request.getRequestURI()).thenReturn("/api/teaching/teacher/classes/100");
        when(userFeignClient.authorizeUser(42L, TeachingAccessInterceptor.CLASS_PERMISSION))
                .thenReturn(Result.success(new UserPermissionResponse(
                        42L, TeachingAccessInterceptor.CLASS_PERMISSION, true, true, true)));

        boolean allowed = new TeachingAccessInterceptor(userFeignClient)
                .preHandle(request, null, null);

        assertThat(allowed).isTrue();
        verify(userFeignClient).authorizeUser(42L, TeachingAccessInterceptor.CLASS_PERMISSION);
    }

    @Test
    void rejectsTeacherWithoutAssignmentPermission() {
        UserContext.setUserId(42L);
        when(request.getRequestURI()).thenReturn("/api/teaching/teacher/assignments");
        when(userFeignClient.authorizeUser(42L, TeachingAccessInterceptor.ASSIGNMENT_PERMISSION))
                .thenReturn(Result.success(new UserPermissionResponse(
                        42L, TeachingAccessInterceptor.ASSIGNMENT_PERMISSION, true, true, false)));
        TeachingAccessInterceptor interceptor = new TeachingAccessInterceptor(userFeignClient);

        assertThatThrownBy(() -> interceptor.preHandle(request, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }
}
