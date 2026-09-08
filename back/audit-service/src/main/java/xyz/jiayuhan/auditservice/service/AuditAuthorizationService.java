package xyz.jiayuhan.auditservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

/** 全局审计只允许具有审计权限的有效管理员访问。 */
@Service
@RequiredArgsConstructor
public class AuditAuthorizationService {

    private final UserFeignClient userFeignClient;

    public void requireAdmin(Long requesterId) {
        Result<UserSummaryResponse> user;
        Result<UserPermissionResponse> permission;
        try {
            user = userFeignClient.getUserSummary(requesterId);
            permission = userFeignClient.authorizeUser(requesterId, "audit:query");
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验审计查询权限");
        }
        if (user == null || permission == null
                || !Integer.valueOf(200).equals(user.getCode())
                || !Integer.valueOf(200).equals(permission.getCode())
                || user.getData() == null || permission.getData() == null
                || user.getData().roles() == null || !user.getData().roles().contains("admin")
                || !Integer.valueOf(1).equals(user.getData().status())
                || !permission.getData().exists() || !permission.getData().active()
                || !permission.getData().granted()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }
}
