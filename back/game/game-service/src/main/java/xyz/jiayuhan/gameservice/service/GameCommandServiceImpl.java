package xyz.jiayuhan.gameservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import xyz.jiayuhan.common.core.exception.BusinessException;
import xyz.jiayuhan.common.core.exception.ResultCode;
import xyz.jiayuhan.common.core.result.Result;
import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;
import xyz.jiayuhan.gameservice.entity.GameMatchTicket;
import xyz.jiayuhan.gameservice.model.request.CreatePkSessionRequest;
import xyz.jiayuhan.gameservice.model.request.SubmitGameAnswerRequest;
import xyz.jiayuhan.gameservice.model.response.GameCommandView;
import xyz.jiayuhan.gameservice.model.response.GameView;
import xyz.jiayuhan.userapi.client.UserFeignClient;
import xyz.jiayuhan.userapi.model.response.UserValidationResponse;

import java.security.SecureRandom;
import java.util.Locale;
import java.util.function.Supplier;

/** 先完成远程校验与题目准备，再进入短事务写入游戏聚合。 */
@Service
@RequiredArgsConstructor
public class GameCommandServiceImpl implements GameCommandService {

    private static final char[] INVITE_ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final GameQuestionProvider questionProvider;
    private final GameSessionTransactionService transactionService;
    private final GameQueryService queryService;
    private final UserFeignClient userFeignClient;

    @Override
    public GameView.SessionDetail createLevelSession(Long studentId, Long levelId) {
        requireActiveStudent(studentId);
        GameView.SessionDetail existing = matchingActiveSession(
                studentId, summary -> "LEVEL".equals(summary.mode())
                        && levelId.equals(summary.levelId()));
        if (existing != null) {
            return existing;
        }
        GameQuestionProvider.PreparedLevel prepared = questionProvider.prepareLevel(levelId);
        try {
            Long sessionId = transactionService.createLevelSession(studentId, prepared);
            return queryService.getSession(studentId, sessionId);
        } catch (DataIntegrityViolationException exception) {
            return recoverConcurrentSession(
                    studentId,
                    summary -> "LEVEL".equals(summary.mode()) && levelId.equals(summary.levelId()),
                    "闯关对局创建冲突");
        }
    }

    @Override
    public GameView.SessionDetail createPkRoom(Long studentId, CreatePkSessionRequest request) {
        requireActiveStudent(studentId);
        GameView.SessionDetail existing = matchingActiveSession(
                studentId, summary -> "PK".equals(summary.mode())
                        && "ROOM".equals(summary.matchMode())
                        && studentId.equals(summary.creatorStudentId()));
        if (existing != null) {
            return existing;
        }
        GameQuestionProvider.PreparedPk prepared = preparePk(request);
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                Long sessionId = transactionService.createPkRoom(
                        studentId, newInviteCode(), prepared);
                return queryService.getSession(studentId, sessionId);
            } catch (DataIntegrityViolationException exception) {
                GameView.SessionDetail concurrent = matchingActiveSession(
                        studentId, summary -> "PK".equals(summary.mode())
                                && "ROOM".equals(summary.matchMode())
                                && studentId.equals(summary.creatorStudentId()));
                if (concurrent != null) {
                    return concurrent;
                }
            }
        }
        throw new BusinessException(ResultCode.CONFLICT, "PK房间创建冲突，请重试");
    }

    @Override
    public GameView.SessionDetail joinPkRoom(Long studentId, String inviteCode) {
        requireActiveStudent(studentId);
        String normalized = inviteCode.trim().toUpperCase(Locale.ROOT);
        GameView.SessionDetail active = matchingActiveSession(
                studentId, summary -> "PK".equals(summary.mode())
                        && "ROOM".equals(summary.matchMode())
                        && normalized.equals(summary.inviteCode()));
        if (active != null) {
            return active;
        }
        try {
            Long sessionId = transactionService.joinPkRoom(studentId, normalized);
            return queryService.getSession(studentId, sessionId);
        } catch (DataIntegrityViolationException exception) {
            return recoverConcurrentSession(
                    studentId,
                    summary -> "PK".equals(summary.mode())
                            && normalized.equals(summary.inviteCode()),
                    "加入PK房间冲突");
        }
    }

    @Override
    public GameCommandView.MatchStatus requestRandomMatch(
            Long studentId, CreatePkSessionRequest request) {
        requireActiveStudent(studentId);
        GameView.SessionDetail active = matchingActiveSession(
                studentId, summary -> "PK".equals(summary.mode())
                        && "RANDOM".equals(summary.matchMode()));
        if (active != null) {
            return new GameCommandView.MatchStatus(
                    null, "MATCHED", active.session().expiresAt(), active.session().id(), active);
        }
        GameMatchTicket waiting = transactionService.findWaitingTicket(studentId);
        if (waiting != null) {
            requireSameRequest(waiting, request);
            return new GameCommandView.MatchStatus(
                    waiting.getId(), waiting.getStatus(), waiting.getExpiresAt(), null, null);
        }
        GameQuestionProvider.PreparedPk prepared = preparePk(request);
        try {
            GameSessionTransactionService.MatchTransactionResult result =
                    transactionService.matchRandom(studentId, prepared);
            GameView.SessionDetail session = result.sessionId() == null
                    ? null
                    : queryService.getSession(studentId, result.sessionId());
            return new GameCommandView.MatchStatus(
                    result.ticketId(), result.status(), result.expiresAt(), result.sessionId(), session);
        } catch (DataIntegrityViolationException exception) {
            GameView.SessionDetail concurrent = matchingActiveSession(
                    studentId, summary -> "PK".equals(summary.mode())
                            && "RANDOM".equals(summary.matchMode()));
            if (concurrent != null) {
                return new GameCommandView.MatchStatus(
                        null, "MATCHED", concurrent.session().expiresAt(),
                        concurrent.session().id(), concurrent);
            }
            GameMatchTicket concurrentTicket = transactionService.findWaitingTicket(studentId);
            if (concurrentTicket != null) {
                return new GameCommandView.MatchStatus(
                        concurrentTicket.getId(), concurrentTicket.getStatus(),
                        concurrentTicket.getExpiresAt(), null, null);
            }
            throw new BusinessException(ResultCode.CONFLICT, "随机匹配冲突，请重试");
        }
    }

    @Override
    public void cancelRandomMatch(Long studentId, Long ticketId) {
        transactionService.cancelMatch(studentId, ticketId);
    }

    @Override
    public GameView.SessionDetail startSession(Long studentId, Long sessionId) {
        transactionService.startSession(studentId, sessionId);
        return queryService.getSession(studentId, sessionId);
    }

    @Override
    public GameCommandView.AnswerFeedback submitAnswer(
            Long studentId,
            Long sessionId,
            Long sessionQuestionId,
            SubmitGameAnswerRequest request) {
        return transactionService.submitAnswer(studentId, sessionId, sessionQuestionId, request);
    }

    @Override
    public GameView.SessionDetail finishSession(Long studentId, Long sessionId) {
        transactionService.finishSession(studentId, sessionId);
        return queryService.getSession(studentId, sessionId);
    }

    @Override
    public GameView.SessionDetail cancelSession(Long studentId, Long sessionId) {
        transactionService.cancelSession(studentId, sessionId);
        return queryService.getSession(studentId, sessionId);
    }

    private GameQuestionProvider.PreparedPk preparePk(CreatePkSessionRequest request) {
        return questionProvider.preparePk(
                request.gradeLevel(),
                request.knowledgePointId(),
                request.difficulty(),
                request.questionCount(),
                request.timeLimitSeconds());
    }

    private GameView.SessionDetail matchingActiveSession(
            Long studentId,
            java.util.function.Predicate<GameSessionSummaryResponse> predicate) {
        Long activeSessionId = transactionService.findActiveSessionId(studentId);
        if (activeSessionId == null) {
            return null;
        }
        GameView.SessionDetail detail = queryService.getSession(studentId, activeSessionId);
        if (!predicate.test(detail.session())) {
            throw new BusinessException(ResultCode.CONFLICT, "已有其他进行中的游戏对局");
        }
        return detail;
    }

    private GameView.SessionDetail recoverConcurrentSession(
            Long studentId,
            java.util.function.Predicate<GameSessionSummaryResponse> predicate,
            String message) {
        GameView.SessionDetail detail = matchingActiveSession(studentId, predicate);
        if (detail == null) {
            throw new BusinessException(ResultCode.CONFLICT, message);
        }
        return detail;
    }

    private void requireSameRequest(GameMatchTicket ticket, CreatePkSessionRequest request) {
        int count = request.questionCount() == null
                ? GameQuestionProvider.DEFAULT_PK_QUESTION_COUNT
                : request.questionCount();
        int time = request.timeLimitSeconds() == null
                ? GameQuestionProvider.DEFAULT_PK_TIME_LIMIT_SECONDS
                : request.timeLimitSeconds();
        if (!ticket.getGradeLevel().equals(request.gradeLevel())
                || !ticket.getKnowledgePointId().equals(request.knowledgePointId())
                || !ticket.getDifficulty().equals(request.difficulty())
                || !ticket.getQuestionCount().equals(count)
                || !ticket.getTimeLimitSeconds().equals(time)) {
            throw new BusinessException(ResultCode.CONFLICT, "已有不同条件的随机匹配正在等待");
        }
    }

    private void requireActiveStudent(Long studentId) {
        UserValidationResponse validation = requireRemote(
                () -> userFeignClient.validateUser(studentId, "student"),
                "暂时无法校验学生身份");
        if (!validation.exists() || !validation.active() || !validation.roleMatched()) {
            throw new BusinessException(ResultCode.FORBIDDEN);
        }
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

    private String newInviteCode() {
        char[] value = new char[8];
        for (int index = 0; index < value.length; index++) {
            value[index] = INVITE_ALPHABET[RANDOM.nextInt(INVITE_ALPHABET.length)];
        }
        return new String(value);
    }
}
