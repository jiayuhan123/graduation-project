\set ON_ERROR_STOP on

BEGIN;

\ir ../user/user-service/src/main/resources/db/seed.sql
\ir ../content/content-service/src/main/resources/db/seed-grade1-pep.sql
\ir ../content/content-service/src/main/resources/db/seed-grade2-pep.sql
\ir ../content/content-service/src/main/resources/db/seed-grade3-pep.sql
\ir ../content/content-service/src/main/resources/db/seed-grade4-pep.sql
\ir ../content/content-service/src/main/resources/db/seed-grade5-pep.sql
\ir ../content/content-service/src/main/resources/db/seed-grade6-pep.sql
\ir ../game/game-service/src/main/resources/db/seed-pep-levels.sql

COMMIT;
