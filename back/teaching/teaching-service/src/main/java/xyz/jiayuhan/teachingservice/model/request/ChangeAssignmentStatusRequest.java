package xyz.jiayuhan.teachingservice.model.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** 结束或取消作业请求。 */
public record ChangeAssignmentStatusRequest(
        @NotBlank(message = "操作不能为空")
        @Pattern(regexp = "CLOSE|CANCEL", message = "作业操作不正确") String action,
        @NotNull(message = "版本号不能为空") @Min(0) Integer version
) {
}
