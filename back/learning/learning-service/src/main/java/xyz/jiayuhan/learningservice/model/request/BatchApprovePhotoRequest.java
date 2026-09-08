package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** OCR 工作台批量通过请求，服务端仍会二次校验可信度和题目匹配。 */
public record BatchApprovePhotoRequest(
        @NotEmpty(message = "请选择至少一个拍照任务")
        @Size(max = 20, message = "一次最多处理20个拍照任务")
        List<@NotNull(message = "任务 ID 不能为空") Long> markingTaskIds
) {
}
