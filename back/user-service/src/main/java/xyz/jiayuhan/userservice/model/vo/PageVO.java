package xyz.jiayuhan.userservice.model.vo;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;
import java.util.function.Function;

/**
 * 与 MyBatis-Plus 分页对象解耦的通用分页响应。
 *
 * @param page 当前页码
 * @param size 每页数量
 * @param total 总记录数
 * @param pages 总页数
 * @param records 当前页数据
 * @param <T> 对外响应元素类型
 */
public record PageVO<T>(
        long page,
        long size,
        long total,
        long pages,
        List<T> records
) {
    /**
     * 将持久层分页结果映射为接口层分页响应，防止直接暴露数据库实体。
     *
     * @param source 持久层分页结果
     * @param mapper 实体到响应对象的转换函数
     * @param <S> 持久层元素类型
     * @param <T> 响应元素类型
     * @return 映射后的分页响应
     */
    public static <S, T> PageVO<T> from(IPage<S> source, Function<S, T> mapper) {
        return new PageVO<>(
                source.getCurrent(),
                source.getSize(),
                source.getTotal(),
                source.getPages(),
                source.getRecords().stream().map(mapper).toList()
        );
    }
}
