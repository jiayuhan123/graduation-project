package xyz.jiayuhan.monomer.adapter;

import org.springframework.stereotype.Component;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.request.BatchUserQueryRequest;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;
import xyz.jiayuhan.userservice.controller.internal.InternalUserController;

import java.util.List;

/** 用进程内调用实现原用户服务契约，替代 Nacos、LoadBalancer 和 HTTP 序列化。 */
@Component
public class LocalUserClient implements UserFeignClient {

    private final InternalUserController controller;

    public LocalUserClient(InternalUserController controller) {
        this.controller = controller;
    }

    @Override
    public Result<UserSummaryResponse> getUserSummary(Long userId) {
        return controller.summary(userId);
    }

    @Override
    public Result<List<UserSummaryResponse>> getUserSummaries(BatchUserQueryRequest request) {
        return controller.summaries(request);
    }

    @Override
    public Result<UserValidationResponse> validateUser(Long userId, String role) {
        return controller.validate(userId, role);
    }

    @Override
    public Result<UserPermissionResponse> authorizeUser(Long userId, String permission) {
        return controller.authorize(userId, permission);
    }
}
