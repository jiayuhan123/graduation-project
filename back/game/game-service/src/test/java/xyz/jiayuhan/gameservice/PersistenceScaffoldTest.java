package xyz.jiayuhan.gameservice;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.gameservice.entity.GameLevel;
import xyz.jiayuhan.gameservice.entity.GameLevelProgress;
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
        Map<Class<?>, String> mappings = Map.of(
                GameLevel.class, "game_level",
                GameSession.class, "game_session",
                GameSessionMember.class, "game_session_member",
                GameSessionQuestion.class, "game_session_question",
                GamePlayerProgress.class, "game_player_progress",
                GameLevelProgress.class, "game_level_progress",
                GameRewardRecord.class, "game_reward_record");

        mappings.forEach((type, table) ->
                assertEquals(table, type.getAnnotation(TableName.class).value()));
    }
}
