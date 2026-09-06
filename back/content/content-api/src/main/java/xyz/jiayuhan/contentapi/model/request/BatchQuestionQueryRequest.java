package xyz.jiayuhan.contentapi.model.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/** 跨服务批量查询题目时使用的受限请求。 */
public record BatchQuestionQueryRequest(
        @NotEmpty(message = "题目 ID 列表不能为空")
        @Size(max = 100, message = "单次最多查询 100 道题")
        List<@NotNull(message = "题目 ID 不能为空") Long> questionIds
) {
}
