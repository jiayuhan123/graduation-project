package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 创建知识点请求。 */
public record CreateKnowledgePointRequest(
        @NotNull(message = "所属模块不能为空") Long moduleId,
        Long parentId,
        @NotBlank(message = "知识点编码不能为空")
        @Size(max = 64, message = "知识点编码不能超过64个字符")
        @Pattern(regexp = "^[A-Z0-9][A-Z0-9._-]*$", message = "知识点编码格式不正确") String code,
        @NotBlank(message = "知识点名称不能为空")
        @Size(max = 100, message = "知识点名称不能超过100个字符") String name,
        @Size(max = 1000, message = "知识点说明不能超过1000个字符") String description,
        @NotNull(message = "基础难度不能为空") @Min(value = 1, message = "难度必须在1到5之间")
        @Max(value = 5, message = "难度必须在1到5之间") Integer difficultyLevel,
        @NotNull(message = "排序不能为空") @Min(value = 0, message = "排序不能小于0") Integer sortOrder,
        @NotNull(message = "状态不能为空") @Min(value = 0, message = "状态只能为0或1")
        @Max(value = 1, message = "状态只能为0或1") Integer status
) {
}
