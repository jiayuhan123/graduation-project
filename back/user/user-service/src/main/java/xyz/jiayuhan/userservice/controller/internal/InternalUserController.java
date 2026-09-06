package xyz.jiayuhan.userservice.controller.internal;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userapi.UserApiConstants;
import xyz.jiayuhan.userapi.model.request.BatchUserQueryRequest;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;
import xyz.jiayuhan.userservice.service.UserDirectoryService;

import java.util.List;

/** 仅供其他微服务通过服务发现直连的用户目录接口，不经公网网关暴露。 */
@Hidden
@Validated
@RestController
@RequestMapping(UserApiConstants.INTERNAL_USERS_PATH)
@RequiredArgsConstructor
public class InternalUserController {

    private final UserDirectoryService userDirectoryService;
    @GetMapping("/{userId}")
    public Result<UserSummaryResponse> summary(@PathVariable Long userId) {
        return Result.success(userDirectoryService.summary(userId));
    }

    @PostMapping("/batch")
    public Result<List<UserSummaryResponse>> summaries(
            @Valid @RequestBody BatchUserQueryRequest request) {
        return Result.success(userDirectoryService.summaries(request.userIds()));
    }

    @GetMapping("/{userId}/validate")
    public Result<UserValidationResponse> validate(
            @PathVariable Long userId,
            @RequestParam(required = false)
            @Pattern(regexp = "^[a-z][a-z0-9_-]{1,63}$", message = "角色编码格式不正确")
            String role) {
        return Result.success(userDirectoryService.validate(userId, role));
    }

    @GetMapping("/{userId}/authorize")
    public Result<UserPermissionResponse> authorize(
            @PathVariable Long userId,
            @RequestParam
            @Pattern(
                    regexp = "^[a-z][a-z0-9_-]*(?::[a-z][a-z0-9_-]*)+$",
                    message = "权限编码格式不正确")
            String permission) {
        return Result.success(userDirectoryService.authorize(userId, permission));
    }
}
