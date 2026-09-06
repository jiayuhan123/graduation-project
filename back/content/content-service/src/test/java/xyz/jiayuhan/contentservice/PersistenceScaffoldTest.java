package xyz.jiayuhan.contentservice;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.contentservice.entity.ContentModule;
import xyz.jiayuhan.contentservice.entity.ContentOutboxEvent;
import xyz.jiayuhan.contentservice.entity.ErrorType;
import xyz.jiayuhan.contentservice.entity.KnowledgePoint;
import xyz.jiayuhan.contentservice.entity.Question;
import xyz.jiayuhan.contentservice.entity.QuestionKnowledgePoint;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceScaffoldTest {

    @Test
    void mapsEveryContentTable() {
        Map<Class<?>, String> mappings = Map.of(
                ContentModule.class, "content_module",
                KnowledgePoint.class, "knowledge_point",
                Question.class, "question",
                QuestionKnowledgePoint.class, "question_knowledge_point",
                ErrorType.class, "error_type",
                ContentOutboxEvent.class, "content_outbox_event");

        mappings.forEach((type, table) ->
                assertEquals(table, type.getAnnotation(TableName.class).value()));
    }
}
