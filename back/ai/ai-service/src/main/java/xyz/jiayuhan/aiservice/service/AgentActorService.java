package xyz.jiayuhan.aiservice.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserPermissionResponse;
import xyz.jiayuhan.userapi.model.response.UserSummaryResponse;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 从服务端用户目录构造 Agent 身份，不信任前端提交的角色和工具范围。 */
@Service
public class AgentActorService {

    private static final String REPORT_PERMISSION = "teaching:report:query";
    private static final String ASSIGNMENT_PERMISSION = "teaching:assignment:manage";

    private final UserFeignClient userFeignClient;

    public AgentActorService(UserFeignClient userFeignClient) {
        this.userFeignClient = userFeignClient;
    }

    public AgentContracts.Actor load(Long userId, String activeRole) {
        Result<UserSummaryResponse> result = userFeignClient.getUserSummary(userId);
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法读取 Agent 用户身份");
        }
        UserSummaryResponse user = result.getData();
        if (!Integer.valueOf(1).equals(user.status())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前账号不可用");
        }
        List<String> roles = user.roles() == null ? List.of() : user.roles();
        if (!roles.contains(activeRole)) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前角色与登录身份不匹配");
        }
        Set<String> allowedTools = allowedTools(userId, activeRole);
        if (allowedTools.isEmpty()) {
            throw new BusinessException(ResultCode.FORBIDDEN, "当前角色暂不支持教学 Agent");
        }
        return new AgentContracts.Actor(
                user.id(), displayName(user), activeRole, roles,
                List.copyOf(new LinkedHashSet<>(allowedTools)));
    }

    public void requirePermission(Long userId, String permissionCode) {
        UserPermissionResponse permission = permission(userId, permissionCode);
        if (!permission.exists() || !permission.active() || !permission.granted()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
    }

    private Set<String> allowedTools(Long userId, String activeRole) {
        Set<String> tools = new LinkedHashSet<>();
        boolean reportGranted = granted(userId, REPORT_PERMISSION);
        if (reportGranted) {
            tools.add("get_class_dashboard");
        }
        if ("teacher".equals(activeRole) && granted(userId, ASSIGNMENT_PERMISSION)) {
            tools.add("search_questions");
            tools.add("create_assignment_draft");
        } else if ("admin".equals(activeRole) && reportGranted) {
            tools.add("search_questions");
        }
        return tools;
    }

    private boolean granted(Long userId, String permissionCode) {
        UserPermissionResponse permission = permission(userId, permissionCode);
        return permission.exists() && permission.active() && permission.granted();
    }

    private UserPermissionResponse permission(Long userId, String permissionCode) {
        Result<UserPermissionResponse> result = userFeignClient.authorizeUser(userId, permissionCode);
        if (result == null || !Integer.valueOf(200).equals(result.getCode()) || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "暂时无法校验 Agent 操作权限");
        }
        return result.getData();
    }

    private String displayName(UserSummaryResponse user) {
        if (StringUtils.hasText(user.realName())) {
            return user.realName();
        }
        if (StringUtils.hasText(user.nickname())) {
            return user.nickname();
        }
        return user.username();
    }
}
