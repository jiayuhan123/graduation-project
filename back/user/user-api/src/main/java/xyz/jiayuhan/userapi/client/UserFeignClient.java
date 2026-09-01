package xyz.jiayuhan.userapi.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userapi.UserApiConstants;
import xyz.jiayuhan.userapi.model.request.BatchUserQueryRequest;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.util.List;

/** 通过 Nacos 服务发现直连用户服务的内部目录客户端。 */
@FeignClient(
        name = UserApiConstants.SERVICE_NAME,
        contextId = "userFeignClient",
        path = UserApiConstants.INTERNAL_USERS_PATH,
        configuration = UserFeignConfiguration.class
)
public interface UserFeignClient {

    @GetMapping("/{userId}")
    Result<UserSummaryResponse> getUserSummary(@PathVariable("userId") Long userId);

    @PostMapping("/batch")
    Result<List<UserSummaryResponse>> getUserSummaries(
            @Valid @RequestBody BatchUserQueryRequest request);

    @GetMapping("/{userId}/validate")
    Result<UserValidationResponse> validateUser(
            @PathVariable("userId") Long userId,
            @RequestParam(name = "role", required = false)
            @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$", message = "角色编码格式不正确")
            String role);
}
