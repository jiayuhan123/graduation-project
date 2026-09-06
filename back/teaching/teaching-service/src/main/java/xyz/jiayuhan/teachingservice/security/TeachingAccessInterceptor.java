package xyz.jiayuhan.teachingservice.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

/** 按教学接口用途实时校验教师权限或学生身份。 */
@Component
public class TeachingAccessInterceptor implements HandlerInterceptor {

    public static final String CLASS_PERMISSION = "teaching:class:manage";
    public static final String ASSIGNMENT_PERMISSION = "teaching:assignment:manage";

    private final UserFeignClient userFeignClient;

    public TeachingAccessInterceptor(@Lazy UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler) {
        Long userId = UserContext.requireUserId();
        String uri = request.getRequestURI();
        if (isStudentPath(uri)) {
            requireStudent(userId);
        } else {
            requirePermission(userId, isClassPath(uri) ? CLASS_PERMISSION : ASSIGNMENT_PERMISSION);
        }
        return true;
    }

    private boolean isStudentPath(String uri) {
        return uri.contains("/student/")
                || uri.endsWith("/classes/enrolled")
                || uri.endsWith("/submissions/mine");
    }

    private boolean isClassPath(String uri) {
        return uri.contains("/teacher/classes") || uri.endsWith("/classes/teaching");
    }

    private void requireStudent(Long userId) {
        Result<UserValidationResponse> result = userFeignClient.validateUser(userId, "student");
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验学生身份");
        }
        UserValidationResponse validation = result.getData();
        if (!validation.exists() || !validation.active() || !validation.roleMatched()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private void requirePermission(Long userId, String permission) {
        Result<UserPermissionResponse> result = userFeignClient.authorizeUser(userId, permission);
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验教学权限");
        }
        UserPermissionResponse authorization = result.getData();
        if (!authorization.exists() || !authorization.active() || !authorization.granted()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
