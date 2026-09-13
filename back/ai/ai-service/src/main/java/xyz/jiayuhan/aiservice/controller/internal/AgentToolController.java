package xyz.jiayuhan.aiservice.controller.internal;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import xyz.jiayuhan.aiapi.AiApiConstants;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiservice.service.AgentToolService;
import xyz.jiayuhan.common.core.result.Result;

/** 仅供受信任 FastAPI 服务回调的白名单工具入口。 */
@Hidden
@RestController
@RequestMapping(AiApiConstants.INTERNAL_TOOLS_PATH)
public class AgentToolController {

    private final AgentToolService toolService;

    public AgentToolController(AgentToolService toolService) {
        this.toolService = toolService;
    }

    @PostMapping("/invoke")
    public Result<AgentContracts.ToolResult> invoke(
            @RequestHeader(AiApiConstants.TOOL_TOKEN_HEADER) String toolToken,
            @Valid @RequestBody AgentContracts.ToolInvocationRequest request) {
        return Result.success(toolService.invoke(toolToken, request));
    }
}
