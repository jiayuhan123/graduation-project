package xyz.jiayuhan.monomer.ocr;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.learningservice.model.request.PhotoRecognitionItemRequest;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class QwenOcrClientTest {

    private static final String ENDPOINT = "https://dashscope.example/v1/chat/completions";

    private MockRestServiceServer server;
    private QwenOcrClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        OcrProperties properties = new OcrProperties();
        properties.setEndpoint(ENDPOINT);
        properties.setModelName("qwen-vl-ocr-latest");
        client = new QwenOcrClient(builder.build(), properties, new ObjectMapper());
    }

    @Test
    void recognizesHomeworkAndNormalizesUnansweredConfidence() throws Exception {
        String modelText = """
                ```json
                {"items":[{"itemNo":9,"recognizedStem":"6 × 7 =","recognizedAnswer":"42","boundingBox":{"x":10,"y":20,"width":120,"height":40},"confidence":0.96},{"itemNo":10,"recognizedStem":"8 ÷ 2 =","recognizedAnswer":"?","boundingBox":null,"confidence":0.99}]}
                ```
                """;
        String response = new ObjectMapper().writeValueAsString(Map.of(
                "choices", List.of(Map.of("message", Map.of("content", modelText)))));
        server.expect(requestTo(ENDPOINT))
                .andExpect(content().string(containsString("qwen-vl-ocr-latest")))
                .andExpect(content().string(containsString("data:image/png;base64,")))
                .andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

        List<PhotoRecognitionItemRequest> result = client.recognize(
                "image".getBytes(StandardCharsets.UTF_8), "image/png");

        assertEquals(2, result.size());
        assertEquals(1, result.getFirst().itemNo());
        assertEquals("6 × 7 =", result.getFirst().recognizedStem());
        assertEquals("42", result.getFirst().recognizedAnswer());
        assertEquals(new BigDecimal("0.9600"), result.getFirst().recognitionConfidence());
        assertEquals(10, result.getFirst().boundingBox().get("x"));
        assertEquals(2, result.get(1).itemNo());
        assertEquals(new BigDecimal("0.5000"), result.get(1).recognitionConfidence());
        assertNull(result.get(1).boundingBox());
        server.verify();
    }

    @Test
    void rejectsAResponseWithoutRecognizableQuestions() throws Exception {
        String response = new ObjectMapper().writeValueAsString(Map.of(
                "choices", List.of(Map.of(
                        "message", Map.of("content", "{\"items\":[]}")))));

        assertThrows(
                QwenOcrClient.OcrProcessingException.class,
                () -> client.parseResponse(response));
    }
}
