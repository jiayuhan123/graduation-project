package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GameSessionQuestion;

public interface GameSessionQuestionMapper extends BaseMapper<GameSessionQuestion> {

    @Select("""
            SELECT *
            FROM game_session_question
            WHERE id = #{questionId}
              AND session_id = #{sessionId}
              AND student_id = #{studentId}
            FOR UPDATE
            """)
    GameSessionQuestion selectForUpdate(
            @Param("questionId") Long questionId,
            @Param("sessionId") Long sessionId,
            @Param("studentId") Long studentId);

    @Select("""
            SELECT id
            FROM game_session_question
            WHERE session_id = #{sessionId}
              AND student_id = #{studentId}
              AND grading_status = 'UNANSWERED'
            ORDER BY sequence_no
            LIMIT 1
            """)
    Long selectNextUnansweredId(
            @Param("sessionId") Long sessionId,
            @Param("studentId") Long studentId);
}
