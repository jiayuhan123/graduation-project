package xyz.jiayuhan.aiservice.service;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.jiayuhan.aiapi.AiApiConstants;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiservice.config.AiProperties;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 通过服务间凭证访问 FastAPI 的记忆、知识库、反馈与质量接口。 */
@Component
public class PythonAgentManagementClient {

    private static final Logger log = LoggerFactory.getLogger(PythonAgentManagementClient.class);

    private final RestClient restClient;
    private final AiProperties properties;

    public PythonAgentManagementClient(RestClient pythonAgentRestClient, AiProperties properties) {
        this.restClient = pythonAgentRestClient;
        this.properties = properties;
    }

    public void feedback(Long actorId, AgentContracts.FeedbackRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", request.runId());
        body.put("actorId", actorId);
        body.put("rating", request.rating());
        body.put("comment", request.comment());
        postMap("/v1/agent/feedback", body);
    }

    public Map<String, Object> forget(Long actorId, UUID conversationId) {
        return postMap(
                "/v1/agent/memory/forget",
                Map.of("actorId", actorId, "conversationId", conversationId));
    }

    public AgentContracts.KnowledgeDocument upsertKnowledge(
            Long actorId,
            AgentContracts.KnowledgeUpsertRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("actorId", actorId);
        body.put("title", request.title());
        body.put("sourceUri", request.sourceUri());
        body.put("sourceType", request.sourceType());
        body.put("content", request.content());
        body.put("metadata", request.metadata());
        return post("/v1/knowledge/documents", body, AgentContracts.KnowledgeDocument.class);
    }

    public List<AgentContracts.KnowledgeDocument> listKnowledge() {
        try {
            List<AgentContracts.KnowledgeDocument> result = restClient.get()
                    .uri("/v1/knowledge/documents")
                    .header(AiApiConstants.BRIDGE_TOKEN_HEADER, properties.getBridgeToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return result == null ? List.of() : result;
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    public void deleteKnowledge(UUID documentId) {
        try {
            restClient.delete()
                    .uri("/v1/knowledge/documents/{id}", documentId)
                    .header(AiApiConstants.BRIDGE_TOKEN_HEADER, properties.getBridgeToken())
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    public Map<String, Object> quality() {
        try {
            Map<String, Object> result = restClient.get()
                    .uri("/v1/agent/quality")
                    .header(AiApiConstants.BRIDGE_TOKEN_HEADER, properties.getBridgeToken())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return result == null ? Map.of() : result;
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private <T> T post(String uri, Object body, Class<T> responseType) {
        try {
            T result = restClient.post()
                    .uri(uri)
                    .header(AiApiConstants.BRIDGE_TOKEN_HEADER, properties.getBridgeToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(responseType);
            if (result == null) {
                throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "Python Agent 返回空响应");
            }
            return result;
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private Map<String, Object> postMap(String uri, Object body) {
        try {
            Map<String, Object> result = restClient.post()
                    .uri(uri)
                    .header(AiApiConstants.BRIDGE_TOKEN_HEADER, properties.getBridgeToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return result == null ? Map.of() : result;
        } catch (RestClientException exception) {
            throw unavailable(exception);
        }
    }

    private BusinessException unavailable(RestClientException exception) {
        log.warn("Python Agent 管理接口调用失败: {}", exception.getClass().getSimpleName());
        return new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "Python Agent 管理接口暂时不可用");
    }
}
