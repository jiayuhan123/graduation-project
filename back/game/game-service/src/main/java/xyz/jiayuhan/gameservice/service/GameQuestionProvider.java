package xyz.jiayuhan.gameservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.contentapi.client.ContentFeignClient;
import xyz.jiayuhan.contentapi.model.request.BatchQuestionQueryRequest;
import xyz.jiayuhan.contentapi.model.response.KnowledgePointResponse;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.gameservice.entity.GameLevel;
import xyz.jiayuhan.gameservice.entity.GameLevelQuestion;
import xyz.jiayuhan.gameservice.mapper.GameLevelMapper;
import xyz.jiayuhan.gameservice.mapper.GameLevelQuestionMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** 从内容服务的已启用固定题库准备闯关和 PK 共用的不可变题目快照。 */
@Service
@RequiredArgsConstructor
public class GameQuestionProvider {

    static final int DEFAULT_PK_QUESTION_COUNT = 1;
    static final int DEFAULT_PK_TIME_LIMIT_SECONDS = 180;
    private static final BigDecimal TOTAL_SCORE = new BigDecimal("100.00");

    private final GameLevelMapper gameLevelMapper;
    private final GameLevelQuestionMapper gameLevelQuestionMapper;
    private final ContentFeignClient contentFeignClient;

    public PreparedLevel prepareLevel(Long levelId) {
        GameLevel level = gameLevelMapper.selectOne(new LambdaQueryWrapper<GameLevel>()
                .eq(GameLevel::getId, levelId)
                .eq(GameLevel::getStatus, "ENABLED"));
        if (level == null) {
            throw new BusinessException(ResultCode.GAME_LEVEL_NOT_FOUND);
        }
        List<QuestionResponse> selected = "CURATED".equals(level.getQuestionSource())
                ? curatedQuestions(level)
                : knowledgePointQuestions(
                        level.getKnowledgePointId(), level.getDifficulty(), level.getQuestionCount());
        return new PreparedLevel(level, withScores(selected));
    }

    public PreparedPk preparePk(
            Integer gradeLevel,
            Long knowledgePointId,
            Integer difficulty,
            Integer questionCount,
            Integer timeLimitSeconds) {
        KnowledgePointResponse knowledgePoint = requireRemote(
                () -> contentFeignClient.getKnowledgePoint(knowledgePointId),
                "暂时无法读取PK知识点");
        if (!Integer.valueOf(1).equals(knowledgePoint.status())) {
            throw new BusinessException(ResultCode.KNOWLEDGE_POINT_NOT_FOUND);
        }
        int normalizedCount = questionCount == null ? DEFAULT_PK_QUESTION_COUNT : questionCount;
        int normalizedTime = timeLimitSeconds == null
                ? DEFAULT_PK_TIME_LIMIT_SECONDS
                : timeLimitSeconds;
        List<QuestionResponse> selected = knowledgePointQuestions(
                knowledgePointId, difficulty, normalizedCount);
        return new PreparedPk(
                gradeLevel,
                knowledgePointId,
                difficulty,
                normalizedCount,
                normalizedTime,
                withScores(selected));
    }

    private List<QuestionResponse> curatedQuestions(GameLevel level) {
        List<GameLevelQuestion> relations = gameLevelQuestionMapper.selectList(
                new LambdaQueryWrapper<GameLevelQuestion>()
                        .eq(GameLevelQuestion::getLevelId, level.getId())
                        .orderByDesc(GameLevelQuestion::getRequired)
                        .orderByAsc(GameLevelQuestion::getSequenceNo)
                        .orderByAsc(GameLevelQuestion::getQuestionId));
        if (relations.size() < level.getQuestionCount()) {
            throw insufficientPool(level.getQuestionCount(), relations.size());
        }
        long requiredCount = relations.stream().filter(GameLevelQuestion::getRequired).count();
        if (requiredCount > level.getQuestionCount()) {
            throw new BusinessException(ResultCode.CONFLICT, "关卡必选题数量超过关卡题量");
        }
        List<Long> ids = relations.stream()
                .limit(level.getQuestionCount())
                .map(GameLevelQuestion::getQuestionId)
                .toList();
        List<QuestionResponse> questions = requireRemote(
                () -> contentFeignClient.getQuestions(new BatchQuestionQueryRequest(ids)),
                "暂时无法读取关卡固定题池");
        Map<Long, QuestionResponse> byId = new LinkedHashMap<>();
        questions.stream().filter(this::isUsableFixedQuestion)
                .forEach(question -> byId.put(question.id(), question));
        List<QuestionResponse> ordered = ids.stream().map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList();
        if (ordered.size() != ids.size()) {
            throw new BusinessException(ResultCode.CONFLICT, "关卡题池包含未启用或不可判题的题目");
        }
        return ordered;
    }

    private List<QuestionResponse> knowledgePointQuestions(
            Long knowledgePointId, Integer difficulty, int requestedCount) {
        Map<Long, QuestionResponse> candidates = new LinkedHashMap<>();
        PageResult<QuestionResponse> exact = requireRemote(
                () -> contentFeignClient.pageQuestions(
                        1, 100, null, knowledgePointId, "FIXED", null, difficulty),
                "暂时无法读取固定题库");
        exact.records().stream().filter(this::isUsableFixedQuestion)
                .forEach(question -> candidates.put(question.id(), question));
        if (candidates.size() < requestedCount) {
            PageResult<QuestionResponse> fallback = requireRemote(
                    () -> contentFeignClient.pageQuestions(
                            1, 100, null, knowledgePointId, "FIXED", null, null),
                    "暂时无法读取固定题库");
            fallback.records().stream().filter(this::isUsableFixedQuestion)
                    .forEach(question -> candidates.putIfAbsent(question.id(), question));
        }
        if (candidates.size() < requestedCount) {
            throw insufficientPool(requestedCount, candidates.size());
        }
        List<QuestionResponse> selected = new ArrayList<>(candidates.values());
        Collections.shuffle(selected);
        if (difficulty != null) {
            selected.sort(Comparator.comparingInt(
                    question -> difficulty.equals(question.difficulty()) ? 0 : 1));
        }
        return List.copyOf(selected.subList(0, requestedCount));
    }

    private List<PreparedQuestion> withScores(List<QuestionResponse> questions) {
        BigDecimal regular = TOTAL_SCORE.divide(
                BigDecimal.valueOf(questions.size()), 2, RoundingMode.DOWN);
        List<PreparedQuestion> prepared = new ArrayList<>(questions.size());
        BigDecimal assigned = BigDecimal.ZERO;
        for (int index = 0; index < questions.size(); index++) {
            BigDecimal score = index == questions.size() - 1
                    ? TOTAL_SCORE.subtract(assigned)
                    : regular;
            prepared.add(new PreparedQuestion(questions.get(index), score));
            assigned = assigned.add(score);
        }
        return List.copyOf(prepared);
    }

    private boolean isUsableFixedQuestion(QuestionResponse question) {
        return question != null
                && question.id() != null
                && "FIXED".equals(question.questionMode())
                && "ENABLED".equals(question.status())
                && question.standardAnswer() != null
                && !question.standardAnswer().isBlank();
    }

    private BusinessException insufficientPool(int requested, int available) {
        return new BusinessException(
                ResultCode.PRACTICE_QUESTION_POOL_EMPTY,
                "固定题库题量不足：需要" + requested + "题，当前可用" + available + "题");
    }

    private <T> T requireRemote(Supplier<Result<T>> request, String message) {
        Result<T> result;
        try {
            result = request.get();
        } catch (RuntimeException exception) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        if (result == null || !Integer.valueOf(200).equals(result.getCode())
                || result.getData() == null) {
            throw new BusinessException(ResultCode.SERVICE_UNAVAILABLE, message);
        }
        return result.getData();
    }

    public record PreparedQuestion(QuestionResponse question, BigDecimal maxScore) {
    }

    public record PreparedLevel(GameLevel level, List<PreparedQuestion> questions) {
    }

    public record PreparedPk(
            Integer gradeLevel,
            Long knowledgePointId,
            Integer difficulty,
            Integer questionCount,
            Integer timeLimitSeconds,
            List<PreparedQuestion> questions
    ) {
    }
}
