package xyz.jiayuhan.teachingservice;

import com.baomidou.mybatisplus.annotation.TableName;
import org.junit.jupiter.api.Test;
import xyz.jiayuhan.teachingservice.entity.Assignment;
import xyz.jiayuhan.teachingservice.entity.AssignmentQuestion;
import xyz.jiayuhan.teachingservice.entity.AssignmentSubmission;
import xyz.jiayuhan.teachingservice.entity.ClassStudent;
import xyz.jiayuhan.teachingservice.entity.ClassTeacher;
import xyz.jiayuhan.teachingservice.entity.TeachingClass;
import xyz.jiayuhan.teachingservice.entity.TeachingOutboxEvent;

import java.util.Map;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PersistenceScaffoldTest {

    @Test
    void mapsEveryTeachingTable() {
        Map<Class<?>, String> mappings = Map.of(
                TeachingClass.class, "teaching_class",
                ClassTeacher.class, "class_teacher",
                ClassStudent.class, "class_student",
                Assignment.class, "assignment",
                AssignmentQuestion.class, "assignment_question",
                AssignmentSubmission.class, "assignment_submission",
                TeachingOutboxEvent.class, "teaching_outbox_event");

        mappings.forEach((type, table) ->
                assertEquals(table, type.getAnnotation(TableName.class).value()));
    }

    @Test
    void schemaDoesNotDeclarePhysicalForeignKeys() throws Exception {
        try (var input = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
            assertNotNull(input);
            String schema = new String(input.readAllBytes(), StandardCharsets.UTF_8).toUpperCase();
            assertFalse(schema.contains("FOREIGN KEY"));
            assertFalse(schema.contains("REFERENCES "));
        }
    }
}
