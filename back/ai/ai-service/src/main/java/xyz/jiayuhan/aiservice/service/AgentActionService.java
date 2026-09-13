package xyz.jiayuhan.aiservice.service;

import org.springframework.stereotype.Service;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiapi.port.AgentDomainToolPort;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

/** 人工确认后执行签名动作；模型不参与最终写入链路。 */
@Service
public class AgentActionService {

    private static final String ASSIGNMENT_PERMISSION = "teaching:assignment:manage";

    private final AgentTokenService tokenService;
    private final AgentActionReplayGuard replayGuard;
    private final AgentDomainToolPort domainToolPort;
    private final AgentActorService actorService;

    public AgentActionService(
            AgentTokenService tokenService,
            AgentActionReplayGuard replayGuard,
            AgentDomainToolPort domainToolPort,
            AgentActorService actorService) {
        this.tokenService = tokenService;
        this.replayGuard = replayGuard;
        this.domainToolPort = domainToolPort;
        this.actorService = actorService;
    }

    public AgentContracts.AssignmentCreated confirm(Long actorId, String confirmationToken) {
        AgentTokenService.ActionTokenPayload payload = tokenService.verifyActionToken(confirmationToken);
        if (!actorId.equals(payload.actorId()) || !"CREATE_ASSIGNMENT".equals(payload.actionType())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "无权确认该 Agent 操作");
        }
        actorService.requirePermission(actorId, ASSIGNMENT_PERMISSION);
        replayGuard.claim(payload.draftId(), payload.expiresAt());
        try {
            return domainToolPort.createAssignmentDraft(actorId, payload.command());
        } catch (RuntimeException exception) {
            replayGuard.release(payload.draftId());
            throw exception;
        }
    }
}
