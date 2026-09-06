package xyz.jiayuhan.gameservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;
import xyz.jiayuhan.gameservice.model.response.GameView;
import xyz.jiayuhan.gameservice.service.GameQueryService;

import java.util.List;

/** 当前学生可见的关卡、对局、进度和奖励接口。 */
@Tag(name = "游戏闯关查询")
@Validated
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameQueryController {

    private final GameQueryService gameQueryService;

    @Operation(summary = "分页查询已启用关卡")
    @GetMapping("/levels")
    public Result<PageResult<GameView.Level>> pageLevels(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) @Min(1) @Max(6) Integer gradeLevel,
            @RequestParam(required = false) Long knowledgePointId,
            @RequestParam(required = false) String mode) {
        return Result.success(gameQueryService.pageLevels(
                page, size, gradeLevel, knowledgePointId, mode));
    }

    @Operation(summary = "查询已启用关卡详情")
    @GetMapping("/levels/{levelId}")
    public Result<GameView.Level> getLevel(@PathVariable Long levelId) {
        return Result.success(gameQueryService.getLevel(levelId));
    }

    @Operation(summary = "分页查询我的闯关记录")
    @GetMapping("/sessions")
    public Result<PageResult<GameSessionSummaryResponse>> pageMySessions(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size,
            @RequestParam(required = false) String status) {
        return Result.success(gameQueryService.pageMySessions(
                UserContext.requireUserId(), page, size, status));
    }

    @Operation(summary = "查询我参与的对局详情")
    @GetMapping("/sessions/{sessionId}")
    public Result<GameView.SessionDetail> getSession(@PathVariable Long sessionId) {
        return Result.success(gameQueryService.getSession(UserContext.requireUserId(), sessionId));
    }

    @Operation(summary = "查询我的玩家总进度")
    @GetMapping("/progress")
    public Result<GameView.PlayerProgress> getPlayerProgress() {
        return Result.success(gameQueryService.getPlayerProgress(UserContext.requireUserId()));
    }

    @Operation(summary = "查询我的关卡进度")
    @GetMapping("/level-progress")
    public Result<List<GameView.LevelProgress>> listLevelProgress(
            @RequestParam(required = false) String status) {
        return Result.success(gameQueryService.listLevelProgress(
                UserContext.requireUserId(), status));
    }

    @Operation(summary = "分页查询我的奖励流水")
    @GetMapping("/rewards")
    public Result<PageResult<GameView.Reward>> pageRewards(
            @RequestParam(defaultValue = "1") @Min(1) long page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) long size) {
        return Result.success(gameQueryService.pageRewards(
                UserContext.requireUserId(), page, size));
    }
}
