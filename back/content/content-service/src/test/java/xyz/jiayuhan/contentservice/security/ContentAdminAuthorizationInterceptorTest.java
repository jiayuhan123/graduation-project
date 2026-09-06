package xyz.jiayuhan.contentservice.security;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** 验证内容管理接口只接受用户服务确认的有效权限。 */
@ExtendWith(MockitoExtension.class)
class ContentAdminAuthorizationInterceptorTest {

    @Mock
    private UserFeignClient userFeignClient;

    @AfterEach
    void clearUserContext() {
        UserContext.clear();
    }

    @Test
    void allowsActiveUserWithContentManagePermission() {
        UserContext.setUserId(42L);
        when(userFeignClient.authorizeUser(42L, "content:manage"))
                .thenReturn(Result.success(new UserPermissionResponse(
                        42L, "content:manage", true, true, true)));
        ContentAdminAuthorizationInterceptor interceptor =
                new ContentAdminAuthorizationInterceptor(userFeignClient);

        assertThat(interceptor.preHandle(null, null, null)).isTrue();
    }

    @Test
    void rejectsUserWithoutPermission() {
        UserContext.setUserId(42L);
        when(userFeignClient.authorizeUser(42L, "content:manage"))
                .thenReturn(Result.success(new UserPermissionResponse(
                        42L, "content:manage", true, true, false)));
        ContentAdminAuthorizationInterceptor interceptor =
                new ContentAdminAuthorizationInterceptor(userFeignClient);

        assertThatThrownBy(() -> interceptor.preHandle(null, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.FORBIDDEN.getCode()));
    }

    @Test
    void reportsDependencyFailureWhenAuthorizationResponseIsUnavailable() {
        UserContext.setUserId(42L);
        when(userFeignClient.authorizeUser(42L, "content:manage")).thenReturn(null);
        ContentAdminAuthorizationInterceptor interceptor =
                new ContentAdminAuthorizationInterceptor(userFeignClient);

        assertThatThrownBy(() -> interceptor.preHandle(null, null, null))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.SERVICE_UNAVAILABLE.getCode()));
    }
}
