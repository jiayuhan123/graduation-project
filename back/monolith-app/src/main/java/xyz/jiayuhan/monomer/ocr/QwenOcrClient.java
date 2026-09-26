package xyz.jiayuhan.monomer.ocr;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionItemRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 调用百炼 Qwen-VL-OCR，并把不可信模型输出收敛为学习域的识别 DTO。 */
@Component
@ConditionalOnProperty(prefix = "app.ocr", name = "enabled", havingValue = "true")
public class QwenOcrClient {

    private static final int MAX_TEXT_LENGTH = 2000;
    private static final BigDecimal UNANSWERED_CONFIDENCE_LIMIT = new BigDecimal("0.5000");
    private static final String PROMPT = """
            你只负责转录图片，不负责解题。请识别这张小学数学作业图片中的每一道题和学生已经书写的答案。

            仅返回一个合法 JSON 对象，不要返回 Markdown、代码块、解释或额外文字。格式必须是：
            {"items":[{"itemNo":1,"recognizedStem":"印刷或手写的题目原文","recognizedAnswer":"学生书写的答案","boundingBox":{"x":0,"y":0,"width":100,"height":50},"confidence":0.95}]}

            规则：
            1. 按图片从上到下、从左到右排序，一道题对应一个 items 元素，itemNo 从 1 连续递增。
            2. recognizedStem 只包含题目原文；recognizedAnswer 只包含学生实际书写的答案或最终结果，绝对不要计算、补全或猜测答案。
            3. 没有作答或无法辨认时 recognizedAnswer 填 "?"，并将 confidence 设为不高于 0.5。
            4. boundingBox 使用原图像素坐标，x、y 是题目区域左上角，width、height 是整个题目与作答区域的宽高；无法可靠定位时填 null。
            5. confidence 是本题题干与答案整体转录可信度，范围 0 到 1；文字模糊、遮挡、裁切或答案不确定时必须降低。
            6. 保留数学符号、分数、小数、单位和等号，不要改写题目语义。
            7. 图片中没有可识别数学题时返回 {"items":[]}。
            """;

    private final RestClient restClient;
    private final OcrProperties properties;
    private final ObjectMapper objectMapper;

    public QwenOcrClient(
            @Qualifier("qwenOcrRestClient") RestClient restClient,
            OcrProperties properties,
            ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<PhotoRecognitionItemRequest> recognize(byte[] image, String contentType) {
        if (image == null || image.length == 0) {
            throw new OcrProcessingException("作业图片为空");
        }
        String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(image);
        Map<String, Object> request = Map.of(
                "model", properties.getModelName(),
                "messages", List.of(Map.of(
                        "role", "user",
                        "content", List.of(
                                Map.of(
                                        "type", "image_url",
                                        "image_url", Map.of(
                                                "url", dataUrl,
                                                "min_pixels", properties.getMinPixels(),
                                                "max_pixels", properties.getMaxPixels())),
                                Map.of("type", "text", "text", PROMPT)))),
                "temperature", 0,
                "max_tokens", properties.getMaxOutputTokens());

        String responseBody;
        try {
            responseBody = restClient.post()
                    .uri(properties.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientResponseException exception) {
            throw new OcrProcessingException(
                    "百炼 OCR 请求失败，HTTP " + exception.getStatusCode().value(), exception);
        } catch (RestClientException exception) {
            throw new OcrProcessingException("百炼 OCR 服务暂时不可用", exception);
        }
        return parseResponse(responseBody);
    }

    List<PhotoRecognitionItemRequest> parseResponse(String responseBody) {
        if (!StringUtils.hasText(responseBody)) {
            throw new OcrProcessingException("百炼 OCR 返回了空响应");
        }
        try {
            QwenResponse response = objectMapper.readValue(responseBody, QwenResponse.class);
            if (response.choices() == null || response.choices().isEmpty()
                    || response.choices().getFirst().message() == null) {
                throw new OcrProcessingException("百炼 OCR 响应中缺少识别结果");
            }
            String modelText = contentText(response.choices().getFirst().message().content());
            RecognizedDocument document = objectMapper.readValue(
                    extractJsonObject(modelText), RecognizedDocument.class);
            return normalize(document);
        } catch (OcrProcessingException exception) {
            throw exception;
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new OcrProcessingException("百炼 OCR 返回格式不正确", exception);
        }
    }

    private String contentText(JsonNode content) {
        if (content == null || content.isNull()) {
            throw new OcrProcessingException("百炼 OCR 响应中缺少文本内容");
        }
        if (content.isString()) {
            return content.stringValue();
        }
        if (content.isArray()) {
            StringBuilder result = new StringBuilder();
            for (JsonNode part : content) {
                JsonNode text = part.get("text");
                if (text != null && text.isString()) {
                    result.append(text.stringValue());
                }
            }
            if (!result.isEmpty()) {
                return result.toString();
            }
        }
        throw new OcrProcessingException("百炼 OCR 响应文本类型不正确");
    }

    private String extractJsonObject(String text) {
        if (!StringUtils.hasText(text)) {
            throw new OcrProcessingException("百炼 OCR 未识别出内容");
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new OcrProcessingException("百炼 OCR 未返回 JSON 对象");
        }
        return text.substring(start, end + 1);
    }

    private List<PhotoRecognitionItemRequest> normalize(RecognizedDocument document) {
        if (document == null || document.items() == null || document.items().isEmpty()) {
            throw new OcrProcessingException("图片中没有识别到数学题");
        }
        if (document.items().size() > properties.getMaxItems()) {
            throw new OcrProcessingException("单张图片识别题目数量超过限制");
        }

        List<PhotoRecognitionItemRequest> result = new ArrayList<>();
        int itemNo = 1;
        for (RecognizedItem item : document.items()) {
            if (item == null || !StringUtils.hasText(item.recognizedStem())) {
                continue;
            }
            String stem = checkedText(item.recognizedStem(), "题干");
            String answer = StringUtils.hasText(item.recognizedAnswer())
                    ? checkedText(item.recognizedAnswer(), "答案") : "?";
            BigDecimal confidence = normalizeConfidence(item.confidence());
            if ("?".equals(answer)) {
                confidence = confidence.min(UNANSWERED_CONFIDENCE_LIMIT);
            }
            result.add(new PhotoRecognitionItemRequest(
                    itemNo++, null, normalizeBoundingBox(item.boundingBox()),
                    stem, answer, null, confidence));
        }
        if (result.isEmpty()) {
            throw new OcrProcessingException("图片中没有有效的数学题识别结果");
        }
        return List.copyOf(result);
    }

    private String checkedText(String text, String fieldName) {
        String normalized = text.trim();
        if (normalized.length() > MAX_TEXT_LENGTH) {
            throw new OcrProcessingException(fieldName + "长度超过系统限制");
        }
        return normalized;
    }

    private BigDecimal normalizeConfidence(BigDecimal confidence) {
        BigDecimal value = confidence == null ? new BigDecimal("0.5000") : confidence;
        value = value.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private Map<String, Integer> normalizeBoundingBox(Map<String, Integer> box) {
        if (box == null) {
            return null;
        }
        Integer x = box.get("x");
        Integer y = box.get("y");
        Integer width = box.get("width");
        Integer height = box.get("height");
        if (x == null || y == null || width == null || height == null
                || x < 0 || y < 0 || width <= 0 || height <= 0) {
            return null;
        }
        Map<String, Integer> normalized = new LinkedHashMap<>();
        normalized.put("x", x);
        normalized.put("y", y);
        normalized.put("width", width);
        normalized.put("height", height);
        return Map.copyOf(normalized);
    }

    private record QwenResponse(List<Choice> choices) {
    }

    private record Choice(Message message) {
    }

    private record Message(JsonNode content) {
    }

    private record RecognizedDocument(List<RecognizedItem> items) {
    }

    private record RecognizedItem(
            Integer itemNo,
            String recognizedStem,
            String recognizedAnswer,
            Map<String, Integer> boundingBox,
            BigDecimal confidence) {
    }

    /** 对外只暴露安全、可写入任务失败原因的摘要，不携带响应正文或密钥。 */
    static final class OcrProcessingException extends RuntimeException {
        OcrProcessingException(String message) {
            super(message);
        }

        OcrProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
