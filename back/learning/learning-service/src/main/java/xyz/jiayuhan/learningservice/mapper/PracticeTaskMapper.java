package xyz.jiayuhan.learningservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.learningservice.entity.PracticeTask;

public interface PracticeTaskMapper extends BaseMapper<PracticeTask> {

    @Select("""
            SELECT *
            FROM practice_task
            WHERE id = #{taskId}
              AND student_id = #{studentId}
            FOR UPDATE
            """)
    PracticeTask selectForUpdate(
            @Param("taskId") Long taskId,
            @Param("studentId") Long studentId);

    @Insert("""
            INSERT INTO practice_task (
                id, student_id, source_type, source_id, title, mode, status,
                question_count, time_limit_seconds, target_accuracy, started_at,
                completed_at, expires_at, created_at, updated_at, version
            ) VALUES (
                #{id}, #{studentId}, #{sourceType}, #{sourceId}, #{title}, #{mode}, #{status},
                #{questionCount}, #{timeLimitSeconds}, #{targetAccuracy}, #{startedAt},
                #{completedAt}, #{expiresAt}, #{createdAt}, #{updatedAt}, #{version}
            )
            ON CONFLICT (student_id, source_type, source_id)
            WHERE source_type = 'ASSIGNMENT' AND source_id IS NOT NULL
            DO NOTHING
            """)
    int insertAssignmentIfAbsent(PracticeTask task);
}
