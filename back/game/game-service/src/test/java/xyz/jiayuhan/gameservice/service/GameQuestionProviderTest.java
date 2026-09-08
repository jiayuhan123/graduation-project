package xyz.jiayuhan.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.gameservice.mapper.GameLevelMapper;
import xyz.jiayuhan.gameservice.mapper.GameLevelQuestionMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameQuestionProviderTest {

    private static final Long KNOWLEDGE_POINT_ID = 9201L;

    @Mock private GameLevelMapper levelMapper;
    @Mock private GameLevelQuestionMapper levelQuestionMapper;
    @Mock private ContentFeignClient contentFeignClient;

    private GameQuestionProvider provider;

    @BeforeEach
    void setUp() {
        provider = new GameQuestionProvider(levelMapper, levelQuestionMapper, contentFeignClient);
    }

    @Test
    void preparesPkFromEnabledFixedPoolAndMakesScoresTotalOneHundred() {
        when(contentFeignClient.getKnowledgePoint(KNOWLEDGE_POINT_ID))
                .thenReturn(Result.success(new KnowledgePointResponse(
                        KNOWLEDGE_POINT_ID, 1L, null, "KP", "整数计算", null, 3, 1, 1)));
        when(contentFeignClient.pageQuestions(
                1, 100, null, KNOWLEDGE_POINT_ID, "FIXED", null, 3))
                .thenReturn(Result.success(new PageResult<>(
                        1, 100, 3, 1, List.of(
                                question(1L, "{\"value\":1}"),
                                question(2L, "{\"value\":2}"),
                                question(3L, "{\"text\":\"3/4\"}")))));

        GameQuestionProvider.PreparedPk result = provider.preparePk(
                3, KNOWLEDGE_POINT_ID, 3, 3, 180);

        assertThat(result.questions()).hasSize(3);
        assertThat(result.questions().stream()
                .map(GameQuestionProvider.PreparedQuestion::maxScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

    @Test
    void rejectsInsufficientFixedPool() {
        when(contentFeignClient.getKnowledgePoint(KNOWLEDGE_POINT_ID))
                .thenReturn(Result.success(new KnowledgePointResponse(
                        KNOWLEDGE_POINT_ID, 1L, null, "KP", "整数计算", null, 3, 1, 1)));
        when(contentFeignClient.pageQuestions(
                1, 100, null, KNOWLEDGE_POINT_ID, "FIXED", null, 3))
                .thenReturn(Result.success(new PageResult<>(
                        1, 100, 1, 1, List.of(question(1L, "{\"value\":1}")))));
        when(contentFeignClient.pageQuestions(
                1, 100, null, KNOWLEDGE_POINT_ID, "FIXED", null, null))
                .thenReturn(Result.success(new PageResult<>(
                        1, 100, 1, 1, List.of(question(1L, "{\"value\":1}")))));

        assertThatThrownBy(() -> provider.preparePk(3, KNOWLEDGE_POINT_ID, 3, 2, 180))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("题量不足");
    }

    @Test
    void defaultsPkToOneQuestionForTheCurrentSparseFixedPool() {
        when(contentFeignClient.getKnowledgePoint(KNOWLEDGE_POINT_ID))
                .thenReturn(Result.success(new KnowledgePointResponse(
                        KNOWLEDGE_POINT_ID, 1L, null, "KP", "整数计算", null, 3, 1, 1)));
        when(contentFeignClient.pageQuestions(
                1, 100, null, KNOWLEDGE_POINT_ID, "FIXED", null, 3))
                .thenReturn(Result.success(new PageResult<>(
                        1, 100, 1, 1, List.of(question(1L, "{\"value\":1}")))));

        GameQuestionProvider.PreparedPk result = provider.preparePk(
                3, KNOWLEDGE_POINT_ID, 3, null, null);

        assertThat(result.questionCount()).isEqualTo(1);
        assertThat(result.timeLimitSeconds()).isEqualTo(180);
        assertThat(result.questions()).hasSize(1);
    }

    private QuestionResponse question(Long id, String answer) {
        return new QuestionResponse(
                id, "Q-" + id, "FIXED", "ARITHMETIC", "计算题", null,
                answer, "解析", null, 3, 20, "ENABLED", List.of(KNOWLEDGE_POINT_ID));
    }
}
