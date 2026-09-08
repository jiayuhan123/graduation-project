package xyz.jiayuhan.learningservice.mq;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import xyz.jiayuhan.common.mq.MqTopology;
import xyz.jiayuhan.common.mq.event.GameLearningEvent;
import xyz.jiayuhan.learningservice.model.request.GameAnswerSyncCommand;
import xyz.jiayuhan.learningservice.model.request.GameSessionSyncCommand;
import xyz.jiayuhan.learningservice.service.LearningCommandService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

/** 幂等消费游戏结果，将其纳入答题、错题、掌握度和推荐闭环。 */
@Component
@RequiredArgsConstructor
public class GameLearningEventConsumer {

    private static final String ANSWER_GRADED = "game.answer.graded.v1";
    private static final String SESSION_COMPLETED = "game.session.completed.v1";

    private final LearningCommandService learningCommandService;

    @RabbitListener(queues = MqTopology.LEARNING_GAME_EVENT_QUEUE)
    public void onGameEvent(GameLearningEvent event) {
        Map<String, Object> data = event.data();
        OffsetDateTime occurredAt = OffsetDateTime.ofInstant(event.occurredAt(), ZoneOffset.UTC);
        switch (event.eventType()) {
            case ANSWER_GRADED -> learningCommandService.recordGameAnswer(
                    event.studentId(),
                    new GameAnswerSyncCommand(
                            longValue(data, "sessionId"),
                            longValue(data, "sessionQuestionId"),
                            intValue(data, "sequenceNo"),
                            longValue(data, "questionId"),
                            longValue(data, "knowledgePointId"),
                            intValue(data, "gradeLevel"),
                            intValue(data, "difficulty"),
                            intValue(data, "questionCount"),
                            intValue(data, "timeLimitSeconds"),
                            stringValue(data, "mode"),
                            stringValue(data, "studentAnswer"),
                            booleanValue(data, "correct"),
                            decimalValue(data, "score"),
                            intValue(data, "responseMs"),
                            occurredAt));
            case SESSION_COMPLETED -> learningCommandService.completeGameSession(
                    event.studentId(),
                    new GameSessionSyncCommand(
                            event.aggregateId(),
                            longValue(data, "knowledgePointId"),
                            intValue(data, "gradeLevel"),
                            intValue(data, "difficulty"),
                            intValue(data, "questionCount"),
                            intValue(data, "timeLimitSeconds"),
                            stringValue(data, "mode"),
                            stringValue(data, "result"),
                            occurredAt));
            default -> throw new IllegalArgumentException("不支持的游戏学习事件: " + event.eventType());
        }
    }

    private Long longValue(Map<String, Object> data, String key) {
        return numberValue(data, key).longValue();
    }

    private Integer intValue(Map<String, Object> data, String key) {
        return numberValue(data, key).intValue();
    }

    private Number numberValue(Map<String, Object> data, String key) {
        Object value = required(data, key);
        if (value instanceof Number number) {
            return number;
        }
        try {
            return new BigDecimal(value.toString());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("游戏事件字段格式错误: " + key, exception);
        }
    }

    private BigDecimal decimalValue(Map<String, Object> data, String key) {
        return new BigDecimal(required(data, key).toString());
    }

    private Boolean booleanValue(Map<String, Object> data, String key) {
        Object value = required(data, key);
        return value instanceof Boolean bool ? bool : Boolean.valueOf(value.toString());
    }

    private String stringValue(Map<String, Object> data, String key) {
        return required(data, key).toString();
    }

    private Object required(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            throw new IllegalArgumentException("游戏事件缺少字段: " + key);
        }
        return value;
    }
}
