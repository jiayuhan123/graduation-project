package xyz.jiayuhan.userservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.jiayuhan.userservice.entity.OutboxEvent;

import java.time.LocalDateTime;
import java.util.List;

/** 用户服务事务发件箱 Mapper。 */
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {

    /**
     * 锁定一批到期事件；SKIP LOCKED 允许多个服务实例并行派发且不会重复领取同一行。
     */
    List<OutboxEvent> selectPendingBatchForUpdate(@Param("limit") int limit);

    /** 删除超过保留期的已发布记录，避免发件箱表无限增长。 */
    int deletePublishedBefore(@Param("cutoff") LocalDateTime cutoff);
}
