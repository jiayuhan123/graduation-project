package xyz.jiayuhan.learningservice.support;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

class AnswerGraderTest {

    private final AnswerGrader grader = new AnswerGrader(JsonMapper.builder().build());

    @Test
    void shouldCompareEquivalentNumericFormats() {
        assertThat(grader.isCorrect("{\"value\":3.50}", "3.5")).isTrue();
        assertThat(grader.isCorrect("{\"value\":0.75}", "3/4")).isTrue();
        assertThat(grader.isCorrect("{\"value\":0.15}", "15%")).isTrue();
        assertThat(grader.isCorrect("{\"value\":\"2又3/4\"}", "2.75")).isTrue();
    }

    @Test
    void shouldCompareJudgementAndRejectWrongAnswer() {
        assertThat(grader.isCorrect("{\"value\":true}", "正确")).isTrue();
        assertThat(grader.isCorrect("{\"value\":false}", "√")).isFalse();
        assertThat(grader.isCorrect("{\"value\":24}", "42")).isFalse();
    }
}
