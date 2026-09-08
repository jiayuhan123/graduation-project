package xyz.jiayuhan.gameservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;
import xyz.jiayuhan.gameservice.entity.GameLevel;
import xyz.jiayuhan.gameservice.entity.GameLevelProgress;
import xyz.jiayuhan.gameservice.entity.GamePlayerProgress;
import xyz.jiayuhan.gameservice.entity.GameRewardRecord;
import xyz.jiayuhan.gameservice.entity.GameSession;
import xyz.jiayuhan.gameservice.entity.GameSessionMember;
import xyz.jiayuhan.gameservice.entity.GameSessionQuestion;
import xyz.jiayuhan.gameservice.mapper.GameLevelMapper;
import xyz.jiayuhan.gameservice.mapper.GameLevelProgressMapper;
import xyz.jiayuhan.gameservice.mapper.GamePlayerProgressMapper;
import xyz.jiayuhan.gameservice.mapper.GameRewardRecordMapper;
import xyz.jiayuhan.gameservice.mapper.GameSessionMapper;
import xyz.jiayuhan.gameservice.mapper.GameSessionMemberMapper;
import xyz.jiayuhan.gameservice.mapper.GameSessionQuestionMapper;
import xyz.jiayuhan.gameservice.model.response.GameView;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/** 对局查询通过成员表验证归属，只返回当前学生自己的题目过程。 */
@Service
@RequiredArgsConstructor
public class GameQueryServiceImpl implements GameQueryService {

    private final GameLevelMapper gameLevelMapper;
    private final GameSessionMapper gameSessionMapper;
    private final GameSessionMemberMapper gameSessionMemberMapper;
    private final GameSessionQuestionMapper gameSessionQuestionMapper;
    private final GamePlayerProgressMapper gamePlayerProgressMapper;
    private final GameLevelProgressMapper gameLevelProgressMapper;
    private final GameRewardRecordMapper gameRewardRecordMapper;
    private final ObjectMapper objectMapper;

    @Override
    public PageResult<GameView.Level> pageLevels(
            long page, long size, Integer gradeLevel, Long knowledgePointId, String mode) {
        Page<GameLevel> result = gameLevelMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<GameLevel>()
                        .eq(GameLevel::getStatus, "ENABLED")
                        .eq(gradeLevel != null, GameLevel::getGradeLevel, gradeLevel)
                        .eq(knowledgePointId != null, GameLevel::getKnowledgePointId, knowledgePointId)
                        .and(mode != null, wrapper -> wrapper
                                .eq(GameLevel::getMode, mode)
                                .or()
                                .eq(GameLevel::getMode, "BOTH"))
                        .orderByAsc(GameLevel::getSortOrder)
                        .orderByAsc(GameLevel::getId));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    @Override
    public GameView.Level getLevel(Long levelId) {
        GameLevel level = gameLevelMapper.selectOne(new LambdaQueryWrapper<GameLevel>()
                .eq(GameLevel::getId, levelId)
                .eq(GameLevel::getStatus, "ENABLED"));
        if (level == null) {
            throw new BusinessException(ResultCode.GAME_LEVEL_NOT_FOUND);
        }
        return toView(level);
    }

    @Override
    public PageResult<GameSessionSummaryResponse> pageMySessions(
            Long studentId, long page, long size, String status) {
        List<Long> statusSessionIds = null;
        if (status != null) {
            statusSessionIds = gameSessionMapper.selectList(new LambdaQueryWrapper<GameSession>()
                            .select(GameSession::getId)
                            .eq(GameSession::getStatus, status))
                    .stream()
                    .map(GameSession::getId)
                    .toList();
            if (statusSessionIds.isEmpty()) {
                return new PageResult<>(page, size, 0, 0, List.of());
            }
        }
        Page<GameSessionMember> memberships = gameSessionMemberMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<GameSessionMember>()
                        .eq(GameSessionMember::getStudentId, studentId)
                        .in(statusSessionIds != null, GameSessionMember::getSessionId, statusSessionIds)
                        .orderByDesc(GameSessionMember::getJoinedAt));
        List<Long> ids = memberships.getRecords().stream()
                .map(GameSessionMember::getSessionId)
                .toList();
        Map<Long, GameSession> sessions = ids.isEmpty()
                ? Map.of()
                : gameSessionMapper.selectByIds(ids)
                        .stream()
                        .collect(Collectors.toMap(GameSession::getId, Function.identity()));
        List<GameSessionSummaryResponse> records = ids.stream()
                .map(sessions::get)
                .filter(java.util.Objects::nonNull)
                .map(this::toSummary)
                .toList();
        return pageResult(memberships, records);
    }

    @Override
    public GameView.SessionDetail getSession(Long studentId, Long sessionId) {
        boolean member = gameSessionMemberMapper.exists(new LambdaQueryWrapper<GameSessionMember>()
                .eq(GameSessionMember::getSessionId, sessionId)
                .eq(GameSessionMember::getStudentId, studentId));
        if (!member) {
            throw new BusinessException(ResultCode.GAME_SESSION_NOT_FOUND);
        }
        GameSession session = requireSession(sessionId);
        List<GameView.SessionMember> members = gameSessionMemberMapper.selectList(
                        new LambdaQueryWrapper<GameSessionMember>()
                                .eq(GameSessionMember::getSessionId, sessionId)
                                .orderByAsc(GameSessionMember::getSeatNo))
                .stream()
                .map(this::toView)
                .toList();
        List<GameView.SessionQuestion> questions = gameSessionQuestionMapper.selectList(
                        new LambdaQueryWrapper<GameSessionQuestion>()
                                .eq(GameSessionQuestion::getSessionId, sessionId)
                                .eq(GameSessionQuestion::getStudentId, studentId)
                                .orderByAsc(GameSessionQuestion::getSequenceNo))
                .stream()
                .map(this::toView)
                .toList();
        return new GameView.SessionDetail(toSummary(session), members, questions);
    }

    @Override
    public GameSessionSummaryResponse getSessionInternal(Long sessionId) {
        return toSummary(requireSession(sessionId));
    }

    @Override
    public GameView.PlayerProgress getPlayerProgress(Long studentId) {
        GamePlayerProgress progress = gamePlayerProgressMapper.selectById(studentId);
        if (progress == null) {
            return new GameView.PlayerProgress(
                    studentId, 0L, 0L, 1, 0, 0, 0,
                    0, 0, 0, 0, 1000, 0, 0, 0, null);
        }
        return new GameView.PlayerProgress(
                progress.getStudentId(), progress.getTotalPoints(), progress.getExperience(),
                progress.getPlayerLevel(), progress.getTotalSessions(), progress.getSuccessSessions(),
                progress.getLevelPassedCount(), progress.getPkSessions(), progress.getPkWins(),
                progress.getPkLosses(), progress.getPkDraws(), progress.getPkRating(),
                progress.getCurrentWinStreak(), progress.getBestWinStreak(), progress.getBestCombo(),
                progress.getLastPlayedAt());
    }

    @Override
    public List<GameView.LevelProgress> listLevelProgress(Long studentId, String status) {
        return gameLevelProgressMapper.selectList(new LambdaQueryWrapper<GameLevelProgress>()
                        .eq(GameLevelProgress::getStudentId, studentId)
                        .eq(status != null, GameLevelProgress::getStatus, status)
                        .orderByDesc(GameLevelProgress::getLastPlayedAt)
                        .orderByAsc(GameLevelProgress::getLevelId))
                .stream()
                .map(this::toView)
                .toList();
    }

    @Override
    public PageResult<GameView.Reward> pageRewards(Long studentId, long page, long size) {
        Page<GameRewardRecord> result = gameRewardRecordMapper.selectPage(
                new Page<>(page, size),
                new LambdaQueryWrapper<GameRewardRecord>()
                        .eq(GameRewardRecord::getStudentId, studentId)
                        .orderByDesc(GameRewardRecord::getCreatedAt)
                        .orderByDesc(GameRewardRecord::getId));
        return pageResult(result, result.getRecords().stream().map(this::toView).toList());
    }

    private GameSession requireSession(Long sessionId) {
        GameSession session = gameSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.GAME_SESSION_NOT_FOUND);
        }
        return session;
    }

    private GameView.Level toView(GameLevel level) {
        return new GameView.Level(
                level.getId(), level.getCode(), level.getName(), level.getDescription(),
                level.getGradeLevel(), level.getKnowledgePointId(), level.getDifficulty(),
                level.getQuestionSource(), level.getMode(),
                level.getQuestionCount(), level.getTimeLimitSeconds(), level.getPassRate(),
                level.getMinPlayers(), level.getMaxPlayers(), level.getBasePoints(),
                level.getBaseExperience(), level.getPreviousLevelId(), level.getSortOrder(),
                level.getStatus(), level.getRuleConfig());
    }

    private GameSessionSummaryResponse toSummary(GameSession session) {
        return new GameSessionSummaryResponse(
                session.getId(), session.getLevelId(), session.getMode(),
                session.getCreatorStudentId(), session.getMatchMode(), session.getInviteCode(),
                session.getStatus(), session.getMemberCount(), session.getGradeLevelSnapshot(),
                session.getKnowledgePointIdSnapshot(), session.getDifficultySnapshot(),
                session.getQuestionCountSnapshot(), session.getTimeLimitSecondsSnapshot(),
                session.getPassRateSnapshot(), session.getTeamProgress(), session.getWinnerStudentId(),
                session.getResultType(), session.getStartedAt(), session.getEndedAt(),
                session.getExpiresAt(), session.getSettledAt(), session.getResultDetail());
    }

    private GameView.SessionMember toView(GameSessionMember member) {
        return new GameView.SessionMember(
                member.getStudentId(), member.getSeatNo(), member.getMemberRole(), member.getStatus(),
                member.getResult(),
                member.getPracticeTaskId(), member.getAnsweredCount(), member.getCorrectCount(),
                member.getScore(), member.getContributionValue(), member.getCurrentCombo(),
                member.getMaxCombo(), member.getTotalResponseMs(), member.getRatingBefore(),
                member.getRatingAfter(), member.getPointsReward(), member.getExperienceReward(),
                member.getJoinedAt(), member.getLastAnsweredAt(), member.getFinishedAt());
    }

    private GameView.SessionQuestion toView(GameSessionQuestion question) {
        QuestionResponse snapshot = readQuestionSnapshot(question.getQuestionSnapshot());
        return new GameView.SessionQuestion(
                question.getId(), question.getSequenceNo(), question.getQuestionId(),
                snapshot.questionType(), snapshot.stem(), snapshot.options(),
                question.getGradingStatus(), question.getStudentAnswer(),
                question.getAnswerRecordId(), question.getCorrect(),
                question.getScore(), question.getResponseMs(), question.getComboAfter(),
                question.getProgressDelta(), question.getEnergyDelta(), question.getAnsweredAt());
    }

    private QuestionResponse readQuestionSnapshot(String json) {
        try {
            return objectMapper.readValue(json, QuestionResponse.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "游戏题目快照损坏");
        }
    }

    private GameView.LevelProgress toView(GameLevelProgress progress) {
        return new GameView.LevelProgress(
                progress.getLevelId(), progress.getStatus(), progress.getAttemptCount(),
                progress.getBestScore(), progress.getBestCorrectCount(), progress.getBestStarCount(),
                progress.getBestDurationSeconds(), progress.getLastSessionId(),
                progress.getFirstUnlockedAt(), progress.getFirstPassedAt(),
                progress.getLastPlayedAt());
    }

    private GameView.Reward toView(GameRewardRecord reward) {
        return new GameView.Reward(
                reward.getId(), reward.getSessionId(), reward.getBusinessType(),
                reward.getRewardType(), reward.getAmount(), reward.getBalanceAfter(),
                reward.getReason(), reward.getCreatedAt());
    }

    private <T> PageResult<T> pageResult(Page<?> page, List<T> records) {
        return new PageResult<>(page.getCurrent(), page.getSize(), page.getTotal(), page.getPages(), records);
    }
}
