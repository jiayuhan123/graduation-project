package xyz.jiayuhan.gameservice.service;

import xyz.jiayuhan.common.core.model.PageResult;
import xyz.jiayuhan.gameapi.model.response.GameSessionSummaryResponse;
import xyz.jiayuhan.gameservice.model.response.GameView;

import java.util.List;

/** 关卡、对局、进度和奖励的只读应用服务。 */
public interface GameQueryService {

    PageResult<GameView.Level> pageLevels(
            long page, long size, Integer gradeLevel, Long knowledgePointId, String mode);

    GameView.Level getLevel(Long levelId);

    PageResult<GameSessionSummaryResponse> pageMySessions(
            Long studentId, long page, long size, String status);

    GameView.SessionDetail getSession(Long studentId, Long sessionId);

    GameSessionSummaryResponse getSessionInternal(Long sessionId);

    GameView.PlayerProgress getPlayerProgress(Long studentId);

    List<GameView.LevelProgress> listLevelProgress(Long studentId, String status);

    PageResult<GameView.Reward> pageRewards(Long studentId, long page, long size);
}
