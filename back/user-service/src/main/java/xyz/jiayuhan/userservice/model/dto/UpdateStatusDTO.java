package xyz.jiayuhan.userservice.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 启用或禁用用户/角色的通用请求。
 *
 * @param status 目标状态：1 启用、0 禁用
 */
public record UpdateStatusDTO(
        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态只能为 0 或 1")
        @Max(value = 1, message = "状态只能为 0 或 1")
        Integer status
) {
}
