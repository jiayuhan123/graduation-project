package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 学生确认或教师复核一道低可信 OCR 结果。 */
public record ReviewPhotoItemRequest(
        @NotBlank(message = "复核结论不能为空")
        @Pattern(regexp = "APPROVE|CORRECT|REJECT", message = "复核结论不正确") String decision,
        @Size(max = 2000) String recognizedStem,
        @Size(max = 2000) String recognizedAnswer,
        Long matchedQuestionId,
        @Size(max = 500) String comment
) {
}
