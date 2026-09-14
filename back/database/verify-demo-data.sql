-- Portfolio demo dataset verification. Raises an error on incomplete data.

DO $$
DECLARE
    demo_teacher_count INTEGER;
    demo_student_count INTEGER;
    class_member_count INTEGER;
    assignment_count INTEGER;
    completed_submission_count INTEGER;
    answer_count INTEGER;
    mastery_count INTEGER;
    open_error_count INTEGER;
    recommendation_count INTEGER;
    game_session_count INTEGER;
    notification_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO demo_teacher_count
    FROM t_user user_account
    JOIN sys_user_role user_role ON user_role.user_id = user_account.id
    JOIN sys_role role ON role.id = user_role.role_id
    WHERE user_account.username = 'demo_teacher'
      AND user_account.status = 1
      AND user_account.is_deleted = FALSE
      AND role.code = 'teacher';

    SELECT COUNT(*) INTO demo_student_count
    FROM t_user user_account
    JOIN sys_user_role user_role ON user_role.user_id = user_account.id
    JOIN sys_role role ON role.id = user_role.role_id
    WHERE user_account.username = 'demo_student'
      AND user_account.status = 1
      AND user_account.is_deleted = FALSE
      AND role.code = 'student';

    SELECT COUNT(*) INTO class_member_count
    FROM class_student
    WHERE class_id = 820000000000000001 AND status = 'ACTIVE';

    SELECT COUNT(*) INTO assignment_count
    FROM assignment
    WHERE class_id = 820000000000000001;

    SELECT COUNT(*) INTO completed_submission_count
    FROM assignment_submission
    WHERE assignment_id = 821000000000000001 AND status = 'GRADED';

    SELECT COUNT(*) INTO answer_count
    FROM answer_record
    WHERE student_id BETWEEN 810000000000000101 AND 810000000000000112
      AND grading_status = 'GRADED';

    SELECT COUNT(*) INTO mastery_count
    FROM mastery_record
    WHERE student_id BETWEEN 810000000000000101 AND 810000000000000112;

    SELECT COUNT(*) INTO open_error_count
    FROM error_record
    WHERE student_id BETWEEN 810000000000000101 AND 810000000000000112
      AND status <> 'RESOLVED';

    SELECT COUNT(*) INTO recommendation_count
    FROM recommendation
    WHERE student_id = 810000000000000101;

    SELECT COUNT(*) INTO game_session_count
    FROM game_session
    WHERE creator_student_id = 810000000000000101 AND status = 'COMPLETED';

    SELECT COUNT(*) INTO notification_count
    FROM notification
    WHERE user_id IN (810000000000000001, 810000000000000101);

    IF demo_teacher_count <> 1 OR demo_student_count <> 1 THEN
        RAISE EXCEPTION 'Demo login accounts or roles are incomplete';
    END IF;
    IF class_member_count <> 12 THEN
        RAISE EXCEPTION 'Expected 12 active demo class members, got %', class_member_count;
    END IF;
    IF assignment_count <> 3 OR completed_submission_count <> 12 THEN
        RAISE EXCEPTION 'Demo assignment lifecycle is incomplete';
    END IF;
    IF answer_count < 72 OR mastery_count <> 60 OR open_error_count < 10 THEN
        RAISE EXCEPTION 'Demo learning history is incomplete';
    END IF;
    IF recommendation_count <> 3 OR game_session_count <> 1 OR notification_count <> 4 THEN
        RAISE EXCEPTION 'Demo recommendation, game, or notification data is incomplete';
    END IF;

    RAISE NOTICE 'Demo data verified: % students, % assignments, % graded submissions, % answers, % mastery records, % active errors, % recommendations, % completed game sessions, % notifications',
        class_member_count, assignment_count, completed_submission_count,
        answer_count, mastery_count, open_error_count, recommendation_count,
        game_session_count, notification_count;
END
$$;

SELECT
    (SELECT COUNT(*) FROM class_student
     WHERE class_id = 820000000000000001 AND status = 'ACTIVE') AS students,
    (SELECT COUNT(*) FROM assignment
     WHERE class_id = 820000000000000001) AS assignments,
    (SELECT COUNT(*) FROM practice_task
     WHERE id BETWEEN 831000000000000000 AND 831999999999999999) AS practice_tasks,
    (SELECT COUNT(*) FROM answer_record
     WHERE id BETWEEN 833000000000000000 AND 833999999999999999) AS answers,
    (SELECT COUNT(*) FROM mastery_record
     WHERE id BETWEEN 836000000000000000 AND 836999999999999999) AS mastery_records,
    (SELECT COUNT(*) FROM error_record
     WHERE id BETWEEN 834000000000000000 AND 834999999999999999) AS error_records,
    (SELECT COUNT(*) FROM recommendation
     WHERE id BETWEEN 835000000000000000 AND 835999999999999999) AS recommendations,
    (SELECT COUNT(*) FROM game_session
     WHERE id BETWEEN 840000000000000000 AND 840999999999999999) AS game_sessions,
    (SELECT COUNT(*) FROM notification
     WHERE id::TEXT LIKE '85000000-0000-0000-0000-%') AS notifications;

