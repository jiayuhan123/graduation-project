package xyz.jiayuhan.aiservice.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiservice.config.AiProperties;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 准备可信运行上下文并管理 Java 到 Python 的流式生命周期。 */
@Service
public class AgentGatewayService {

    private final AiProperties properties;
    private final AgentActorService actorService;
    private final AgentTokenService tokenService;
    private final PythonAgentClient pythonAgentClient;
    private final ObjectMapper objectMapper;

    public AgentGatewayService(
            AiProperties properties,
            AgentActorService actorService,
            AgentTokenService tokenService,
            PythonAgentClient pythonAgentClient,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.actorService = actorService;
        this.tokenService = tokenService;
        this.pythonAgentClient = pythonAgentClient;
        this.objectMapper = objectMapper;
    }

    public SseEmitter stream(Long userId, AgentContracts.ChatRequest chatRequest) {
        validateConfiguration();
        UUID runId = UUID.randomUUID();
        UUID conversationId = chatRequest.conversationId() == null
                ? UUID.randomUUID() : chatRequest.conversationId();
        AgentContracts.Actor actor = actorService.load(userId, chatRequest.role());
        String toolToken = tokenService.issueToolToken(
                runId, actor.userId(), actor.activeRole(), actor.roles(), Set.copyOf(actor.allowedTools()));
        AgentContracts.RunRequest runRequest = new AgentContracts.RunRequest(
                runId, conversationId, chatRequest.message().trim(), actor, toolToken);

        SseEmitter emitter = new SseEmitter(properties.getStreamTimeout().toMillis());
        Thread.ofVirtual().name("agent-stream-" + runId).start(() -> execute(runRequest, emitter));
        return emitter;
    }

    private void execute(AgentContracts.RunRequest request, SseEmitter emitter) {
        try {
            emitter.send(SseEmitter.event().name("connected").data(
                    objectMapper.writeValueAsString(Map.of(
                            "runId", request.runId(),
                            "conversationId", request.conversationId())),
                    MediaType.TEXT_PLAIN));
            pythonAgentClient.stream(request, emitter);
            emitter.complete();
        } catch (Exception exception) {
            sendError(emitter, exception);
        }
    }

    private void sendError(SseEmitter emitter, Exception exception) {
        String message = exception instanceof BusinessException
                ? exception.getMessage() : "Agent 服务暂时不可用，请稍后重试";
        try {
            emitter.send(SseEmitter.event().name("error").data(
                    objectMapper.writeValueAsString(Map.of("message", message)),
                    MediaType.TEXT_PLAIN));
            emitter.complete();
        } catch (IOException | JacksonException sendException) {
            emitter.completeWithError(sendException);
        }
    }

    private void validateConfiguration() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "教学 Agent 尚未启用");
        }
        if (!StringUtils.hasText(properties.getBridgeToken())) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "AI_AGENT_BRIDGE_TOKEN 未配置");
        }
    }
}
