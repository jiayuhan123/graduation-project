package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.util.List;

/** 供其他领域模块进行用户引用校验和展示信息解析。 */
public interface UserDirectoryService {

    UserSummaryResponse summary(Long userId);

    List<UserSummaryResponse> summaries(List<Long> userIds);

    UserValidationResponse validate(Long userId, String roleCode);

    UserPermissionResponse authorize(Long userId, String permissionCode);
}
