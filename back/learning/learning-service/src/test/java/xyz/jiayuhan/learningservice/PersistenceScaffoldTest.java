package xyz.jiayuhan.learningservice;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.learningservice.entity.AnswerRecord;
import xyz.jiayuhan.learningservice.entity.AssignmentSyncEvent;
import xyz.jiayuhan.learningservice.entity.ErrorRecord;
import xyz.jiayuhan.learningservice.entity.MasteryChangeLog;
import xyz.jiayuhan.learningservice.entity.MasteryRecord;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingItem;
import xyz.jiayuhan.learningservice.entity.PhotoMarkingTask;
import xyz.jiayuhan.learningservice.entity.PhotoReviewRecord;
import xyz.jiayuhan.learningservice.entity.PracticeTask;
import xyz.jiayuhan.learningservice.entity.PracticeTaskKnowledgePoint;
import xyz.jiayuhan.learningservice.entity.PracticeTaskQuestion;
import xyz.jiayuhan.learningservice.entity.Recommendation;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PersistenceScaffoldTest {

    @Test
    void mapsEveryLearningTable() {
        Map<Class<?>, String> mappings = Map.ofEntries(
                Map.entry(PracticeTask.class, "practice_task"),
                Map.entry(PracticeTaskKnowledgePoint.class, "practice_task_knowledge_point"),
                Map.entry(PracticeTaskQuestion.class, "practice_task_question"),
                Map.entry(AnswerRecord.class, "answer_record"),
                Map.entry(ErrorRecord.class, "error_record"),
                Map.entry(MasteryRecord.class, "mastery_record"),
                Map.entry(MasteryChangeLog.class, "mastery_change_log"),
                Map.entry(Recommendation.class, "recommendation"),
                Map.entry(AssignmentSyncEvent.class, "assignment_sync_event"),
                Map.entry(PhotoMarkingTask.class, "photo_marking_task"),
                Map.entry(PhotoMarkingItem.class, "photo_marking_item"),
                Map.entry(PhotoReviewRecord.class, "photo_review_record"));

        mappings.forEach((type, table) ->
                assertEquals(table, type.getAnnotation(TableName.class).value()));
    }
}
