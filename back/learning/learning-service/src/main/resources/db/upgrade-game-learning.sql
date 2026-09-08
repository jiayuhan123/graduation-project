-- 将游戏逐题结果可靠同步到统一学习档案。
-- 执行前请备份 learning_db；脚本可重复执行。
BEGIN;

ALTER TABLE practice_task
    DROP CONSTRAINT IF EXISTS ck_practice_task_source;
ALTER TABLE practice_task
    ADD CONSTRAINT ck_practice_task_source CHECK (source_type IN
        ('SELF', 'RECOMMENDATION', 'ASSIGNMENT', 'GAME', 'GAME_REMEDIAL', 'PHOTO'));

CREATE UNIQUE INDEX IF NOT EXISTS uk_practice_task_game_student
    ON practice_task (student_id, source_type, source_id)
    WHERE source_type = 'GAME' AND source_id IS NOT NULL;

COMMIT;
