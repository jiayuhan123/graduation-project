package xyz.jiayuhan.contentservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.jiayuhan.contentservice.entity.ContentOutboxEvent;

import java.time.OffsetDateTime;
import java.util.List;

/** 内容服务事务发件箱 Mapper。 */
public interface ContentOutboxEventMapper extends BaseMapper<ContentOutboxEvent> {

    List<ContentOutboxEvent> selectPendingBatchForUpdate(@Param("limit") int limit);

    int deletePublishedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
