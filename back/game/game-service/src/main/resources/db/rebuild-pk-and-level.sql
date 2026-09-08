-- Destructive rebuild for an EMPTY development or test game_db only.
-- psql resolves the relative include from this file's directory.
-- Never run this script against a database that contains data requiring preservation.

\set ON_ERROR_STOP on

BEGIN;

SET LOCAL lock_timeout = '10s';

DO $$
DECLARE
    game_table RECORD;
    row_count BIGINT;
BEGIN
    FOR game_table IN
        SELECT tablename
        FROM pg_tables
        WHERE schemaname = 'public'
          AND tablename LIKE 'game\_%' ESCAPE '\'
    LOOP
        EXECUTE format('SELECT COUNT(*) FROM public.%I', game_table.tablename)
        INTO row_count;
        IF row_count > 0 THEN
            RAISE EXCEPTION 'Refusing to rebuild non-empty table public.% (% rows)',
                game_table.tablename, row_count;
        END IF;
    END LOOP;
END
$$;

DROP TABLE IF EXISTS game_event_outbox CASCADE;
DROP TABLE IF EXISTS game_reward_record CASCADE;
DROP TABLE IF EXISTS game_level_progress CASCADE;
DROP TABLE IF EXISTS game_player_progress CASCADE;
DROP TABLE IF EXISTS game_session_question CASCADE;
DROP TABLE IF EXISTS game_session_member CASCADE;
DROP TABLE IF EXISTS game_match_ticket CASCADE;
DROP TABLE IF EXISTS game_session CASCADE;
DROP TABLE IF EXISTS game_level_question CASCADE;
DROP TABLE IF EXISTS game_level CASCADE;

\ir schema.sql

COMMIT;
