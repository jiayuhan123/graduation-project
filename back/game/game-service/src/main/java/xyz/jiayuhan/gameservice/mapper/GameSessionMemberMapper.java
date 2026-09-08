package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GameSessionMember;

public interface GameSessionMemberMapper extends BaseMapper<GameSessionMember> {

    @Select("""
            SELECT s.id
            FROM game_session s
            JOIN game_session_member m ON m.session_id = s.id
            WHERE m.student_id = #{studentId}
              AND m.status IN ('JOINED', 'READY', 'PLAYING')
              AND s.status IN ('WAITING', 'READY', 'IN_PROGRESS')
            ORDER BY s.created_at DESC
            LIMIT 1
            """)
    Long selectActiveSessionId(@Param("studentId") Long studentId);
}
