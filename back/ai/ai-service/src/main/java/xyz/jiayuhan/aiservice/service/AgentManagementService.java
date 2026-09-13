package xyz.jiayuhan.aiservice.service;

import org.springframework.stereotype.Service;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 对外管理能力的授权边界，FastAPI 不直接暴露给浏览器。 */
@Service
public class AgentManagementService {

    private static final String CONTENT_PERMISSION = "content:manage";

    private final AgentActorService actorService;
    private final PythonAgentManagementClient client;

    public AgentManagementService(
            AgentActorService actorService,
            PythonAgentManagementClient client) {
        this.actorService = actorService;
        this.client = client;
    }

    public void feedback(Long actorId, AgentContracts.FeedbackRequest request) {
        if (request.rating() == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, "反馈只能选择有帮助或无帮助");
        }
        client.feedback(actorId, request);
    }

    public Map<String, Object> forget(Long actorId, UUID conversationId) {
        return client.forget(actorId, conversationId);
    }

    public AgentContracts.KnowledgeDocument upsertKnowledge(
            Long actorId,
            AgentContracts.KnowledgeUpsertRequest request) {
        actorService.requirePermission(actorId, CONTENT_PERMISSION);
        return client.upsertKnowledge(actorId, request);
    }

    public List<AgentContracts.KnowledgeDocument> listKnowledge(Long actorId) {
        actorService.requirePermission(actorId, CONTENT_PERMISSION);
        return client.listKnowledge();
    }

    public void deleteKnowledge(Long actorId, UUID documentId) {
        actorService.requirePermission(actorId, CONTENT_PERMISSION);
        client.deleteKnowledge(documentId);
    }

    public Map<String, Object> quality(Long actorId) {
        actorService.requirePermission(actorId, CONTENT_PERMISSION);
        return client.quality();
    }
}
