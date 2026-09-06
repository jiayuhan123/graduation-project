package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 修改知识点请求；稳定编码及所属模块不可修改。 */
public record UpdateKnowledgePointRequest(
        Long parentId,
        @NotBlank(message = "知识点名称不能为空")
        @Size(max = 100, message = "知识点名称不能超过100个字符") String name,
        @Size(max = 1000, message = "知识点说明不能超过1000个字符") String description,
        @NotNull(message = "基础难度不能为空") @Min(value = 1, message = "难度必须在1到5之间")
        @Max(value = 5, message = "难度必须在1到5之间") Integer difficultyLevel,
        @NotNull(message = "排序不能为空") @Min(value = 0, message = "排序不能小于0") Integer sortOrder,
        @NotNull(message = "版本号不能为空") @Min(value = 0, message = "版本号不能小于0") Integer version
) {
}
