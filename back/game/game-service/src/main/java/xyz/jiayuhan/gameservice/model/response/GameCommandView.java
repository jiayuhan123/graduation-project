package xyz.jiayuhan.gameservice.model.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** 游戏命令接口返回的不可变结果。 */
public final class GameCommandView {

    private GameCommandView() {
    }

    public record MatchStatus(
            Long ticketId,
            String status,
            OffsetDateTime expiresAt,
            Long sessionId,
            GameView.SessionDetail session
    ) {
    }

    public record AnswerFeedback(
            Long sessionId,
            Long sessionQuestionId,
            boolean accepted,
            Boolean correct,
            BigDecimal score,
            Integer combo,
            BigDecimal progress,
            Integer answeredCount,
            Integer questionCount,
            Long nextSessionQuestionId,
            String sessionStatus,
            String resultType,
            Long winnerStudentId,
            boolean idempotentReplay
    ) {
    }
}
