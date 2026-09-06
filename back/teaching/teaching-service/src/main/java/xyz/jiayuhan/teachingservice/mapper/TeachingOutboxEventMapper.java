package xyz.jiayuhan.teachingservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.jiayuhan.teachingservice.entity.TeachingOutboxEvent;

import java.time.OffsetDateTime;
import java.util.List;

/** 教学服务事务发件箱 Mapper。 */
public interface TeachingOutboxEventMapper extends BaseMapper<TeachingOutboxEvent> {

    List<TeachingOutboxEvent> selectPendingBatchForUpdate(@Param("limit") int limit);

    int deletePublishedBefore(@Param("cutoff") OffsetDateTime cutoff);
}
