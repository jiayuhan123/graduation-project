-- Existing learning_db upgrade for the online learning closure.
-- Safe to execute repeatedly after the original schema.sql.

BEGIN;

CREATE UNIQUE INDEX IF NOT EXISTS uk_practice_task_assignment_student
ON practice_task (student_id, source_type, source_id)
WHERE source_type = 'ASSIGNMENT' AND source_id IS NOT NULL;

COMMIT;
