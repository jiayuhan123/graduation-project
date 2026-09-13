package xyz.jiayuhan.aiservice.service;

import org.junit.jupiter.api.Test;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiapi.port.AgentDomainToolPort;
import xyz.jiayuhan.common.core.exception.BusinessException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentActionServiceTest {

    @Test
    void rejectsConfirmationFromAnotherActor() {
        AgentTokenService tokenService = mock(AgentTokenService.class);
        AgentActionReplayGuard replayGuard = mock(AgentActionReplayGuard.class);
        AgentDomainToolPort domainToolPort = mock(AgentDomainToolPort.class);
        AgentActorService actorService = mock(AgentActorService.class);
        AgentContracts.AssignmentDraftCommand command = command();
        AgentTokenService.ActionTokenPayload payload = new AgentTokenService.ActionTokenPayload(
                "agent-action", UUID.randomUUID(), 7L, "CREATE_ASSIGNMENT", command,
                OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(tokenService.verifyActionToken("signed")).thenReturn(payload);
        AgentActionService service = new AgentActionService(
                tokenService, replayGuard, domainToolPort, actorService);

        assertThatThrownBy(() -> service.confirm(8L, "signed"))
                .isInstanceOf(BusinessException.class);
        verify(replayGuard, never()).claim(payload.draftId(), payload.expiresAt());
        verify(domainToolPort, never()).createAssignmentDraft(7L, command);
        verify(actorService, never()).requirePermission(7L, "teaching:assignment:manage");
    }

    private AgentContracts.AssignmentDraftCommand command() {
        OffsetDateTime start = OffsetDateTime.now(ZoneOffset.UTC).plusHours(1);
        return new AgentContracts.AssignmentDraftCommand(
                12L, "巩固作业", "Agent 草稿", "UNIFIED", start, start.plusDays(1), null,
                List.of(new AgentContracts.AssignmentQuestion(
                        100L, "ALL", 1, BigDecimal.TEN)));
    }
}
