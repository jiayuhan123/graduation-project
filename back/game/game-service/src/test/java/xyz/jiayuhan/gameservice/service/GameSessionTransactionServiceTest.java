package xyz.jiayuhan.gameservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
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
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameSessionTransactionServiceTest {

    private static final Long STUDENT_ID = 101L;
    private static final Long OPPONENT_ID = 102L;
    private static final Long KNOWLEDGE_POINT_ID = 9201L;

    @Mock private GameLevelMapper levelMapper;
    @Mock private GameSessionMapper sessionMapper;
    @Mock private GameMatchTicketMapper matchTicketMapper;
    @Mock private GameSessionMemberMapper memberMapper;
    @Mock private GameSessionQuestionMapper questionMapper;
    @Mock private GamePlayerProgressMapper playerProgressMapper;
    @Mock private GameLevelProgressMapper levelProgressMapper;
    @Mock private GameRewardRecordMapper rewardRecordMapper;
    @Mock private GameEventOutboxService outboxService;

    private ObjectMapper objectMapper;
    private GameSessionTransactionService service;

    @BeforeEach
    void setUp() {
        objectMapper = JsonMapper.builder().build();
        service = new GameSessionTransactionService(
                levelMapper,
                sessionMapper,
                matchTicketMapper,
                memberMapper,
                questionMapper,
                playerProgressMapper,
                levelProgressMapper,
                rewardRecordMapper,
                outboxService,
                objectMapper,
                new GameAnswerGrader(objectMapper));
        lenient().when(sessionMapper.updateById(any(GameSession.class))).thenReturn(1);
        lenient().when(questionMapper.updateById(any(GameSessionQuestion.class))).thenReturn(1);
        lenient().when(memberMapper.update(any(GameSessionMember.class), any())).thenReturn(1);
        lenient().when(levelProgressMapper.insertUnlockedIfAbsent(anyLong(), anyLong(), any()))
                .thenReturn(1);
        lenient().when(levelMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void createsReadyLevelSessionWithFixedQuestionSnapshot() {
        GameLevel level = level(11L, 1);
        GameQuestionProvider.PreparedLevel prepared = new GameQuestionProvider.PreparedLevel(
                level,
                List.of(new GameQuestionProvider.PreparedQuestion(
                        question(201L, "{\"value\":42}"), new BigDecimal("100.00"))));
        when(memberMapper.selectActiveSessionId(STUDENT_ID)).thenReturn(null);
        when(levelMapper.selectForShare(level.getId())).thenReturn(level);
        doAnswer(invocation -> {
            GameSession session = invocation.getArgument(0);
            session.setId(301L);
            session.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            return 1;
        }).when(sessionMapper).insert(any(GameSession.class));
        doAnswer(invocation -> {
            GameSessionQuestion value = invocation.getArgument(0);
            value.setId(401L);
            return 1;
        }).when(questionMapper).insert(any(GameSessionQuestion.class));

        Long sessionId = service.createLevelSession(STUDENT_ID, prepared);

        assertThat(sessionId).isEqualTo(301L);
        ArgumentCaptor<GameSession> sessionCaptor = ArgumentCaptor.forClass(GameSession.class);
        verify(sessionMapper).insert(sessionCaptor.capture());
        assertThat(sessionCaptor.getValue().getMode()).isEqualTo("LEVEL");
        assertThat(sessionCaptor.getValue().getStatus()).isEqualTo("READY");
        verify(memberMapper).insert(any(GameSessionMember.class));
        ArgumentCaptor<GameSessionQuestion> questionCaptor =
                ArgumentCaptor.forClass(GameSessionQuestion.class);
        verify(questionMapper).insert(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getQuestionSnapshot()).contains("Q-201");
    }

    @Test
    void gradesLevelAnswerImmediatelySettlesOnceAndReplaysIdempotently() {
        GameLevel level = level(11L, 1);
        level.setBasePoints(30);
        level.setBaseExperience(20);
        GameSession session = levelSession(301L, level.getId());
        GameSessionMember member = member(session.getId(), STUDENT_ID, 1);
        GameSessionQuestion question = sessionQuestion(
                401L, session.getId(), STUDENT_ID, "{\"text\":\"3/4\"}");
        when(sessionMapper.selectForUpdate(session.getId())).thenReturn(session);
        when(memberMapper.selectOne(any())).thenReturn(member);
        when(questionMapper.selectForUpdate(question.getId(), session.getId(), STUDENT_ID))
                .thenReturn(question);
        when(levelMapper.selectById(level.getId())).thenReturn(level);
        when(levelProgressMapper.selectForUpdate(STUDENT_ID, level.getId())).thenReturn(null);
        when(playerProgressMapper.selectForUpdate(STUDENT_ID)).thenReturn(null);
        when(playerProgressMapper.insert(any(GamePlayerProgress.class))).thenReturn(1);
        when(levelProgressMapper.insert(any(GameLevelProgress.class))).thenReturn(1);
        when(rewardRecordMapper.insert(any(GameRewardRecord.class))).thenReturn(1);

        SubmitGameAnswerRequest request = new SubmitGameAnswerRequest(
                "answer-request-001", "3/4", 900);
        GameCommandView.AnswerFeedback first = service.submitAnswer(
                STUDENT_ID, session.getId(), question.getId(), request);
        GameCommandView.AnswerFeedback replay = service.submitAnswer(
                STUDENT_ID, session.getId(), question.getId(), request);

        assertThat(first.accepted()).isTrue();
        assertThat(first.correct()).isTrue();
        assertThat(first.sessionStatus()).isEqualTo("COMPLETED");
        assertThat(first.resultType()).isEqualTo("LEVEL_PASSED");
        assertThat(replay.idempotentReplay()).isTrue();
        assertThat(member.getPointsReward()).isEqualTo(30);
        assertThat(member.getExperienceReward()).isEqualTo(20);
        verify(questionMapper).updateById(question);
        verify(outboxService, times(2)).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void replaysTheSameNextQuestionForAnIdempotentAnswer() {
        GameSession session = levelSession(302L, 12L);
        session.setQuestionCountSnapshot(2);
        GameSessionMember member = member(session.getId(), STUDENT_ID, 1);
        GameSessionQuestion question = sessionQuestion(
                402L, session.getId(), STUDENT_ID, "{\"value\":42}");
        when(sessionMapper.selectForUpdate(session.getId())).thenReturn(session);
        when(memberMapper.selectOne(any())).thenReturn(member);
        when(questionMapper.selectForUpdate(question.getId(), session.getId(), STUDENT_ID))
                .thenReturn(question);
        when(questionMapper.selectNextUnansweredId(session.getId(), STUDENT_ID)).thenReturn(403L);

        SubmitGameAnswerRequest request = new SubmitGameAnswerRequest(
                "answer-request-002", "42", 800);
        GameCommandView.AnswerFeedback first = service.submitAnswer(
                STUDENT_ID, session.getId(), question.getId(), request);
        GameCommandView.AnswerFeedback replay = service.submitAnswer(
                STUDENT_ID, session.getId(), question.getId(), request);

        assertThat(first.nextSessionQuestionId()).isEqualTo(403L);
        assertThat(replay.nextSessionQuestionId()).isEqualTo(403L);
        assertThat(replay.idempotentReplay()).isTrue();
        verify(questionMapper).updateById(question);
        verify(outboxService).append(any(), any(), any(), any(), any(), any());
    }

    @Test
    void matchesTwoPlayersWithSameQuestionSnapshots() {
        GameQuestionProvider.PreparedPk prepared = new GameQuestionProvider.PreparedPk(
                3,
                KNOWLEDGE_POINT_ID,
                3,
                1,
                180,
                List.of(new GameQuestionProvider.PreparedQuestion(
                        question(201L, "{\"value\":42}"), new BigDecimal("100.00"))));
        GameMatchTicket candidate = new GameMatchTicket();
        candidate.setId(501L);
        candidate.setStudentId(OPPONENT_ID);
        candidate.setStatus("WAITING");
        candidate.setVersion(0);
        when(memberMapper.selectActiveSessionId(STUDENT_ID)).thenReturn(null);
        when(matchTicketMapper.selectWaitingByStudentForUpdate(STUDENT_ID)).thenReturn(null);
        when(matchTicketMapper.selectCandidateForUpdate(
                anyLong(), anyInt(), anyLong(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(candidate);
        when(matchTicketMapper.updateById(candidate)).thenReturn(1);
        doAnswer(invocation -> {
            GameSession value = invocation.getArgument(0);
            value.setId(601L);
            return 1;
        }).when(sessionMapper).insert(any(GameSession.class));
        doAnswer(invocation -> {
            GameMatchTicket value = invocation.getArgument(0);
            value.setId(502L);
            return 1;
        }).when(matchTicketMapper).insert(any(GameMatchTicket.class));
        when(memberMapper.insert(any(GameSessionMember.class))).thenReturn(1);
        when(questionMapper.insert(any(GameSessionQuestion.class))).thenReturn(1);

        GameSessionTransactionService.MatchTransactionResult result =
                service.matchRandom(STUDENT_ID, prepared);

        assertThat(result.status()).isEqualTo("MATCHED");
        assertThat(result.sessionId()).isEqualTo(601L);
        assertThat(candidate.getMatchedSessionId()).isEqualTo(601L);
        verify(memberMapper, times(2)).insert(any(GameSessionMember.class));
        verify(questionMapper, times(2)).insert(any(GameSessionQuestion.class));
    }

    @Test
    void settlesPkByScoreAndUpdatesBothRatingsAndRewards() {
        GameSession session = pkSession(701L);
        GameSessionMember first = member(session.getId(), STUDENT_ID, 1);
        GameSessionMember second = member(session.getId(), OPPONENT_ID, 2);
        GameSessionQuestion firstQuestion = sessionQuestion(
                801L, session.getId(), STUDENT_ID, "{\"value\":42}");
        GameSessionQuestion secondQuestion = sessionQuestion(
                802L, session.getId(), OPPONENT_ID, "{\"value\":42}");
        when(sessionMapper.selectForUpdate(session.getId())).thenReturn(session);
        when(memberMapper.selectOne(any())).thenReturn(first, second);
        when(memberMapper.selectList(any())).thenReturn(
                List.of(first, second),
                List.of(first, second),
                List.of(first, second),
                List.of(first, second),
                List.of(first, second));
        when(questionMapper.selectForUpdate(firstQuestion.getId(), session.getId(), STUDENT_ID))
                .thenReturn(firstQuestion);
        when(questionMapper.selectForUpdate(secondQuestion.getId(), session.getId(), OPPONENT_ID))
                .thenReturn(secondQuestion);
        when(playerProgressMapper.selectForUpdate(STUDENT_ID)).thenReturn(null);
        when(playerProgressMapper.selectForUpdate(OPPONENT_ID)).thenReturn(null);
        when(playerProgressMapper.insert(any(GamePlayerProgress.class))).thenReturn(1);
        when(rewardRecordMapper.insert(any(GameRewardRecord.class))).thenReturn(1);

        GameCommandView.AnswerFeedback firstResult = service.submitAnswer(
                STUDENT_ID,
                session.getId(),
                firstQuestion.getId(),
                new SubmitGameAnswerRequest("answer-request-101", "42", 1000));
        GameCommandView.AnswerFeedback secondResult = service.submitAnswer(
                OPPONENT_ID,
                session.getId(),
                secondQuestion.getId(),
                new SubmitGameAnswerRequest("answer-request-102", "41", 900));

        assertThat(firstResult.sessionStatus()).isEqualTo("IN_PROGRESS");
        assertThat(secondResult.sessionStatus()).isEqualTo("COMPLETED");
        assertThat(secondResult.winnerStudentId()).isEqualTo(STUDENT_ID);
        assertThat(first.getResult()).isEqualTo("WIN");
        assertThat(second.getResult()).isEqualTo("LOSE");
        assertThat(first.getRatingAfter()).isGreaterThan(first.getRatingBefore());
        assertThat(second.getRatingAfter()).isLessThan(second.getRatingBefore());
        verify(rewardRecordMapper, times(4)).insert(any(GameRewardRecord.class));
    }

    @Test
    void pkTimeoutDoesNotTreatAnUnansweredPlayerAsFaster() {
        GameSession session = pkSession(702L);
        session.setQuestionCountSnapshot(2);
        session.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        GameSessionMember first = member(session.getId(), STUDENT_ID, 1);
        first.setAnsweredCount(1);
        first.setTotalResponseMs(1000L);
        GameSessionMember second = member(session.getId(), OPPONENT_ID, 2);
        when(sessionMapper.selectForUpdate(session.getId())).thenReturn(session);
        when(memberMapper.selectOne(any())).thenReturn(first);
        when(memberMapper.selectList(any())).thenReturn(List.of(first, second));
        when(playerProgressMapper.selectForUpdate(STUDENT_ID)).thenReturn(null);
        when(playerProgressMapper.selectForUpdate(OPPONENT_ID)).thenReturn(null);
        when(playerProgressMapper.insert(any(GamePlayerProgress.class))).thenReturn(1);
        when(rewardRecordMapper.insert(any(GameRewardRecord.class))).thenReturn(1);

        service.finishSession(STUDENT_ID, session.getId());

        assertThat(session.getStatus()).isEqualTo("COMPLETED");
        assertThat(session.getWinnerStudentId()).isEqualTo(STUDENT_ID);
        assertThat(first.getResult()).isEqualTo("WIN");
        assertThat(second.getResult()).isEqualTo("LOSE");
    }

    private GameLevel level(Long id, int questionCount) {
        GameLevel level = new GameLevel();
        level.setId(id);
        level.setGradeLevel(3);
        level.setKnowledgePointId(KNOWLEDGE_POINT_ID);
        level.setDifficulty(3);
        level.setQuestionCount(questionCount);
        level.setStatus("ENABLED");
        level.setVersion(0);
        level.setTimeLimitSeconds(180);
        level.setPassRate(new BigDecimal("0.8000"));
        level.setBasePoints(10);
        level.setBaseExperience(10);
        return level;
    }

    private GameSession levelSession(Long id, Long levelId) {
        GameSession session = new GameSession();
        session.setId(id);
        session.setLevelId(levelId);
        session.setMode("LEVEL");
        session.setCreatorStudentId(STUDENT_ID);
        session.setStatus("IN_PROGRESS");
        session.setMemberCount(1);
        session.setGradeLevelSnapshot(3);
        session.setKnowledgePointIdSnapshot(KNOWLEDGE_POINT_ID);
        session.setDifficultySnapshot(3);
        session.setQuestionCountSnapshot(1);
        session.setTimeLimitSecondsSnapshot(180);
        session.setPassRateSnapshot(new BigDecimal("0.8000"));
        session.setTeamProgress(BigDecimal.ZERO.setScale(2));
        session.setStartedAt(OffsetDateTime.now(ZoneOffset.UTC).minusSeconds(30));
        session.setExpiresAt(OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(2));
        session.setVersion(0);
        return session;
    }

    private GameSession pkSession(Long id) {
        GameSession session = levelSession(id, null);
        session.setMode("PK");
        session.setMatchMode("RANDOM");
        session.setMemberCount(2);
        session.setPassRateSnapshot(null);
        return session;
    }

    private GameSessionMember member(Long sessionId, Long studentId, int seat) {
        GameSessionMember member = new GameSessionMember();
        member.setSessionId(sessionId);
        member.setStudentId(studentId);
        member.setSeatNo(seat);
        member.setMemberRole(seat == 1 ? "CREATOR" : "OPPONENT");
        member.setStatus("PLAYING");
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
        return member;
    }

    private GameSessionQuestion sessionQuestion(
            Long id, Long sessionId, Long studentId, String standardAnswer) {
        GameSessionQuestion question = new GameSessionQuestion();
        question.setId(id);
        question.setSessionId(sessionId);
        question.setStudentId(studentId);
        question.setSequenceNo(1);
        question.setQuestionId(201L);
        question.setQuestionSnapshot(objectMapper.writeValueAsString(question(201L, standardAnswer)));
        question.setMaxScore(new BigDecimal("100.00"));
        question.setGradingStatus("UNANSWERED");
        question.setProgressDelta(BigDecimal.ZERO.setScale(2));
        question.setEnergyDelta(0);
        return question;
    }

    private QuestionResponse question(Long id, String answer) {
        return new QuestionResponse(
                id, "Q-" + id, "FIXED", "ARITHMETIC", "计算题", null,
                answer, "解析", null, 3, 20, "ENABLED", List.of(KNOWLEDGE_POINT_ID));
    }
}
