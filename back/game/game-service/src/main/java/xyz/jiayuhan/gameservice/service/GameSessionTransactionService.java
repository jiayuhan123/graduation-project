package xyz.jiayuhan.gameservice.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.contentapi.model.response.QuestionResponse;
import xyz.jiayuhan.gameservice.entity.GameLevel;
import xyz.jiayuhan.gameservice.entity.GameLevelProgress;
import xyz.jiayuhan.gameservice.entity.GameMatchTicket;
import xyz.jiayuhan.gameservice.entity.GamePlayerProgress;
import xyz.jiayuhan.gameservice.entity.GameRewardRecord;
import xyz.jiayuhan.gameservice.entity.GameSession;
import xyz.jiayuhan.gameservice.entity.GameSessionMember;
import xyz.jiayuhan.gameservice.entity.GameSessionQuestion;
import xyz.jiayuhan.gameservice.event.GameEventOutboxService;
import xyz.jiayuhan.gameservice.mapper.GameLevelMapper;
import xyz.jiayuhan.gameservice.mapper.GameLevelProgressMapper;
import xyz.jiayuhan.gameservice.mapper.GameMatchTicketMapper;
import xyz.jiayuhan.gameservice.mapper.GamePlayerProgressMapper;
import xyz.jiayuhan.gameservice.mapper.GameRewardRecordMapper;
import xyz.jiayuhan.gameservice.mapper.GameSessionMapper;
import xyz.jiayuhan.gameservice.mapper.GameSessionMemberMapper;
import xyz.jiayuhan.gameservice.mapper.GameSessionQuestionMapper;
import xyz.jiayuhan.gameservice.model.request.SubmitGameAnswerRequest;
import xyz.jiayuhan.gameservice.model.response.GameCommandView;
import xyz.jiayuhan.gameservice.support.GameAnswerGrader;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 对游戏聚合加行锁，原子完成对局推进、结算、成长与奖励。 */
@Service
@RequiredArgsConstructor
public class GameSessionTransactionService {

    private static final String LEVEL = "LEVEL";
    private static final String PK = "PK";
    private static final String WAITING = "WAITING";
    private static final String READY = "READY";
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String COMPLETED = "COMPLETED";
    private static final int PK_WIN_POINTS = 100;
    private static final int PK_LOSE_POINTS = 20;
    private static final int PK_DRAW_POINTS = 50;
    private static final int PK_WIN_EXPERIENCE = 50;
    private static final int PK_LOSE_EXPERIENCE = 20;
    private static final int PK_DRAW_EXPERIENCE = 30;

    private final GameLevelMapper levelMapper;
    private final GameSessionMapper sessionMapper;
    private final GameMatchTicketMapper matchTicketMapper;
    private final GameSessionMemberMapper memberMapper;
    private final GameSessionQuestionMapper questionMapper;
    private final GamePlayerProgressMapper playerProgressMapper;
    private final GameLevelProgressMapper levelProgressMapper;
    private final GameRewardRecordMapper rewardRecordMapper;
    private final GameEventOutboxService outboxService;
    private final ObjectMapper objectMapper;
    private final GameAnswerGrader answerGrader;

    @Transactional(readOnly = true)
    public Long findActiveSessionId(Long studentId) {
        return memberMapper.selectActiveSessionId(studentId);
    }

    @Transactional(readOnly = true)
    public GameMatchTicket findWaitingTicket(Long studentId) {
        return matchTicketMapper.selectOne(new LambdaQueryWrapper<GameMatchTicket>()
                .eq(GameMatchTicket::getStudentId, studentId)
                .eq(GameMatchTicket::getStatus, WAITING)
                .gt(GameMatchTicket::getExpiresAt, now()));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createLevelSession(
            Long studentId,
            GameQuestionProvider.PreparedLevel prepared) {
        requireNoActiveSession(studentId);
        GameLevel level = levelMapper.selectForShare(prepared.level().getId());
        if (level == null || !"ENABLED".equals(level.getStatus())
                || !level.getVersion().equals(prepared.level().getVersion())
                || level.getQuestionCount() != prepared.questions().size()) {
            throw new BusinessException(ResultCode.CONFLICT, "关卡配置已发生变化，请重新进入");
        }
        requireLevelUnlocked(studentId, level);
        OffsetDateTime now = now();
        GameSession session = newSession(
                LEVEL,
                level.getId(),
                studentId,
                null,
                null,
                READY,
                1,
                level.getGradeLevel(),
                level.getKnowledgePointId(),
                level.getDifficulty(),
                prepared.questions().size(),
                level.getTimeLimitSeconds(),
                level.getPassRate(),
                now.plusMinutes(15));
        sessionMapper.insert(session);
        memberMapper.insert(newMember(session.getId(), studentId, 1, "CREATOR", READY, now));
        insertQuestions(session.getId(), studentId, prepared.questions());
        return session.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long createPkRoom(
            Long studentId,
            String inviteCode,
            GameQuestionProvider.PreparedPk prepared) {
        requireNoActiveSession(studentId);
        OffsetDateTime now = now();
        GameSession session = newSession(
                PK,
                null,
                studentId,
                "ROOM",
                inviteCode,
                WAITING,
                1,
                prepared.gradeLevel(),
                prepared.knowledgePointId(),
                prepared.difficulty(),
                prepared.questionCount(),
                prepared.timeLimitSeconds(),
                null,
                now.plusMinutes(5));
        sessionMapper.insert(session);
        memberMapper.insert(newMember(session.getId(), studentId, 1, "CREATOR", "JOINED", now));
        insertQuestions(session.getId(), studentId, prepared.questions());
        return session.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long joinPkRoom(Long studentId, String inviteCode) {
        requireNoActiveSession(studentId);
        GameSession session = sessionMapper.selectRoomForUpdate(inviteCode);
        if (session == null || !WAITING.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.GAME_SESSION_NOT_FOUND, "PK房间不存在或已关闭");
        }
        OffsetDateTime now = now();
        if (session.getExpiresAt() != null && !now.isBefore(session.getExpiresAt())) {
            throw new BusinessException(ResultCode.CONFLICT, "PK房间已经过期");
        }
        if (studentId.equals(session.getCreatorStudentId())) {
            return session.getId();
        }
        GameSessionMember opponent = newMember(
                session.getId(), studentId, 2, "OPPONENT", READY, now);
        memberMapper.insert(opponent);
        copyQuestions(session.getId(), session.getCreatorStudentId(), studentId);
        updateAllMemberStatuses(session.getId(), READY, now);
        session.setMemberCount(2);
        session.setStatus(READY);
        session.setExpiresAt(now.plusMinutes(2));
        assertUpdated(sessionMapper.updateById(session));
        return session.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public MatchTransactionResult matchRandom(
            Long studentId,
            GameQuestionProvider.PreparedPk prepared) {
        Long activeSessionId = memberMapper.selectActiveSessionId(studentId);
        if (activeSessionId != null) {
            return new MatchTransactionResult(null, "MATCHED", null, activeSessionId);
        }
        OffsetDateTime now = now();
        matchTicketMapper.expireWaiting(now);
        GameMatchTicket ownWaiting = matchTicketMapper.selectWaitingByStudentForUpdate(studentId);
        if (ownWaiting != null) {
            requireSameMatchRule(ownWaiting, prepared);
            return toMatchResult(ownWaiting);
        }
        GameMatchTicket candidate = matchTicketMapper.selectCandidateForUpdate(
                studentId,
                prepared.gradeLevel(),
                prepared.knowledgePointId(),
                prepared.difficulty(),
                prepared.questionCount(),
                prepared.timeLimitSeconds(),
                now);
        if (candidate == null) {
            GameMatchTicket waiting = newTicket(studentId, prepared, WAITING, now.plusMinutes(2));
            matchTicketMapper.insert(waiting);
            return toMatchResult(waiting);
        }

        GameSession session = newSession(
                PK,
                null,
                candidate.getStudentId(),
                "RANDOM",
                null,
                READY,
                2,
                prepared.gradeLevel(),
                prepared.knowledgePointId(),
                prepared.difficulty(),
                prepared.questionCount(),
                prepared.timeLimitSeconds(),
                null,
                now.plusMinutes(2));
        sessionMapper.insert(session);
        memberMapper.insert(newMember(
                session.getId(), candidate.getStudentId(), 1, "CREATOR", READY, now));
        memberMapper.insert(newMember(session.getId(), studentId, 2, "OPPONENT", READY, now));
        insertQuestions(session.getId(), candidate.getStudentId(), prepared.questions());
        insertQuestions(session.getId(), studentId, prepared.questions());

        candidate.setStatus("MATCHED");
        candidate.setMatchedSessionId(session.getId());
        candidate.setMatchedAt(now);
        assertUpdated(matchTicketMapper.updateById(candidate));

        GameMatchTicket matched = newTicket(studentId, prepared, "MATCHED", now.plusMinutes(2));
        matched.setMatchedSessionId(session.getId());
        matched.setMatchedAt(now);
        matchTicketMapper.insert(matched);
        return toMatchResult(matched);
    }

    @Transactional(rollbackFor = Exception.class)
    public void cancelMatch(Long studentId, Long ticketId) {
        GameMatchTicket ticket = matchTicketMapper.selectOwnedForUpdate(ticketId, studentId);
        if (ticket == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "匹配票据不存在");
        }
        if ("CANCELLED".equals(ticket.getStatus()) || "EXPIRED".equals(ticket.getStatus())) {
            return;
        }
        if (!WAITING.equals(ticket.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "已经匹配成功，不能取消匹配票据");
        }
        ticket.setStatus("CANCELLED");
        assertUpdated(matchTicketMapper.updateById(ticket));
    }

    @Transactional(rollbackFor = Exception.class)
    public Long startSession(Long studentId, Long sessionId) {
        GameSession session = requireSessionForUpdate(sessionId);
        requireMember(sessionId, studentId);
        if (IN_PROGRESS.equals(session.getStatus()) || COMPLETED.equals(session.getStatus())) {
            return sessionId;
        }
        if (!READY.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "对局尚未准备完成");
        }
        if (PK.equals(session.getMode()) && session.getMemberCount() != 2) {
            throw new BusinessException(ResultCode.CONFLICT, "PK需要两名玩家");
        }
        OffsetDateTime now = now();
        session.setStatus(IN_PROGRESS);
        session.setStartedAt(now);
        session.setExpiresAt(now.plusSeconds(session.getTimeLimitSecondsSnapshot()));
        assertUpdated(sessionMapper.updateById(session));
        updateAllMemberStatuses(sessionId, "PLAYING", now);
        return sessionId;
    }

    @Transactional(rollbackFor = Exception.class)
    public GameCommandView.AnswerFeedback submitAnswer(
            Long studentId,
            Long sessionId,
            Long sessionQuestionId,
            SubmitGameAnswerRequest request) {
        GameSession session = requireSessionForUpdate(sessionId);
        GameSessionMember member = requireMember(sessionId, studentId);
        GameSessionQuestion question = questionMapper.selectForUpdate(
                sessionQuestionId, sessionId, studentId);
        if (question == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "对局题目不存在");
        }
        if ("GRADED".equals(question.getGradingStatus())) {
            if (request.requestId().equals(question.getAnswerRequestId())) {
                return answerFeedback(session, member, question, true, true);
            }
            throw new BusinessException(ResultCode.CONFLICT, "该题已经作答，不能重复修改");
        }
        if (!IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "对局当前不能作答");
        }
        OffsetDateTime answeredAt = now();
        if (session.getExpiresAt() != null && !answeredAt.isBefore(session.getExpiresAt())) {
            settleSession(session, answeredAt);
            return answerFeedback(session, member, question, false, false);
        }
        if (question.getSequenceNo() != member.getAnsweredCount() + 1) {
            throw new BusinessException(ResultCode.CONFLICT, "请按题目顺序作答");
        }

        QuestionResponse snapshot = readQuestionSnapshot(question.getQuestionSnapshot());
        boolean correct = answerGrader.isCorrect(snapshot.standardAnswer(), request.answer());
        int combo = correct ? member.getCurrentCombo() + 1 : 0;
        BigDecimal score = correct ? question.getMaxScore() : BigDecimal.ZERO;

        question.setGradingStatus("GRADED");
        question.setStudentAnswer(answerJson(request.answer()));
        question.setAnswerRequestId(request.requestId());
        question.setCorrect(correct);
        question.setScore(score);
        question.setResponseMs(request.responseMs());
        question.setComboAfter(combo);
        question.setProgressDelta(score);
        question.setEnergyDelta(correct ? 10 : 0);
        question.setAnsweredAt(answeredAt);
        assertUpdated(questionMapper.updateById(question));

        member.setAnsweredCount(member.getAnsweredCount() + 1);
        member.setCorrectCount(member.getCorrectCount() + (correct ? 1 : 0));
        member.setScore(member.getScore().add(score));
        member.setContributionValue(member.getContributionValue().add(score));
        member.setCurrentCombo(combo);
        member.setMaxCombo(Math.max(member.getMaxCombo(), combo));
        member.setTotalResponseMs(member.getTotalResponseMs() + request.responseMs());
        member.setLastAnsweredAt(answeredAt);
        if (member.getAnsweredCount().equals(session.getQuestionCountSnapshot())) {
            member.setFinishedAt(answeredAt);
        }
        updateMember(member);

        session.setTeamProgress(calculateProgress(session));
        outboxService.append(
                "game.answer." + question.getId(),
                GameEventOutboxService.ANSWER_GRADED,
                "ANSWER",
                question.getId(),
                studentId,
                Map.of(
                        "sessionId", sessionId,
                        "questionId", question.getQuestionId(),
                        "knowledgePointId", session.getKnowledgePointIdSnapshot(),
                        "correct", correct,
                        "score", score,
                        "responseMs", request.responseMs(),
                        "combo", combo));

        boolean shouldSettle = LEVEL.equals(session.getMode())
                ? member.getAnsweredCount().equals(session.getQuestionCountSnapshot())
                : allMembersAnswered(session);
        if (shouldSettle) {
            settleSession(session, answeredAt);
        } else {
            assertUpdated(sessionMapper.updateById(session));
        }
        Long nextQuestionId = COMPLETED.equals(session.getStatus())
                ? null
                : questionMapper.selectNextUnansweredId(sessionId, studentId);
        return new GameCommandView.AnswerFeedback(
                sessionId,
                question.getId(),
                true,
                question.getCorrect(),
                question.getScore(),
                question.getComboAfter(),
                session.getTeamProgress(),
                member.getAnsweredCount(),
                session.getQuestionCountSnapshot(),
                nextQuestionId,
                session.getStatus(),
                session.getResultType(),
                session.getWinnerStudentId(),
                false);
    }

    @Transactional(rollbackFor = Exception.class)
    public Long finishSession(Long studentId, Long sessionId) {
        GameSession session = requireSessionForUpdate(sessionId);
        GameSessionMember member = requireMember(sessionId, studentId);
        if (COMPLETED.equals(session.getStatus())) {
            return sessionId;
        }
        if (!IN_PROGRESS.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "对局当前不能结算");
        }
        OffsetDateTime now = now();
        boolean expired = session.getExpiresAt() != null && !now.isBefore(session.getExpiresAt());
        boolean callerFinished = member.getAnsweredCount().equals(session.getQuestionCountSnapshot());
        if (!expired && !callerFinished) {
            throw new BusinessException(ResultCode.CONFLICT, "还有未完成的题目");
        }
        if (expired || LEVEL.equals(session.getMode()) || allMembersAnswered(session)) {
            settleSession(session, now);
        }
        return sessionId;
    }

    @Transactional(rollbackFor = Exception.class)
    public Long cancelSession(Long studentId, Long sessionId) {
        GameSession session = requireSessionForUpdate(sessionId);
        requireMember(sessionId, studentId);
        if ("CANCELLED".equals(session.getStatus()) || "EXPIRED".equals(session.getStatus())) {
            return sessionId;
        }
        if (!WAITING.equals(session.getStatus()) && !READY.equals(session.getStatus())) {
            throw new BusinessException(ResultCode.CONFLICT, "已经开始的对局不能取消");
        }
        cancelSession(session, now(), "玩家取消对局");
        return sessionId;
    }

    @Transactional(rollbackFor = Exception.class)
    public int expireDueSessions(int batchSize) {
        OffsetDateTime now = now();
        matchTicketMapper.expireWaiting(now);
        List<GameSession> sessions = sessionMapper.selectExpiredBatchForUpdate(now, batchSize);
        for (GameSession session : sessions) {
            if (IN_PROGRESS.equals(session.getStatus())) {
                settleSession(session, now);
            } else {
                expireSession(session, now, "对局等待超时");
            }
        }
        return sessions.size();
    }

    private void settleSession(GameSession session, OffsetDateTime settledAt) {
        if (COMPLETED.equals(session.getStatus())) {
            return;
        }
        if (LEVEL.equals(session.getMode())) {
            settleLevel(session, settledAt);
        } else {
            settlePk(session, settledAt);
        }
    }

    private void settleLevel(GameSession session, OffsetDateTime settledAt) {
        GameSessionMember member = requireMember(session.getId(), session.getCreatorStudentId());
        int questionCount = session.getQuestionCountSnapshot();
        BigDecimal accuracy = BigDecimal.valueOf(member.getCorrectCount())
                .divide(BigDecimal.valueOf(questionCount), 4, RoundingMode.HALF_UP);
        boolean passed = accuracy.compareTo(session.getPassRateSnapshot()) >= 0;
        int durationSeconds = durationSeconds(session, settledAt);
        int stars = starCount(passed, accuracy);
        GameLevel level = levelMapper.selectById(session.getLevelId());
        if (level == null) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "结算时关卡配置不存在");
        }

        GameLevelProgress progress = levelProgressMapper.selectForUpdate(
                member.getStudentId(), session.getLevelId());
        boolean firstPass = progress == null || !"PASSED".equals(progress.getStatus());
        saveLevelProgress(progress, session, member, passed, stars, durationSeconds, settledAt);

        int points = passed ? level.getBasePoints() : 0;
        int experience = passed ? level.getBaseExperience() : 0;
        PlayerState playerState = loadPlayer(member.getStudentId());
        applyLevelResult(
                playerState.progress(), passed, firstPass, member.getMaxCombo(),
                points, experience, settledAt);
        persistPlayer(playerState);
        addReward(member.getStudentId(), session.getId(), LEVEL, "POINTS", points,
                playerState.progress().getTotalPoints(), "LEVEL_COMPLETED");
        addReward(member.getStudentId(), session.getId(), LEVEL, "EXPERIENCE", experience,
                playerState.progress().getExperience(), "LEVEL_COMPLETED");

        member.setStatus("FINISHED");
        member.setResult(passed ? "PASSED" : "FAILED");
        member.setPointsReward(points);
        member.setExperienceReward(experience);
        if (member.getFinishedAt() == null) {
            member.setFinishedAt(settledAt);
        }
        updateMember(member);

        if (passed) {
            unlockNextLevels(member.getStudentId(), session.getLevelId(), settledAt);
        }
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("score", member.getScore());
        detail.put("accuracy", accuracy);
        detail.put("stars", stars);
        detail.put("durationSeconds", durationSeconds);
        completeSession(
                session,
                passed ? "LEVEL_PASSED" : "LEVEL_FAILED",
                null,
                detail,
                settledAt);
        appendSessionCompleted(session, member, playerState.progress().getPkRating());
    }

    private void settlePk(GameSession session, OffsetDateTime settledAt) {
        List<GameSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<GameSessionMember>()
                        .eq(GameSessionMember::getSessionId, session.getId())
                        .orderByAsc(GameSessionMember::getSeatNo));
        if (members.size() != 2) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "PK结算人数不正确");
        }
        GameSessionMember first = members.get(0);
        GameSessionMember second = members.get(1);
        int comparison = comparePkPerformance(first, second);
        boolean draw = comparison == 0;
        GameSessionMember winner = draw ? null : (comparison > 0 ? first : second);

        List<GameSessionMember> lockOrder = new ArrayList<>(members);
        lockOrder.sort(Comparator.comparing(GameSessionMember::getStudentId));
        Map<Long, PlayerState> playerStates = new LinkedHashMap<>();
        for (GameSessionMember member : lockOrder) {
            playerStates.put(member.getStudentId(), loadPlayer(member.getStudentId()));
        }
        int firstBefore = playerStates.get(first.getStudentId()).progress().getPkRating();
        int secondBefore = playerStates.get(second.getStudentId()).progress().getPkRating();
        double firstResult = draw ? 0.5 : (winner == first ? 1.0 : 0.0);
        int firstAfter = nextRating(firstBefore, secondBefore, firstResult);
        int secondAfter = nextRating(secondBefore, firstBefore, 1.0 - firstResult);

        settlePkMember(
                session, first, playerStates.get(first.getStudentId()),
                draw, winner == first, firstBefore, firstAfter, settledAt);
        settlePkMember(
                session, second, playerStates.get(second.getStudentId()),
                draw, winner == second, secondBefore, secondAfter, settledAt);

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("firstStudentId", first.getStudentId());
        detail.put("firstScore", first.getScore());
        detail.put("firstResponseMs", first.getTotalResponseMs());
        detail.put("secondStudentId", second.getStudentId());
        detail.put("secondScore", second.getScore());
        detail.put("secondResponseMs", second.getTotalResponseMs());
        completeSession(
                session,
                draw ? "PK_DRAW" : "PK_WIN",
                winner == null ? null : winner.getStudentId(),
                detail,
                settledAt);
        appendSessionCompleted(
                session, first, playerStates.get(first.getStudentId()).progress().getPkRating());
        appendSessionCompleted(
                session, second, playerStates.get(second.getStudentId()).progress().getPkRating());
    }

    private void settlePkMember(
            GameSession session,
            GameSessionMember member,
            PlayerState state,
            boolean draw,
            boolean won,
            int ratingBefore,
            int ratingAfter,
            OffsetDateTime settledAt) {
        String result = draw ? "DRAW" : (won ? "WIN" : "LOSE");
        int points = draw ? PK_DRAW_POINTS : (won ? PK_WIN_POINTS : PK_LOSE_POINTS);
        int experience = draw ? PK_DRAW_EXPERIENCE : (won ? PK_WIN_EXPERIENCE : PK_LOSE_EXPERIENCE);
        applyPkResult(state.progress(), result, ratingAfter, member.getMaxCombo(), points, experience, settledAt);
        persistPlayer(state);
        addReward(member.getStudentId(), session.getId(), PK, "POINTS", points,
                state.progress().getTotalPoints(), "PK_" + result);
        addReward(member.getStudentId(), session.getId(), PK, "EXPERIENCE", experience,
                state.progress().getExperience(), "PK_" + result);

        member.setStatus("FINISHED");
        member.setResult(result);
        member.setRatingBefore(ratingBefore);
        member.setRatingAfter(ratingAfter);
        member.setPointsReward(points);
        member.setExperienceReward(experience);
        if (member.getFinishedAt() == null) {
            member.setFinishedAt(settledAt);
        }
        updateMember(member);
    }

    private void completeSession(
            GameSession session,
            String resultType,
            Long winnerStudentId,
            Map<String, Object> detail,
            OffsetDateTime settledAt) {
        session.setStatus(COMPLETED);
        session.setResultType(resultType);
        session.setWinnerStudentId(winnerStudentId);
        session.setEndedAt(settledAt);
        session.setSettledAt(settledAt);
        session.setResultDetail(objectMapper.writeValueAsString(detail));
        assertUpdated(sessionMapper.updateById(session));
    }

    private void saveLevelProgress(
            GameLevelProgress progress,
            GameSession session,
            GameSessionMember member,
            boolean passed,
            int stars,
            int durationSeconds,
            OffsetDateTime playedAt) {
        boolean insert = progress == null;
        if (insert) {
            progress = new GameLevelProgress();
            progress.setStudentId(member.getStudentId());
            progress.setLevelId(session.getLevelId());
            progress.setAttemptCount(0);
            progress.setFirstUnlockedAt(playedAt);
        }
        progress.setStatus(passed || "PASSED".equals(progress.getStatus()) ? "PASSED" : "UNLOCKED");
        progress.setAttemptCount(progress.getAttemptCount() + 1);
        progress.setBestScore(max(progress.getBestScore(), member.getScore()));
        progress.setBestCorrectCount(max(progress.getBestCorrectCount(), member.getCorrectCount()));
        progress.setBestStarCount(max(progress.getBestStarCount(), stars));
        progress.setBestDurationSeconds(min(progress.getBestDurationSeconds(), durationSeconds));
        progress.setLastSessionId(session.getId());
        progress.setLastPlayedAt(playedAt);
        if (passed && progress.getFirstPassedAt() == null) {
            progress.setFirstPassedAt(playedAt);
        }
        if (insert) {
            levelProgressMapper.insert(progress);
        } else {
            assertUpdated(levelProgressMapper.update(
                    progress,
                    new LambdaUpdateWrapper<GameLevelProgress>()
                            .eq(GameLevelProgress::getStudentId, progress.getStudentId())
                            .eq(GameLevelProgress::getLevelId, progress.getLevelId())));
        }
    }

    private void unlockNextLevels(Long studentId, Long levelId, OffsetDateTime unlockedAt) {
        levelMapper.selectList(new LambdaQueryWrapper<GameLevel>()
                        .eq(GameLevel::getPreviousLevelId, levelId)
                        .eq(GameLevel::getStatus, "ENABLED"))
                .forEach(level -> levelProgressMapper.insertUnlockedIfAbsent(
                        studentId, level.getId(), unlockedAt));
    }

    private void requireLevelUnlocked(Long studentId, GameLevel level) {
        if (level.getPreviousLevelId() != null) {
            GameLevelProgress previous = levelProgressMapper.selectOne(
                    new LambdaQueryWrapper<GameLevelProgress>()
                            .eq(GameLevelProgress::getStudentId, studentId)
                            .eq(GameLevelProgress::getLevelId, level.getPreviousLevelId()));
            if (previous == null || !"PASSED".equals(previous.getStatus())) {
                throw new BusinessException(ResultCode.FORBIDDEN, "请先通过前置关卡");
            }
        }
        levelProgressMapper.insertUnlockedIfAbsent(studentId, level.getId(), now());
    }

    private PlayerState loadPlayer(Long studentId) {
        GamePlayerProgress progress = playerProgressMapper.selectForUpdate(studentId);
        if (progress != null) {
            return new PlayerState(progress, false);
        }
        progress = new GamePlayerProgress();
        progress.setStudentId(studentId);
        progress.setTotalPoints(0L);
        progress.setExperience(0L);
        progress.setPlayerLevel(1);
        progress.setTotalSessions(0);
        progress.setSuccessSessions(0);
        progress.setLevelPassedCount(0);
        progress.setPkSessions(0);
        progress.setPkWins(0);
        progress.setPkLosses(0);
        progress.setPkDraws(0);
        progress.setPkRating(1000);
        progress.setCurrentWinStreak(0);
        progress.setBestWinStreak(0);
        progress.setBestCombo(0);
        progress.setVersion(0);
        return new PlayerState(progress, true);
    }

    private void applyLevelResult(
            GamePlayerProgress progress,
            boolean passed,
            boolean firstPass,
            int maxCombo,
            int points,
            int experience,
            OffsetDateTime playedAt) {
        progress.setTotalSessions(progress.getTotalSessions() + 1);
        progress.setSuccessSessions(progress.getSuccessSessions() + (passed ? 1 : 0));
        progress.setLevelPassedCount(progress.getLevelPassedCount() + (passed && firstPass ? 1 : 0));
        progress.setBestCombo(Math.max(progress.getBestCombo(), maxCombo));
        progress.setTotalPoints(progress.getTotalPoints() + points);
        progress.setExperience(progress.getExperience() + experience);
        progress.setPlayerLevel(playerLevel(progress.getExperience()));
        progress.setLastPlayedAt(playedAt);
    }

    private void applyPkResult(
            GamePlayerProgress progress,
            String result,
            int ratingAfter,
            int maxCombo,
            int points,
            int experience,
            OffsetDateTime playedAt) {
        progress.setTotalSessions(progress.getTotalSessions() + 1);
        progress.setSuccessSessions(progress.getSuccessSessions() + ("WIN".equals(result) ? 1 : 0));
        progress.setPkSessions(progress.getPkSessions() + 1);
        progress.setPkWins(progress.getPkWins() + ("WIN".equals(result) ? 1 : 0));
        progress.setPkLosses(progress.getPkLosses() + ("LOSE".equals(result) ? 1 : 0));
        progress.setPkDraws(progress.getPkDraws() + ("DRAW".equals(result) ? 1 : 0));
        progress.setPkRating(ratingAfter);
        int streak = "WIN".equals(result) ? progress.getCurrentWinStreak() + 1 : 0;
        progress.setCurrentWinStreak(streak);
        progress.setBestWinStreak(Math.max(progress.getBestWinStreak(), streak));
        progress.setBestCombo(Math.max(progress.getBestCombo(), maxCombo));
        progress.setTotalPoints(progress.getTotalPoints() + points);
        progress.setExperience(progress.getExperience() + experience);
        progress.setPlayerLevel(playerLevel(progress.getExperience()));
        progress.setLastPlayedAt(playedAt);
    }

    private void persistPlayer(PlayerState state) {
        if (state.insert()) {
            playerProgressMapper.insert(state.progress());
        } else {
            assertUpdated(playerProgressMapper.updateById(state.progress()));
        }
    }

    private void addReward(
            Long studentId,
            Long sessionId,
            String businessType,
            String rewardType,
            int amount,
            long balanceAfter,
            String reason) {
        if (amount <= 0) {
            return;
        }
        GameRewardRecord reward = new GameRewardRecord();
        reward.setStudentId(studentId);
        reward.setSessionId(sessionId);
        reward.setBusinessType(businessType);
        reward.setRewardType(rewardType);
        reward.setAmount((long) amount);
        reward.setBalanceAfter(balanceAfter);
        reward.setReason(reason);
        reward.setIdempotencyKey(
                "game:" + sessionId + ":" + studentId + ":" + rewardType.toLowerCase());
        rewardRecordMapper.insert(reward);
    }

    private void appendSessionCompleted(
            GameSession session, GameSessionMember member, Integer pkRating) {
        outboxService.append(
                "game.session." + session.getId() + "." + member.getStudentId() + ".completed",
                GameEventOutboxService.SESSION_COMPLETED,
                "SESSION",
                session.getId(),
                member.getStudentId(),
                Map.of(
                        "mode", session.getMode(),
                        "result", member.getResult(),
                        "score", member.getScore(),
                        "correctCount", member.getCorrectCount(),
                        "questionCount", session.getQuestionCountSnapshot(),
                        "pointsReward", member.getPointsReward(),
                        "experienceReward", member.getExperienceReward(),
                        "pkRating", pkRating));
    }

    private void cancelSession(GameSession session, OffsetDateTime endedAt, String reason) {
        session.setStatus("CANCELLED");
        session.setEndedAt(endedAt);
        session.setResultDetail(objectMapper.writeValueAsString(Map.of("reason", reason)));
        assertUpdated(sessionMapper.updateById(session));
        finishMembersAsCancelled(session.getId(), endedAt);
    }

    private void expireSession(GameSession session, OffsetDateTime endedAt, String reason) {
        session.setStatus("EXPIRED");
        session.setEndedAt(endedAt);
        session.setResultDetail(objectMapper.writeValueAsString(Map.of("reason", reason)));
        assertUpdated(sessionMapper.updateById(session));
        finishMembersAsCancelled(session.getId(), endedAt);
    }

    private void finishMembersAsCancelled(Long sessionId, OffsetDateTime endedAt) {
        GameSessionMember update = new GameSessionMember();
        update.setStatus("LEFT");
        update.setResult("CANCELLED");
        update.setFinishedAt(endedAt);
        memberMapper.update(update, new LambdaUpdateWrapper<GameSessionMember>()
                .eq(GameSessionMember::getSessionId, sessionId)
                .in(GameSessionMember::getStatus, List.of("JOINED", READY, "PLAYING")));
    }

    private GameSession newSession(
            String mode,
            Long levelId,
            Long creatorStudentId,
            String matchMode,
            String inviteCode,
            String status,
            int memberCount,
            int gradeLevel,
            Long knowledgePointId,
            int difficulty,
            int questionCount,
            int timeLimitSeconds,
            BigDecimal passRate,
            OffsetDateTime expiresAt) {
        GameSession session = new GameSession();
        session.setMode(mode);
        session.setLevelId(levelId);
        session.setCreatorStudentId(creatorStudentId);
        session.setMatchMode(matchMode);
        session.setInviteCode(inviteCode);
        session.setStatus(status);
        session.setMemberCount(memberCount);
        session.setGradeLevelSnapshot(gradeLevel);
        session.setKnowledgePointIdSnapshot(knowledgePointId);
        session.setDifficultySnapshot(difficulty);
        session.setQuestionCountSnapshot(questionCount);
        session.setTimeLimitSecondsSnapshot(timeLimitSeconds);
        session.setPassRateSnapshot(passRate);
        session.setTeamProgress(BigDecimal.ZERO.setScale(2));
        session.setExpiresAt(expiresAt);
        session.setVersion(0);
        return session;
    }

    private GameSessionMember newMember(
            Long sessionId,
            Long studentId,
            int seatNo,
            String role,
            String status,
            OffsetDateTime joinedAt) {
        GameSessionMember member = new GameSessionMember();
        member.setSessionId(sessionId);
        member.setStudentId(studentId);
        member.setSeatNo(seatNo);
        member.setMemberRole(role);
        member.setStatus(status);
        member.setResult("PENDING");
        member.setAnsweredCount(0);
        member.setCorrectCount(0);
        member.setScore(BigDecimal.ZERO.setScale(2));
        member.setContributionValue(BigDecimal.ZERO.setScale(2));
        member.setCurrentCombo(0);
        member.setMaxCombo(0);
        member.setTotalResponseMs(0L);
        member.setPointsReward(0);
        member.setExperienceReward(0);
        member.setJoinedAt(joinedAt);
        return member;
    }

    private GameMatchTicket newTicket(
            Long studentId,
            GameQuestionProvider.PreparedPk prepared,
            String status,
            OffsetDateTime expiresAt) {
        GameMatchTicket ticket = new GameMatchTicket();
        ticket.setStudentId(studentId);
        ticket.setStatus(status);
        ticket.setGradeLevel(prepared.gradeLevel());
        ticket.setKnowledgePointId(prepared.knowledgePointId());
        ticket.setDifficulty(prepared.difficulty());
        ticket.setQuestionCount(prepared.questionCount());
        ticket.setTimeLimitSeconds(prepared.timeLimitSeconds());
        ticket.setExpiresAt(expiresAt);
        ticket.setVersion(0);
        return ticket;
    }

    private void insertQuestions(
            Long sessionId,
            Long studentId,
            List<GameQuestionProvider.PreparedQuestion> preparedQuestions) {
        int sequence = 1;
        for (GameQuestionProvider.PreparedQuestion prepared : preparedQuestions) {
            GameSessionQuestion question = new GameSessionQuestion();
            question.setSessionId(sessionId);
            question.setStudentId(studentId);
            question.setSequenceNo(sequence++);
            question.setQuestionId(prepared.question().id());
            question.setQuestionSnapshot(objectMapper.writeValueAsString(prepared.question()));
            question.setMaxScore(prepared.maxScore());
            question.setGradingStatus("UNANSWERED");
            question.setProgressDelta(BigDecimal.ZERO.setScale(2));
            question.setEnergyDelta(0);
            questionMapper.insert(question);
        }
    }

    private void copyQuestions(Long sessionId, Long fromStudentId, Long toStudentId) {
        List<GameSessionQuestion> source = questionMapper.selectList(
                new LambdaQueryWrapper<GameSessionQuestion>()
                        .eq(GameSessionQuestion::getSessionId, sessionId)
                        .eq(GameSessionQuestion::getStudentId, fromStudentId)
                        .orderByAsc(GameSessionQuestion::getSequenceNo));
        if (source.isEmpty()) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "PK房间题目快照不存在");
        }
        for (GameSessionQuestion existing : source) {
            GameSessionQuestion copy = new GameSessionQuestion();
            copy.setSessionId(sessionId);
            copy.setStudentId(toStudentId);
            copy.setSequenceNo(existing.getSequenceNo());
            copy.setQuestionId(existing.getQuestionId());
            copy.setQuestionSnapshot(existing.getQuestionSnapshot());
            copy.setMaxScore(existing.getMaxScore());
            copy.setGradingStatus("UNANSWERED");
            copy.setProgressDelta(BigDecimal.ZERO.setScale(2));
            copy.setEnergyDelta(0);
            questionMapper.insert(copy);
        }
    }

    private GameSession requireSessionForUpdate(Long sessionId) {
        GameSession session = sessionMapper.selectForUpdate(sessionId);
        if (session == null) {
            throw new BusinessException(ResultCode.GAME_SESSION_NOT_FOUND);
        }
        return session;
    }

    private GameSessionMember requireMember(Long sessionId, Long studentId) {
        GameSessionMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<GameSessionMember>()
                        .eq(GameSessionMember::getSessionId, sessionId)
                        .eq(GameSessionMember::getStudentId, studentId));
        if (member == null) {
            throw new BusinessException(ResultCode.GAME_SESSION_NOT_FOUND);
        }
        return member;
    }

    private void requireNoActiveSession(Long studentId) {
        if (memberMapper.selectActiveSessionId(studentId) != null) {
            throw new BusinessException(ResultCode.CONFLICT, "已有进行中的游戏对局");
        }
    }

    private void requireSameMatchRule(
            GameMatchTicket ticket, GameQuestionProvider.PreparedPk prepared) {
        if (!ticket.getGradeLevel().equals(prepared.gradeLevel())
                || !ticket.getKnowledgePointId().equals(prepared.knowledgePointId())
                || !ticket.getDifficulty().equals(prepared.difficulty())
                || !ticket.getQuestionCount().equals(prepared.questionCount())
                || !ticket.getTimeLimitSeconds().equals(prepared.timeLimitSeconds())) {
            throw new BusinessException(ResultCode.CONFLICT, "已有不同条件的随机匹配正在等待");
        }
    }

    private MatchTransactionResult toMatchResult(GameMatchTicket ticket) {
        return new MatchTransactionResult(
                ticket.getId(),
                ticket.getStatus(),
                ticket.getExpiresAt(),
                ticket.getMatchedSessionId());
    }

    private boolean allMembersAnswered(GameSession session) {
        List<GameSessionMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<GameSessionMember>()
                        .eq(GameSessionMember::getSessionId, session.getId()));
        return members.size() == session.getMemberCount()
                && members.stream().allMatch(member ->
                        member.getAnsweredCount().equals(session.getQuestionCountSnapshot()));
    }

    private BigDecimal calculateProgress(GameSession session) {
        if (LEVEL.equals(session.getMode())) {
            GameSessionMember member = requireMember(session.getId(), session.getCreatorStudentId());
            return member.getScore().setScale(2, RoundingMode.HALF_UP);
        }
        int answered = memberMapper.selectList(new LambdaQueryWrapper<GameSessionMember>()
                        .eq(GameSessionMember::getSessionId, session.getId()))
                .stream()
                .mapToInt(GameSessionMember::getAnsweredCount)
                .sum();
        int total = session.getQuestionCountSnapshot() * session.getMemberCount();
        return BigDecimal.valueOf(answered * 100L)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }

    private int comparePkPerformance(GameSessionMember first, GameSessionMember second) {
        int scoreComparison = first.getScore().compareTo(second.getScore());
        if (scoreComparison != 0) {
            return scoreComparison;
        }
        int correctComparison = first.getCorrectCount().compareTo(second.getCorrectCount());
        if (correctComparison != 0) {
            return correctComparison;
        }
        int answeredComparison = first.getAnsweredCount().compareTo(second.getAnsweredCount());
        if (answeredComparison != 0) {
            return answeredComparison;
        }
        return Long.compare(second.getTotalResponseMs(), first.getTotalResponseMs());
    }

    private int nextRating(int ownRating, int opponentRating, double result) {
        double expected = 1.0 / (1.0 + Math.pow(10.0, (opponentRating - ownRating) / 400.0));
        return Math.max(0, (int) Math.round(ownRating + 32.0 * (result - expected)));
    }

    private void updateMember(GameSessionMember member) {
        assertUpdated(memberMapper.update(
                member,
                new LambdaUpdateWrapper<GameSessionMember>()
                        .eq(GameSessionMember::getSessionId, member.getSessionId())
                        .eq(GameSessionMember::getStudentId, member.getStudentId())));
    }

    private void updateAllMemberStatuses(Long sessionId, String status, OffsetDateTime updatedAt) {
        memberMapper.update(null, new LambdaUpdateWrapper<GameSessionMember>()
                .eq(GameSessionMember::getSessionId, sessionId)
                .set(GameSessionMember::getStatus, status)
                .set(GameSessionMember::getUpdatedAt, updatedAt));
    }

    private GameCommandView.AnswerFeedback answerFeedback(
            GameSession session,
            GameSessionMember member,
            GameSessionQuestion question,
            boolean accepted,
            boolean replay) {
        Long nextQuestionId = COMPLETED.equals(session.getStatus())
                ? null
                : questionMapper.selectNextUnansweredId(session.getId(), member.getStudentId());
        return new GameCommandView.AnswerFeedback(
                session.getId(),
                question.getId(),
                accepted,
                question.getCorrect(),
                question.getScore(),
                question.getComboAfter(),
                session.getTeamProgress(),
                member.getAnsweredCount(),
                session.getQuestionCountSnapshot(),
                nextQuestionId,
                session.getStatus(),
                session.getResultType(),
                session.getWinnerStudentId(),
                replay);
    }

    private QuestionResponse readQuestionSnapshot(String json) {
        try {
            return objectMapper.readValue(json, QuestionResponse.class);
        } catch (JacksonException exception) {
            throw new BusinessException(ResultCode.INTERNAL_SERVER_ERROR, "游戏题目快照损坏");
        }
    }

    private String answerJson(String answer) {
        return objectMapper.writeValueAsString(Map.of("value", answer.trim()));
    }

    private int durationSeconds(GameSession session, OffsetDateTime endedAt) {
        OffsetDateTime startedAt = session.getStartedAt() == null ? session.getCreatedAt() : session.getStartedAt();
        if (startedAt == null) {
            startedAt = endedAt;
        }
        return Math.max(1, Math.toIntExact(Duration.between(startedAt, endedAt).toSeconds()));
    }

    private int starCount(boolean passed, BigDecimal accuracy) {
        if (!passed) {
            return 0;
        }
        if (accuracy.compareTo(BigDecimal.ONE) >= 0) {
            return 3;
        }
        return accuracy.compareTo(new BigDecimal("0.9000")) >= 0 ? 2 : 1;
    }

    private int playerLevel(long experience) {
        return Math.toIntExact(Math.min(Integer.MAX_VALUE, experience / 100L + 1L));
    }

    private BigDecimal max(BigDecimal current, BigDecimal candidate) {
        return current == null || candidate.compareTo(current) > 0 ? candidate : current;
    }

    private Integer max(Integer current, Integer candidate) {
        return current == null ? candidate : Math.max(current, candidate);
    }

    private Integer min(Integer current, Integer candidate) {
        return current == null ? candidate : Math.min(current, candidate);
    }

    private void assertUpdated(int affectedRows) {
        if (affectedRows != 1) {
            throw new BusinessException(ResultCode.CONFLICT, "游戏数据已发生变化，请刷新后重试");
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(ZoneOffset.UTC);
    }

    public record MatchTransactionResult(
            Long ticketId,
            String status,
            OffsetDateTime expiresAt,
            Long sessionId
    ) {
    }

    private record PlayerState(GamePlayerProgress progress, boolean insert) {
    }
}
