package xyz.jiayuhan.learningservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** 学生上传原始作业图片后创建 OCR 批改任务。 */
public record CreatePhotoMarkingRequest(
        @NotNull(message = "原始图片文件不能为空") UUID originalFileId,
        @Min(value = 1, message = "页数至少为1")
        @Max(value = 20, message = "一次最多处理20页") Integer pageCount
) {
}
