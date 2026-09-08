package xyz.jiayuhan.learningservice.mq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.mq.event.GameLearningEvent;
import xyz.jiayuhan.learningservice.model.request.GameAnswerSyncCommand;
import xyz.jiayuhan.learningservice.service.LearningCommandService;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class GameLearningEventConsumerTest {

    @Mock private LearningCommandService learningCommandService;

    @Test
    void convertsGameAnswerToLearningCommand() {
        GameLearningEventConsumer consumer = new GameLearningEventConsumer(learningCommandService);
        Instant occurredAt = Instant.parse("2026-09-08T08:00:00Z");
        GameLearningEvent event = new GameLearningEvent(
                UUID.randomUUID(), "game.answer.graded.v1", occurredAt, "game-service",
                "ANSWER", 301L, 21L,
                Map.ofEntries(
                        Map.entry("sessionId", 101L),
                        Map.entry("sessionQuestionId", 301L),
                        Map.entry("sequenceNo", 1),
                        Map.entry("questionId", 401L),
                        Map.entry("knowledgePointId", 501L),
                        Map.entry("gradeLevel", 3),
                        Map.entry("difficulty", 2),
                        Map.entry("questionCount", 5),
                        Map.entry("timeLimitSeconds", 180),
                        Map.entry("mode", "PK"),
                        Map.entry("studentAnswer", "42"),
                        Map.entry("correct", true),
                        Map.entry("score", new BigDecimal("1.00")),
                        Map.entry("responseMs", 1200)));

        consumer.onGameEvent(event);

        ArgumentCaptor<GameAnswerSyncCommand> command = ArgumentCaptor.forClass(GameAnswerSyncCommand.class);
        verify(learningCommandService).recordGameAnswer(org.mockito.ArgumentMatchers.eq(21L), command.capture());
        assertThat(command.getValue().sessionId()).isEqualTo(101L);
        assertThat(command.getValue().studentAnswer()).isEqualTo("42");
        assertThat(command.getValue().occurredAt().toInstant()).isEqualTo(occurredAt);
    }
}
