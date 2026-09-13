package xyz.jiayuhan.aiservice.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiservice.config.AiProperties;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** 对 Python 不透明的 HMAC 令牌，防止调用方篡改身份、工具范围和待执行动作。 */
@Service
public class AgentTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String TOOL_PURPOSE = "agent-tool";
    private static final String ACTION_PURPOSE = "agent-action";

    private final ObjectMapper objectMapper;
    private final AiProperties properties;

    public AgentTokenService(ObjectMapper objectMapper, AiProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String issueToolToken(
            UUID runId,
            Long actorId,
            String activeRole,
            List<String> roles,
            Set<String> allowedTools) {
        OffsetDateTime expiresAt = now().plus(properties.getToolTokenTtl());
        return sign(new ToolTokenPayload(
                TOOL_PURPOSE, runId, actorId, activeRole, roles, allowedTools, expiresAt));
    }

    public ToolTokenPayload verifyToolToken(String token) {
        ToolTokenPayload payload = verify(token, ToolTokenPayload.class);
        if (!TOOL_PURPOSE.equals(payload.purpose()) || payload.expiresAt().isBefore(now())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Agent 工具令牌无效或已过期");
        }
        return payload;
    }

    public ActionToken issueActionToken(Long actorId, AgentContracts.AssignmentDraftCommand command) {
        UUID draftId = UUID.randomUUID();
        OffsetDateTime expiresAt = now().plus(properties.getActionTokenTtl());
        String token = sign(new ActionTokenPayload(
                ACTION_PURPOSE, draftId, actorId, "CREATE_ASSIGNMENT", command, expiresAt));
        return new ActionToken(draftId, expiresAt, token);
    }

    public ActionTokenPayload verifyActionToken(String token) {
        ActionTokenPayload payload = verify(token, ActionTokenPayload.class);
        if (!ACTION_PURPOSE.equals(payload.purpose()) || payload.expiresAt().isBefore(now())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Agent 操作确认已失效，请重新生成草稿");
        }
        return payload;
    }

    private String sign(Object payload) {
        validateSecret();
        try {
            String encodedPayload = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(payload));
            String signature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(hmac(encodedPayload.getBytes(StandardCharsets.UTF_8)));
            return encodedPayload + "." + signature;
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "无法生成 Agent 安全令牌");
        }
    }

    private <T> T verify(String token, Class<T> payloadType) {
        validateSecret();
        if (!StringUtils.hasText(token)) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
        String[] parts = token.split("\\.", -1);
        if (parts.length != 2) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Agent 安全令牌格式不正确");
        }
        try {
            byte[] expected = hmac(parts[0].getBytes(StandardCharsets.UTF_8));
            byte[] supplied = Base64.getUrlDecoder().decode(parts[1]);
            if (!MessageDigest.isEqual(expected, supplied)) {
                throw new BusinessException(ResultCode.FORBIDDEN, "Agent 安全令牌签名无效");
            }
            byte[] payload = Base64.getUrlDecoder().decode(parts[0]);
            return objectMapper.readValue(payload, payloadType);
        } catch (IllegalArgumentException | JacksonException exception) {
            throw new BusinessException(ResultCode.FORBIDDEN, "Agent 安全令牌无法解析");
        }
    }

    private byte[] hmac(byte[] value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(
                    properties.getToolSigningSecret().getBytes(StandardCharsets.UTF_8),
                    HMAC_ALGORITHM));
            return mac.doFinal(value);
        } catch (GeneralSecurityException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "Agent 令牌签名不可用");
        }
    }

    private void validateSecret() {
        if (!StringUtils.hasText(properties.getToolSigningSecret())
                || properties.getToolSigningSecret().length() < 32) {
            throw new BusinessException(
                    ResultCode.SERVICE_UNAVAILABLE,
                    "AI_TOOL_SIGNING_SECRET 未配置或长度不足32位");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public record ToolTokenPayload(
            String purpose,
            UUID runId,
            Long actorId,
            String activeRole,
            List<String> roles,
            Set<String> allowedTools,
            OffsetDateTime expiresAt
    ) {
    }

    public record ActionTokenPayload(
            String purpose,
            UUID draftId,
            Long actorId,
            String actionType,
            AgentContracts.AssignmentDraftCommand command,
            OffsetDateTime expiresAt
    ) {
    }

    public record ActionToken(UUID draftId, OffsetDateTime expiresAt, String token) {
    }
}
