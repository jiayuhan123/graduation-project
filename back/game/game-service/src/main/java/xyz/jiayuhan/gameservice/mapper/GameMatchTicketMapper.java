package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import xyz.jiayuhan.gameservice.entity.GameMatchTicket;

import java.time.OffsetDateTime;

public interface GameMatchTicketMapper extends BaseMapper<GameMatchTicket> {

    @Select("""
            SELECT *
            FROM game_match_ticket
            WHERE student_id = #{studentId}
              AND status = 'WAITING'
            FOR UPDATE
            """)
    GameMatchTicket selectWaitingByStudentForUpdate(@Param("studentId") Long studentId);

    @Select("""
            SELECT *
            FROM game_match_ticket
            WHERE id = #{ticketId}
              AND student_id = #{studentId}
            FOR UPDATE
            """)
    GameMatchTicket selectOwnedForUpdate(
            @Param("ticketId") Long ticketId,
            @Param("studentId") Long studentId);

    @Select("""
            SELECT *
            FROM game_match_ticket
            WHERE status = 'WAITING'
              AND student_id <> #{studentId}
              AND grade_level = #{gradeLevel}
              AND knowledge_point_id = #{knowledgePointId}
              AND difficulty = #{difficulty}
              AND question_count = #{questionCount}
              AND time_limit_seconds = #{timeLimitSeconds}
              AND expires_at > #{now}
            ORDER BY created_at, id
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """)
    GameMatchTicket selectCandidateForUpdate(
            @Param("studentId") Long studentId,
            @Param("gradeLevel") Integer gradeLevel,
            @Param("knowledgePointId") Long knowledgePointId,
            @Param("difficulty") Integer difficulty,
            @Param("questionCount") Integer questionCount,
            @Param("timeLimitSeconds") Integer timeLimitSeconds,
            @Param("now") OffsetDateTime now);

    @Update("""
            UPDATE game_match_ticket
            SET status = 'EXPIRED', updated_at = #{now}, version = version + 1
            WHERE status = 'WAITING'
              AND expires_at <= #{now}
            """)
    int expireWaiting(@Param("now") OffsetDateTime now);
}
