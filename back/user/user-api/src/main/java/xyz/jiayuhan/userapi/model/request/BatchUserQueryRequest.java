package xyz.jiayuhan.userapi.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 批量查询用户摘要的内部请求。 */
public record BatchUserQueryRequest(
        @NotEmpty(message = "用户 ID 列表不能为空")
        @Size(max = 100, message = "一次最多查询 100 个用户")
        List<@NotNull(message = "用户 ID 不能为空") Long> userIds
) {
}
