package xyz.jiayuhan.auditservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.auditservice.model.AuditLogVO;
import xyz.jiayuhan.auditservice.service.AuditLogService;
import xyz.jiayuhan.auditservice.service.AuditAuthorizationService;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;

import java.time.OffsetDateTime;

/** 当前用户个人操作审计查询接口；全局管理审计需在具体毕设业务中另设管理员权限点。 */
@Tag(name = "操作审计")
@Validated
@RestController
@RequestMapping("/api/audits")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final AuditAuthorizationService authorizationService;

    @Operation(summary = "分页查询当前用户的操作记录")
    @GetMapping("/me")
    public Result<PageResult<AuditLogVO>> myAuditLogs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String action) {
        return Result.success(auditLogService.pageByOperator(
                UserContext.requireUserId(), page, size, action));
    }

    @Operation(summary = "管理员分页查询全局操作审计")
    @GetMapping("/admin")
    public Result<PageResult<AuditLogVO>> adminAuditLogs(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String targetType,
            @RequestParam(required = false) String targetId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime occurredFrom,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime occurredTo) {
        Long requesterId = UserContext.requireUserId();
        authorizationService.requireAdmin(requesterId);
        return Result.success(auditLogService.page(
                page, size, operatorId, source, action, targetType, targetId,
                occurredFrom, occurredTo));
    }
}
