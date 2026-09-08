package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** OCR 工作器无法完成识别时回传的安全失败摘要。 */
public record PhotoRecognitionFailureRequest(
        @NotBlank(message = "失败原因不能为空")
        @Size(max = 1000, message = "失败原因最多1000字") String reason
) {
}
