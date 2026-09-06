package xyz.jiayuhan.gameapi.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.gameapi.GameApiConstants;
import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;

/** 游戏服务内部只读客户端。 */
@FeignClient(
        name = GameApiConstants.SERVICE_NAME,
        contextId = "gameFeignClient",
        path = GameApiConstants.INTERNAL_PATH,
        configuration = GameFeignConfiguration.class
)
public interface GameFeignClient {

    @GetMapping("/sessions/{sessionId}")
    Result<GameSessionSummaryResponse> getSession(@PathVariable("sessionId") Long sessionId);
}
