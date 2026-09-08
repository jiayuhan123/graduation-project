package xyz.jiayuhan.gameservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.gameservice.model.request.CreatePkSessionRequest;
import xyz.jiayuhan.gameservice.model.request.SubmitGameAnswerRequest;
import xyz.jiayuhan.gameservice.model.response.GameCommandView;
import xyz.jiayuhan.gameservice.model.response.GameView;
import xyz.jiayuhan.gameservice.service.GameCommandService;

/** 学生创建、匹配、推进和结算游戏对局的命令接口。 */
@Tag(name = "游戏闯关与PK")
@Validated
@RestController
@RequestMapping("/api/games")
@RequiredArgsConstructor
public class GameCommandController {

    private final GameCommandService commandService;

    @Operation(summary = "创建单人闯关对局")
    @PostMapping("/levels/{levelId}/sessions")
    public Result<GameView.SessionDetail> createLevelSession(@PathVariable Long levelId) {
        return Result.success(commandService.createLevelSession(UserContext.requireUserId(), levelId));
    }

    @Operation(summary = "创建双人PK房间")
    @PostMapping("/pk/rooms")
    public Result<GameView.SessionDetail> createPkRoom(
            @Valid @RequestBody CreatePkSessionRequest request) {
        return Result.success(commandService.createPkRoom(UserContext.requireUserId(), request));
    }

    @Operation(summary = "通过邀请码加入双人PK房间")
    @PostMapping("/pk/rooms/{inviteCode}/join")
    public Result<GameView.SessionDetail> joinPkRoom(
            @PathVariable
            @Pattern(regexp = "^[A-Za-z0-9]{6,12}$", message = "邀请码格式不正确")
            String inviteCode) {
        return Result.success(commandService.joinPkRoom(UserContext.requireUserId(), inviteCode));
    }

    @Operation(summary = "发起或查询随机PK匹配")
    @PostMapping("/pk/matches")
    public Result<GameCommandView.MatchStatus> requestRandomMatch(
            @Valid @RequestBody CreatePkSessionRequest request) {
        return Result.success(commandService.requestRandomMatch(UserContext.requireUserId(), request));
    }

    @Operation(summary = "取消等待中的随机PK匹配")
    @DeleteMapping("/pk/matches/{ticketId}")
    public Result<Void> cancelRandomMatch(@PathVariable Long ticketId) {
        commandService.cancelRandomMatch(UserContext.requireUserId(), ticketId);
        return Result.success();
    }

    @Operation(summary = "开始已经准备完成的对局")
    @PostMapping("/sessions/{sessionId}/start")
    public Result<GameView.SessionDetail> startSession(@PathVariable Long sessionId) {
        return Result.success(commandService.startSession(UserContext.requireUserId(), sessionId));
    }

    @Operation(summary = "提交一道游戏题答案")
    @PostMapping("/sessions/{sessionId}/questions/{questionId}/answers")
    public Result<GameCommandView.AnswerFeedback> submitAnswer(
            @PathVariable Long sessionId,
            @PathVariable Long questionId,
            @Valid @RequestBody SubmitGameAnswerRequest request) {
        return Result.success(commandService.submitAnswer(
                UserContext.requireUserId(), sessionId, questionId, request));
    }

    @Operation(summary = "尝试完成并结算对局")
    @PostMapping("/sessions/{sessionId}/finish")
    public Result<GameView.SessionDetail> finishSession(@PathVariable Long sessionId) {
        return Result.success(commandService.finishSession(UserContext.requireUserId(), sessionId));
    }

    @Operation(summary = "取消尚未开始的对局")
    @DeleteMapping("/sessions/{sessionId}")
    public Result<GameView.SessionDetail> cancelSession(@PathVariable Long sessionId) {
        return Result.success(commandService.cancelSession(UserContext.requireUserId(), sessionId));
    }
}
