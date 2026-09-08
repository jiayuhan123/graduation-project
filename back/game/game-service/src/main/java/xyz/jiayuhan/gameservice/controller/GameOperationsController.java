package xyz.jiayuhan.gameservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;
import xyz.jiayuhan.gameservice.model.response.GameOperationsStatisticsResponse;
import xyz.jiayuhan.gameservice.service.GameOperationsService;

/** 管理员双人 PK 运营指标。 */
@RestController
@RequestMapping("/api/games/admin")
@RequiredArgsConstructor
public class GameOperationsController {

    private final GameOperationsService gameOperationsService;

    @GetMapping("/statistics")
    public Result<GameOperationsStatisticsResponse> statistics() {
        return Result.success(gameOperationsService.statistics(UserContext.requireUserId()));
    }
}
