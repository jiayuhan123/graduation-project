package xyz.jiayuhan.notificationservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.notificationservice.model.NotificationVO;
import xyz.jiayuhan.notificationservice.service.NotificationService;

import java.util.UUID;

/** 当前用户站内通知接口。 */
@Tag(name = "站内通知")
@Validated
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @Operation(summary = "分页查询当前用户通知")
    @GetMapping
    public Result<PageResult<NotificationVO>> page(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) long size,
            @RequestParam(required = false) Boolean read) {
        return Result.success(notificationService.page(UserContext.requireUserId(), page, size, read));
    }

    @Operation(summary = "查询未读通知数量")
    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.success(notificationService.unreadCount(UserContext.requireUserId()));
    }

    @Operation(summary = "标记一条通知为已读")
    @PutMapping("/{notificationId}/read")
    public Result<Void> markRead(@PathVariable UUID notificationId) {
        notificationService.markRead(UserContext.requireUserId(), notificationId);
        return Result.success();
    }

    @Operation(summary = "标记全部通知为已读")
    @PutMapping("/read-all")
    public Result<Void> markAllRead() {
        notificationService.markAllRead(UserContext.requireUserId());
        return Result.success();
    }
}
