package xyz.jiayuhan.gameservice.controller.internal;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.gameapi.GameApiConstants;
import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;
import xyz.jiayuhan.gameservice.service.GameQueryService;

/** 仅供学习服务调用的游戏对局查询契约。 */
@RestController
@RequestMapping(GameApiConstants.INTERNAL_PATH)
@RequiredArgsConstructor
public class InternalGameController {

    private final GameQueryService gameQueryService;

    @GetMapping("/sessions/{sessionId}")
    public Result<GameSessionSummaryResponse> getSession(@PathVariable Long sessionId) {
        return Result.success(gameQueryService.getSessionInternal(sessionId));
    }
}
