package xyz.jiayuhan.fileservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.teachingapi.client.TeachingFeignClient;
import xyz.jiayuhan.teachingapi.model.response.StudentAccessResponse;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

/** 复核图片访问必须同时满足角色权限和真实师生关系。 */
@Service
@RequiredArgsConstructor
public class FileReviewAuthorizationService {

    private static final String REVIEW_PERMISSION = "learning:marking:review";

    private final UserFeignClient userFeignClient;
    private final TeachingFeignClient teachingFeignClient;

    public void authorize(Long requesterId, Long ownerId) {
        if (requesterId.equals(ownerId)) {
            return;
        }
        UserSummaryResponse requester = requireData(
                userFeignClient.getUserSummary(requesterId), "暂时无法读取当前用户资料");
        UserPermissionResponse permission = requireData(
                userFeignClient.authorizeUser(requesterId, REVIEW_PERMISSION),
                "暂时无法校验拍照复核权限");
        if (!permission.exists() || !permission.active() || !permission.granted()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        if (requester.roles() != null && requester.roles().contains("admin")) {
            return;
        }
        StudentAccessResponse access = requireData(
                teachingFeignClient.canViewStudent(requesterId, ownerId),
                "暂时无法校验师生关系");
        if (!access.accessible()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private <T> T requireData(Result<T> result, String message) {
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        return result.getData();
    }
}
