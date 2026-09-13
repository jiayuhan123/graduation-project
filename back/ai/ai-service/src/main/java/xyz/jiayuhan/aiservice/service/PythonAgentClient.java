package xyz.jiayuhan.aiservice.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import xyz.jiayuhan.aiapi.AiApiConstants;
import xyz.jiayuhan.aiapi.model.AgentContracts;
import xyz.jiayuhan.aiservice.config.AiProperties;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** 调用 FastAPI Agent 并将其 SSE 事件逐条转发给前端。 */
@Component
public class PythonAgentClient {

    private final RestClient restClient;
    private final AiProperties properties;

    public PythonAgentClient(RestClient pythonAgentRestClient, AiProperties properties) {
        this.restClient = pythonAgentRestClient;
        this.properties = properties;
    }

    public void stream(AgentContracts.RunRequest request, SseEmitter emitter) {
        restClient.post()
                .uri("/v1/agent/runs/stream")
                .header(AiApiConstants.BRIDGE_TOKEN_HEADER, properties.getBridgeToken())
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .body(request)
                .exchange((ignored, response) -> {
                    if (!response.getStatusCode().is2xxSuccessful()) {
                        throw new BusinessException(
                                ResultCode.SERVICE_UNAVAILABLE,
                                "Python Agent 返回异常状态：" + response.getStatusCode().value());
                    }
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                            response.getBody(), StandardCharsets.UTF_8))) {
                        forwardEvents(reader, emitter);
                    } catch (IOException exception) {
                        throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, "Agent 流式响应中断");
                    }
                    return null;
                });
    }

    private void forwardEvents(BufferedReader reader, SseEmitter emitter) throws IOException {
        String eventName = "message";
        StringBuilder data = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isEmpty()) {
                send(emitter, eventName, data.toString());
                eventName = "message";
                data.setLength(0);
                continue;
            }
            if (line.startsWith("event:")) {
                eventName = line.substring("event:".length()).trim();
            } else if (line.startsWith("data:")) {
                if (!data.isEmpty()) {
                    data.append('\n');
                }
                data.append(line.substring("data:".length()).trim());
            }
        }
        send(emitter, eventName, data.toString());
    }

    private void send(SseEmitter emitter, String eventName, String data) throws IOException {
        if (!StringUtils.hasText(data)) {
            return;
        }
        emitter.send(SseEmitter.event().name(eventName).data(data, MediaType.TEXT_PLAIN));
    }
}
