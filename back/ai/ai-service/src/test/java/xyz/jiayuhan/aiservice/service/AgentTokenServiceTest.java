package xyz.jiayuhan.aiservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import xyz.jiayuhan.aiservice.config.AiProperties;
import xyz.jiayuhan.common.core.exception.BusinessException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AgentTokenServiceTest {

    private AgentTokenService tokenService;

    @BeforeEach
    void setUp() {
        AiProperties properties = new AiProperties();
        properties.setToolSigningSecret("test-signing-secret-with-at-least-32-characters");
        tokenService = new AgentTokenService(JsonMapper.builder().build(), properties);
    }

    @Test
    void roundTripsSignedToolContext() {
        UUID runId = UUID.randomUUID();

        String token = tokenService.issueToolToken(
                runId, 42L, "teacher", List.of("teacher"), Set.of("get_class_dashboard"));
        AgentTokenService.ToolTokenPayload payload = tokenService.verifyToolToken(token);

        assertThat(payload.runId()).isEqualTo(runId);
        assertThat(payload.actorId()).isEqualTo(42L);
        assertThat(payload.allowedTools()).containsExactly("get_class_dashboard");
    }

    @Test
    void rejectsTamperedToken() {
        String token = tokenService.issueToolToken(
                UUID.randomUUID(), 42L, "teacher", List.of("teacher"), Set.of("search_questions"));
        String tampered = "x" + token.substring(1);

        assertThatThrownBy(() -> tokenService.verifyToolToken(tampered))
                .isInstanceOf(BusinessException.class);
    }
}
