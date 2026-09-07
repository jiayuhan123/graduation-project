package xyz.jiayuhan.learningservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import xyz.jiayuhan.learningservice.entity.AssignmentSyncEvent;

import java.time.OffsetDateTime;
import java.util.List;

/** 作业进度同步事务发件箱 Mapper。 */
public interface AssignmentSyncEventMapper extends BaseMapper<AssignmentSyncEvent> {

    List<AssignmentSyncEvent> selectPendingBatchForUpdate(@Param("limit") int limit);

    int deleteSentBefore(@Param("cutoff") OffsetDateTime cutoff);
}
