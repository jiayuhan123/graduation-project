package xyz.jiayuhan.gameservice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import xyz.jiayuhan.gameservice.entity.GameEventOutbox;

import java.time.OffsetDateTime;
import java.util.List;

public interface GameEventOutboxMapper extends BaseMapper<GameEventOutbox> {

    @Select("""
            SELECT *
            FROM game_event_outbox
            WHERE delivery_status = 'PENDING'
              AND next_retry_at <= CURRENT_TIMESTAMP
            ORDER BY created_at, id
            LIMIT #{limit}
            FOR UPDATE SKIP LOCKED
            """)
    List<GameEventOutbox> selectPendingBatchForUpdate(@Param("limit") int limit);

    @Delete("""
            DELETE FROM game_event_outbox
            WHERE delivery_status = 'SENT'
              AND sent_at < #{cutoff}
            """)
    int deleteSentBefore(@Param("cutoff") OffsetDateTime cutoff);
}
