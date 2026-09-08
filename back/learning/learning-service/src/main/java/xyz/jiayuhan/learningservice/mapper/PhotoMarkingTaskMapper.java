package xyz.jiayuhan.learningservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Insert;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;

public interface PhotoMarkingTaskMapper extends BaseMapper<PhotoMarkingTask> {

    @Insert("""
            INSERT INTO photo_marking_task (
                id, student_id, original_file_id, practice_task_id, status, page_count,
                overall_confidence, recognized_item_count, reviewer_id, review_status,
                submitted_at, processed_at, reviewed_at, failure_reason,
                created_at, updated_at, version
            ) VALUES (
                #{id}, #{studentId}, #{originalFileId}, #{practiceTaskId}, #{status}, #{pageCount},
                #{overallConfidence}, #{recognizedItemCount}, #{reviewerId}, #{reviewStatus},
                #{submittedAt}, #{processedAt}, #{reviewedAt}, #{failureReason},
                #{createdAt}, #{updatedAt}, #{version}
            )
            ON CONFLICT (original_file_id) DO NOTHING
            """)
    int insertIfAbsent(PhotoMarkingTask task);

    @Select("""
            SELECT *
            FROM photo_marking_task
            WHERE id = #{taskId}
            FOR UPDATE
            """)
    PhotoMarkingTask selectForUpdate(@Param("taskId") Long taskId);
}
