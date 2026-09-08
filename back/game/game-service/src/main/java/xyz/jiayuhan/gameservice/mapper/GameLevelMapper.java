package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GameLevel;

public interface GameLevelMapper extends BaseMapper<GameLevel> {

    @Select("""
            SELECT *
            FROM game_level
            WHERE id = #{levelId}
            FOR SHARE
            """)
    GameLevel selectForShare(@Param("levelId") Long levelId);
}
