package xyz.jiayuhan.gameservice.support;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class GameAnswerGraderTest {

    private final GameAnswerGrader grader = new GameAnswerGrader(JsonMapper.builder().build());

    @Test
    void normalizesNumbersFractionsPercentagesAndJudgements() {
        assertThat(grader.isCorrect("{\"value\":0.75}", "3/4")).isTrue();
        assertThat(grader.isCorrect("{\"value\":0.8}", "80%")).isTrue();
        assertThat(grader.isCorrect("{\"text\":\"3/7\"}", "3/7")).isTrue();
        assertThat(grader.isCorrect("{\"value\":true}", "√")).isTrue();
        assertThat(grader.isCorrect("{\"value\":42}", "41")).isFalse();
    }
}
