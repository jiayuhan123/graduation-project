package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/** OCR 工作器返回的一道切题结果。 */
public record PhotoRecognitionItemRequest(
        @NotNull(message = "题号不能为空") @Min(value = 1, message = "题号必须大于0") Integer itemNo,
        UUID cropFileId,
        Map<String, Integer> boundingBox,
        @NotBlank(message = "识别题干不能为空") @Size(max = 2000) String recognizedStem,
        @NotBlank(message = "识别答案不能为空") @Size(max = 2000) String recognizedAnswer,
        Long matchedQuestionId,
        @NotNull(message = "识别可信度不能为空")
        @DecimalMin(value = "0.0000") @DecimalMax(value = "1.0000") BigDecimal recognitionConfidence
) {
}
