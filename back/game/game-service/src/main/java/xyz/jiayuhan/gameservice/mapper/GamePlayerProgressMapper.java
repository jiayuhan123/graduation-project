package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GamePlayerProgress;

public interface GamePlayerProgressMapper extends BaseMapper<GamePlayerProgress> {

    @Select("""
            SELECT *
            FROM game_player_progress
            WHERE student_id = #{studentId}
            FOR UPDATE
            """)
    GamePlayerProgress selectForUpdate(@Param("studentId") Long studentId);
}
