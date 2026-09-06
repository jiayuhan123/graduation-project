package xyz.jiayuhan.contentservice.model.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 修改内容模块请求；稳定编码不可修改。 */
public record UpdateContentModuleRequest(
        @NotBlank(message = "模块名称不能为空")
        @Size(max = 100, message = "模块名称不能超过100个字符") String name,
        @NotNull(message = "年级不能为空") @Min(value = 1, message = "年级必须在1到6之间")
        @Max(value = 6, message = "年级必须在1到6之间") Integer gradeLevel,
        @NotNull(message = "学期不能为空") @Min(value = 1, message = "学期必须为1或2")
        @Max(value = 2, message = "学期必须为1或2") Integer semester,
        @NotBlank(message = "学习主题不能为空")
        @Size(max = 100, message = "学习主题不能超过100个字符") String learningTheme,
        @Size(max = 100, message = "出版社不能超过100个字符") String publisher,
        @Size(max = 100, message = "版本说明不能超过100个字符") String editionDescription,
        @Size(max = 500, message = "模块说明不能超过500个字符") String description,
        @NotNull(message = "排序不能为空") @Min(value = 0, message = "排序不能小于0") Integer sortOrder,
        @NotNull(message = "版本号不能为空") @Min(value = 0, message = "版本号不能小于0") Integer version
) {
}
