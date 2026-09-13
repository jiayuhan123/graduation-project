package xyz.jiayuhan.aiservice.service;

import org.junit.jupiter.api.Test;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.common.core.exception.BusinessException;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class AgentManagementServiceTest {

    @Test
    void rejectsNeutralFeedbackBeforeCallingPython() {
        AgentActorService actorService = mock(AgentActorService.class);
        PythonAgentManagementClient client = mock(PythonAgentManagementClient.class);
        AgentManagementService service = new AgentManagementService(actorService, client);
        AgentContracts.FeedbackRequest request = new AgentContracts.FeedbackRequest(
                UUID.randomUUID(), 0, "neutral");

        assertThrows(BusinessException.class, () -> service.feedback(7L, request));

        verify(client, never()).feedback(7L, request);
    }

    @Test
    void checksContentPermissionBeforeKnowledgeIngestion() {
        AgentActorService actorService = mock(AgentActorService.class);
        PythonAgentManagementClient client = mock(PythonAgentManagementClient.class);
        AgentManagementService service = new AgentManagementService(actorService, client);
        AgentContracts.KnowledgeUpsertRequest request = new AgentContracts.KnowledgeUpsertRequest(
                "分数教学", "manual://fraction", "TEXTBOOK", "有效正文内容需要至少二十个字符。",
                Map.of("grade", 5));

        service.upsertKnowledge(7L, request);

        verify(actorService).requirePermission(7L, "content:manage");
        verify(client).upsertKnowledge(7L, request);
    }
}
