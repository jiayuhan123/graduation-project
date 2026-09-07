package xyz.jiayuhan.learningservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.learningservice.entity.MasteryRecord;

public interface MasteryRecordMapper extends BaseMapper<MasteryRecord> {

    /** 同一学生同一知识点的更新在事务内串行，包含尚未创建快照的场景。 */
    @Select("""
            SELECT 1
            FROM (
                SELECT pg_advisory_xact_lock(
                    hashtextextended(CONCAT(#{studentId}, ':', #{knowledgePointId}), 0)
                )
            ) AS mastery_lock
            """)
    int acquireUpdateLock(
            @Param("studentId") Long studentId,
            @Param("knowledgePointId") Long knowledgePointId);
}
