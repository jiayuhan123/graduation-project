package xyz.jiayuhan.userservice.service;

import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.util.List;

/** 供其他微服务进行跨库用户引用校验和展示信息解析。 */
public interface UserDirectoryService {

    UserSummaryResponse summary(Long userId);

    List<UserSummaryResponse> summaries(List<Long> userIds);

    UserValidationResponse validate(Long userId, String roleCode);
}
