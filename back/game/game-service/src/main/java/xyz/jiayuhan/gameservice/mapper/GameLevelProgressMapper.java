package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GameLevelProgress;

import java.time.OffsetDateTime;

public interface GameLevelProgressMapper extends BaseMapper<GameLevelProgress> {

    @Select("""
            SELECT *
            FROM game_level_progress
            WHERE student_id = #{studentId}
              AND level_id = #{levelId}
            FOR UPDATE
            """)
    GameLevelProgress selectForUpdate(
            @Param("studentId") Long studentId,
            @Param("levelId") Long levelId);

    @Insert("""
            INSERT INTO game_level_progress (
                student_id, level_id, status, attempt_count,
                first_unlocked_at, created_at, updated_at
            ) VALUES (
                #{studentId}, #{levelId}, 'UNLOCKED', 0,
                #{unlockedAt}, #{unlockedAt}, #{unlockedAt}
            )
            ON CONFLICT (student_id, level_id) DO UPDATE
            SET status = 'UNLOCKED',
                first_unlocked_at = COALESCE(game_level_progress.first_unlocked_at, #{unlockedAt}),
                updated_at = #{unlockedAt}
            WHERE game_level_progress.status = 'LOCKED'
            """)
    int insertUnlockedIfAbsent(
            @Param("studentId") Long studentId,
            @Param("levelId") Long levelId,
            @Param("unlockedAt") OffsetDateTime unlockedAt);
}
