package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 分层作业发布时的学生层级分配。 */
public record StudentLayerAssignmentRequest(
        @NotNull(message = "学生编号不能为空") Long studentId,
        @NotBlank(message = "学生层级不能为空")
        @Pattern(regexp = "^[A-Z][A-Z0-9_-]{0,31}$", message = "学生层级格式不正确") String layerCode
) {
}
