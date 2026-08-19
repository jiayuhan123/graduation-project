package xyz.jiayuhan.common.core.model;

import java.util.List;

/**
 * 与具体 ORM 框架解耦的通用分页响应。
 *
 * @param page 当前页码，从 1 开始
 * @param size 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param records 当前页数据
 * @param <T> 响应元素类型
 */
public record PageResult<T>(
        long page,
        long size,
        long total,
        long pages,
        List<T> records
) {
}
