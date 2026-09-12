\set ON_ERROR_STOP on

BEGIN;

\ir ../user/user-service/src/main/resources/db/schema.sql
\ir ../content/content-service/src/main/resources/db/schema.sql
\ir ../learning/learning-service/src/main/resources/db/schema.sql
\ir ../teaching/teaching-service/src/main/resources/db/schema.sql
\ir ../game/game-service/src/main/resources/db/schema.sql
\ir ../file-service/src/main/resources/db/schema.sql
\ir ../notification-service/src/main/resources/db/schema.sql
\ir ../audit-service/src/main/resources/db/schema.sql

COMMIT;
