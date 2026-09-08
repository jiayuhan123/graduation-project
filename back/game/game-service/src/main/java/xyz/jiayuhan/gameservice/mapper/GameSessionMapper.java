package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GameSession;

import java.time.OffsetDateTime;
import java.util.List;

public interface GameSessionMapper extends BaseMapper<GameSession> {

    @Select("""
            SELECT *
            FROM game_session
            WHERE id = #{sessionId}
            FOR UPDATE
            """)
    GameSession selectForUpdate(@Param("sessionId") Long sessionId);

    @Select("""
            SELECT *
            FROM game_session
            WHERE mode = 'PK'
              AND match_mode = 'ROOM'
              AND invite_code = #{inviteCode}
              AND status IN ('WAITING', 'READY')
            FOR UPDATE
            """)
    GameSession selectRoomForUpdate(@Param("inviteCode") String inviteCode);

    @Select("""
            SELECT *
            FROM game_session
            WHERE status IN ('WAITING', 'READY', 'IN_PROGRESS')
              AND expires_at IS NOT NULL
              AND expires_at <= #{now}
            ORDER BY expires_at, id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<GameSession> selectExpiredBatchForUpdate(
            @Param("now") OffsetDateTime now,
            @Param("limit") int limit);
}
