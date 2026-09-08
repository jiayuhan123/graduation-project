package xyz.jiayuhan.gameservice;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.gameservice.entity.GameEventOutbox;
import xyz.jiayuhan.gameservice.entity.GameLevel;
import xyz.jiayuhan.gameservice.entity.GameLevelProgress;
import xyz.jiayuhan.gameservice.entity.GameLevelQuestion;
import xyz.jiayuhan.gameservice.entity.GameMatchTicket;
import xyz.jiayuhan.gameservice.entity.GamePlayerProgress;
import xyz.jiayuhan.gameservice.entity.GameRewardRecord;
import xyz.jiayuhan.gameservice.entity.GameSession;
import xyz.jiayuhan.gameservice.entity.GameSessionMember;
import xyz.jiayuhan.gameservice.entity.GameSessionQuestion;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceScaffoldTest {

    @Test
    void mapsEveryGameTable() {
        Map<Class<?>, String> mappings = Map.ofEntries(
                Map.entry(GameLevel.class, "game_level"),
                Map.entry(GameLevelQuestion.class, "game_level_question"),
                Map.entry(GameSession.class, "game_session"),
                Map.entry(GameMatchTicket.class, "game_match_ticket"),
                Map.entry(GameSessionMember.class, "game_session_member"),
                Map.entry(GameSessionQuestion.class, "game_session_question"),
                Map.entry(GamePlayerProgress.class, "game_player_progress"),
                Map.entry(GameLevelProgress.class, "game_level_progress"),
                Map.entry(GameRewardRecord.class, "game_reward_record"),
                Map.entry(GameEventOutbox.class, "game_event_outbox"));

        mappings.forEach((type, table) ->
                assertEquals(table, type.getAnnotation(TableName.class).value()));
    }
}
