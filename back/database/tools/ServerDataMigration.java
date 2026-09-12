import java.sql.Array;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 将同一 PostgreSQL 实例上的八个旧业务库复制到模块化单体库。
 *
 * <p>旧库连接始终处于只读、可重复读事务；目标库在单个事务内清空并复制，任何异常都会回滚。</p>
 */
public final class ServerDataMigration {

    private static final String TARGET_DATABASE = "monolith_db";
    private static final int EXPECTED_TABLE_COUNT = 47;
    private static final int BATCH_SIZE = 200;

    private static final Map<String, List<String>> SOURCE_TABLES = createSourceTables();

    private ServerDataMigration() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1 || !(args[0].equals("inspect") || args[0].equals("migrate"))) {
            throw new IllegalArgumentException("Usage: ServerDataMigration inspect|migrate");
        }

        DriverManager.setLoginTimeout(10);
        var environment = Environment.load();
        try (var target = connect(environment, TARGET_DATABASE)) {
            validateTarget(target);
            if (args[0].equals("inspect")) {
                inspect(environment, target);
            } else {
                migrate(environment, target);
            }
        }
    }

    private static void inspect(Environment environment, Connection target) throws Exception {
        long sourceTotal = 0;
        long targetTotal = 0;
        for (var sourceEntry : SOURCE_TABLES.entrySet()) {
            try (var source = openReadOnlySnapshot(environment, sourceEntry.getKey())) {
                System.out.println("Database " + sourceEntry.getKey() + ':');
                for (var table : sourceEntry.getValue()) {
                    boolean sourceTableExists = validateCompatibleColumns(source, target, table);
                    long sourceRows = sourceTableExists ? countRows(source, table) : 0;
                    long targetRows = countRows(target, table);
                    sourceTotal += sourceRows;
                    targetTotal += targetRows;
                    String sourceDisplay = sourceTableExists ? Long.toString(sourceRows) : "missing (treated as 0)";
                    System.out.printf("- %-34s source=%s target=%d%n", table, sourceDisplay, targetRows);
                }
                source.rollback();
            }
        }
        System.out.println("Compatible tables=" + EXPECTED_TABLE_COUNT
                + " sourceRows=" + sourceTotal + " targetRows=" + targetTotal);
    }

    private static void migrate(Environment environment, Connection target) throws Exception {
        var sources = new LinkedHashMap<String, Connection>();
        target.setAutoCommit(false);
        target.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);

        try {
            lockMigration(target);
            for (var database : SOURCE_TABLES.keySet()) {
                sources.put(database, openReadOnlySnapshot(environment, database));
            }

            var sourceCounts = new LinkedHashMap<String, Long>();
            for (var sourceEntry : SOURCE_TABLES.entrySet()) {
                var source = sources.get(sourceEntry.getKey());
                for (var table : sourceEntry.getValue()) {
                    boolean sourceTableExists = validateCompatibleColumns(source, target, table);
                    sourceCounts.put(table, sourceTableExists ? countRows(source, table) : 0L);
                }
            }

            truncateTarget(target);

            long copiedTotal = 0;
            for (var sourceEntry : SOURCE_TABLES.entrySet()) {
                var source = sources.get(sourceEntry.getKey());
                for (var table : sourceEntry.getValue()) {
                    long copied = copyTable(source, target, table);
                    copiedTotal += copied;
                    System.out.printf("Copied %-34s rows=%d%n", table, copied);
                }
            }

            resetIdentity(target, "sys_role", "id");
            resetIdentity(target, "sys_permission", "id");

            for (var countEntry : sourceCounts.entrySet()) {
                long targetRows = countRows(target, countEntry.getKey());
                if (targetRows != countEntry.getValue()) {
                    throw new IllegalStateException("Row-count mismatch for " + countEntry.getKey()
                            + ": source=" + countEntry.getValue() + ", target=" + targetRows);
                }
            }

            target.commit();
            System.out.println("Migration committed: tables=" + sourceCounts.size() + " rows=" + copiedTotal);
        } catch (Exception exception) {
            target.rollback();
            System.err.println("Migration rolled back: " + exception.getMessage());
            throw exception;
        } finally {
            for (var source : sources.values()) {
                try {
                    source.rollback();
                } finally {
                    source.close();
                }
            }
            target.setAutoCommit(true);
        }
    }

    private static Connection openReadOnlySnapshot(Environment environment, String database) throws SQLException {
        var connection = connect(environment, database);
        connection.setAutoCommit(false);
        connection.setTransactionIsolation(Connection.TRANSACTION_REPEATABLE_READ);
        connection.setReadOnly(true);
        return connection;
    }

    private static Connection connect(Environment environment, String database) throws SQLException {
        String url = "jdbc:postgresql://" + environment.host() + ':' + environment.port() + '/' + database
                + "?connectTimeout=10&socketTimeout=120&tcpKeepAlive=true&ApplicationName=monolith-data-migration";
        return DriverManager.getConnection(url, environment.username(), environment.password());
    }

    private static void validateTarget(Connection target) throws SQLException {
        String query = """
                SELECT count(*)
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_type = 'BASE TABLE'
                """;
        try (var statement = target.prepareStatement(query); var resultSet = statement.executeQuery()) {
            resultSet.next();
            int tableCount = resultSet.getInt(1);
            if (tableCount != EXPECTED_TABLE_COUNT) {
                throw new IllegalStateException(
                        "Target database must contain 47 public tables, found " + tableCount);
            }
        }
    }

    private static boolean validateCompatibleColumns(Connection source, Connection target, String table)
            throws SQLException {
        var sourceColumns = readColumns(source, table);
        var targetColumns = readColumns(target, table);
        if (targetColumns.isEmpty()) {
            throw new IllegalStateException("Target table is missing: " + table);
        }
        if (sourceColumns.isEmpty()) {
            return false;
        }
        var sourceTypes = new LinkedHashMap<String, String>();
        var targetTypes = new LinkedHashMap<String, String>();
        sourceColumns.forEach(column -> sourceTypes.put(column.name(), column.databaseType()));
        targetColumns.forEach(column -> targetTypes.put(column.name(), column.databaseType()));
        if (!sourceTypes.equals(targetTypes)) {
            throw new IllegalStateException("Column mismatch for " + table
                    + "; source=" + sourceTypes + "; target=" + targetTypes);
        }
        return true;
    }

    private static List<Column> readColumns(Connection connection, String table) throws SQLException {
        String query = """
                SELECT column_name, udt_name
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = ?
                ORDER BY ordinal_position
                """;
        var columns = new ArrayList<Column>();
        try (var statement = connection.prepareStatement(query)) {
            statement.setString(1, table);
            try (var resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    columns.add(new Column(resultSet.getString(1), resultSet.getString(2)));
                }
            }
        }
        return columns;
    }

    private static long countRows(Connection connection, String table) throws SQLException {
        try (var statement = connection.createStatement();
             var resultSet = statement.executeQuery("SELECT count(*) FROM " + quote(table))) {
            resultSet.next();
            return resultSet.getLong(1);
        }
    }

    private static void lockMigration(Connection target) throws SQLException {
        try (var statement = target.createStatement()) {
            statement.execute("SELECT pg_advisory_xact_lock(hashtext('graduation-project-monolith-migration'))");
        }
    }

    private static void truncateTarget(Connection target) throws SQLException {
        String tables = SOURCE_TABLES.values().stream()
                .flatMap(List::stream)
                .map(ServerDataMigration::quote)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        try (var statement = target.createStatement()) {
            statement.execute("TRUNCATE TABLE " + tables + " RESTART IDENTITY CASCADE");
        }
        System.out.println("Target tables truncated inside migration transaction.");
    }

    private static long copyTable(Connection source, Connection target, String table) throws SQLException {
        var columns = readColumns(source, table);
        if (columns.isEmpty()) {
            return 0;
        }
        String columnList = columns.stream()
                .map(Column::name)
                .map(ServerDataMigration::quote)
                .reduce((left, right) -> left + ", " + right)
                .orElseThrow();
        String placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
        String selectSql = "SELECT " + columnList + " FROM " + quote(table);
        String insertSql = "INSERT INTO " + quote(table) + " (" + columnList + ") VALUES (" + placeholders + ')';

        long copied = 0;
        int pending = 0;
        try (var select = source.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
             var insert = target.prepareStatement(insertSql)) {
            select.setFetchSize(BATCH_SIZE);
            try (var resultSet = select.executeQuery(selectSql)) {
                var metadata = resultSet.getMetaData();
                while (resultSet.next()) {
                    for (int index = 1; index <= columns.size(); index++) {
                        Object value = resultSet.getObject(index);
                        if (value == null) {
                            insert.setNull(index, jdbcNullType(metadata.getColumnType(index)));
                        } else if (value instanceof Array array) {
                            Object arrayValue = array.getArray();
                            insert.setArray(index, target.createArrayOf(array.getBaseTypeName(), (Object[]) arrayValue));
                        } else {
                            insert.setObject(index, value);
                        }
                    }
                    insert.addBatch();
                    copied++;
                    pending++;
                    if (pending == BATCH_SIZE) {
                        insert.executeBatch();
                        pending = 0;
                    }
                }
                if (pending > 0) {
                    insert.executeBatch();
                }
            }
        }
        return copied;
    }

    private static int jdbcNullType(int sourceType) {
        return sourceType == Types.OTHER ? Types.OTHER : sourceType;
    }

    private static void resetIdentity(Connection target, String table, String column) throws SQLException {
        String sql = "SELECT setval(pg_get_serial_sequence('public." + table + "', '" + column + "'), "
                + "COALESCE((SELECT MAX(" + quote(column) + ") FROM " + quote(table) + "), 1), "
                + "EXISTS (SELECT 1 FROM " + quote(table) + "))";
        try (var statement = target.createStatement()) {
            statement.execute(sql);
        }
    }

    private static String quote(String identifier) {
        if (!identifier.matches("[a-z][a-z0-9_]*")) {
            throw new IllegalArgumentException("Unsafe SQL identifier: " + identifier);
        }
        return '"' + identifier + '"';
    }

    private static Map<String, List<String>> createSourceTables() {
        var tables = new LinkedHashMap<String, List<String>>();
        tables.put("user_db", List.of(
                "t_user", "sys_student_profile", "sys_teacher_profile", "sys_role", "sys_permission",
                "sys_user_role", "sys_role_permission", "sys_outbox_event"));
        tables.put("content_db", List.of(
                "content_module", "knowledge_point", "question", "question_knowledge_point", "error_type",
                "question_feedback", "content_outbox_event"));
        tables.put("learning_db", List.of(
                "practice_task", "practice_task_knowledge_point", "practice_task_question", "answer_record",
                "error_record", "mastery_record", "mastery_change_log", "recommendation",
                "assignment_sync_event", "photo_marking_task", "photo_marking_item", "photo_review_record"));
        tables.put("teaching_db", List.of(
                "teaching_class", "class_teacher", "class_student", "assignment", "assignment_question",
                "assignment_submission", "teaching_outbox_event"));
        tables.put("game_db", List.of(
                "game_level", "game_level_question", "game_session", "game_match_ticket", "game_session_member",
                "game_session_question", "game_player_progress", "game_level_progress", "game_reward_record",
                "game_event_outbox"));
        tables.put("file_db", List.of("file_object"));
        tables.put("notification_db", List.of("notification"));
        tables.put("audit_db", List.of("audit_log"));
        return Collections.unmodifiableMap(tables);
    }

    private record Column(String name, String databaseType) {
        private Column {
            Objects.requireNonNull(name);
            Objects.requireNonNull(databaseType);
        }
    }

    private record Environment(String host, String port, String username, String password) {
        private static Environment load() {
            return new Environment(
                    required("DB_HOST"),
                    required("POSTGRES_PORT"),
                    required("DB_USERNAME"),
                    required("DB_PASSWORD"));
        }

        private static String required(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Missing environment variable: " + name);
            }
            return value;
        }
    }
}
