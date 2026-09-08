package xyz.jiayuhan.gameservice.service;

import xyz.jiayuhan.gameservice.model.request.CreatePkSessionRequest;
import xyz.jiayuhan.gameservice.model.request.SubmitGameAnswerRequest;
import xyz.jiayuhan.gameservice.model.response.GameCommandView;
import xyz.jiayuhan.gameservice.model.response.GameView;

/** 闯关和双人 PK 的写模型入口。 */
public interface GameCommandService {

    GameView.SessionDetail createLevelSession(Long studentId, Long levelId);

    GameView.SessionDetail createPkRoom(Long studentId, CreatePkSessionRequest request);

    GameView.SessionDetail joinPkRoom(Long studentId, String inviteCode);

    GameCommandView.MatchStatus requestRandomMatch(Long studentId, CreatePkSessionRequest request);

    void cancelRandomMatch(Long studentId, Long ticketId);

    GameView.SessionDetail startSession(Long studentId, Long sessionId);

    GameCommandView.AnswerFeedback submitAnswer(
            Long studentId,
            Long sessionId,
            Long sessionQuestionId,
            SubmitGameAnswerRequest request);

    GameView.SessionDetail finishSession(Long studentId, Long sessionId);

    GameView.SessionDetail cancelSession(Long studentId, Long sessionId);
}
