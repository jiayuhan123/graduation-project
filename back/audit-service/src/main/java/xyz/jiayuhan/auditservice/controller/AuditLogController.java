package xyz.jiayuhan.auditservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.auditservice.model.AuditLogVO;
import xyz.jiayuhan.auditservice.service.AuditLogService;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;

/** 当前用户个人操作审计查询接口；全局管理审计需在具体毕设业务中另设管理员权限点。 */
@Tag(name = "操作审计")
@Validated
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    @Operation(summary = "分页查询当前用户的操作记录")
    @GetMapping("/me")
    public Result<PageResult<AuditLogVO>> myAuditLogs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String action) {
        return Result.success(auditLogService.pageByOperator(
                UserContext.requireUserId(), page, size, action));
    }
}
