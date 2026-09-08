package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 外部 OCR 工作器完成切题和识别后提交的结构化结果。 */
public record PhotoRecognitionRequest(
        @NotEmpty(message = "识别结果不能为空")
        @Size(max = 100, message = "一次最多返回100道题")
        List<@Valid PhotoRecognitionItemRequest> items
) {
}
