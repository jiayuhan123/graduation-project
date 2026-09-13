package xyz.jiayuhan.aiservice.controller;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiservice.service.AgentActionService;
import xyz.jiayuhan.aiservice.service.AgentGatewayService;
import xyz.jiayuhan.aiservice.service.AgentManagementService;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.common.security.context.UserContext;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 教学 Agent 对前端的流式会话与确认执行接口。 */
@RestController
@RequestMapping("/api/ai")
public class AgentController {

    private final AgentGatewayService gatewayService;
    private final AgentActionService actionService;
    private final AgentManagementService managementService;

    public AgentController(
            AgentGatewayService gatewayService,
            AgentActionService actionService,
            AgentManagementService managementService) {
        this.gatewayService = gatewayService;
        this.actionService = actionService;
        this.managementService = managementService;
    }

    @PostMapping(path = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody AgentContracts.ChatRequest request) {
        return gatewayService.stream(UserContext.requireUserId(), request);
    }

    @PostMapping("/actions/confirm")
    public Result<AgentContracts.AssignmentCreated> confirmAction(
            @Valid @RequestBody AgentContracts.ConfirmActionRequest request) {
        return Result.success(
                "Agent 作业草稿创建成功",
                actionService.confirm(UserContext.requireUserId(), request.confirmationToken()));
    }

    @PostMapping("/feedback")
    public Result<Void> feedback(@Valid @RequestBody AgentContracts.FeedbackRequest request) {
        managementService.feedback(UserContext.requireUserId(), request);
        return Result.success();
    }

    @DeleteMapping("/conversations/{conversationId}/memory")
    public Result<Map<String, Object>> forgetMemory(@PathVariable UUID conversationId) {
        return Result.success(managementService.forget(UserContext.requireUserId(), conversationId));
    }

    @PostMapping("/knowledge/documents")
    public Result<AgentContracts.KnowledgeDocument> upsertKnowledge(
            @Valid @RequestBody AgentContracts.KnowledgeUpsertRequest request) {
        return Result.success(managementService.upsertKnowledge(UserContext.requireUserId(), request));
    }

    @GetMapping("/knowledge/documents")
    public Result<List<AgentContracts.KnowledgeDocument>> listKnowledge() {
        return Result.success(managementService.listKnowledge(UserContext.requireUserId()));
    }

    @DeleteMapping("/knowledge/documents/{documentId}")
    public Result<Void> deleteKnowledge(@PathVariable UUID documentId) {
        managementService.deleteKnowledge(UserContext.requireUserId(), documentId);
        return Result.success();
    }

    @GetMapping("/quality")
    public Result<Map<String, Object>> quality() {
        return Result.success(managementService.quality(UserContext.requireUserId()));
    }
}
