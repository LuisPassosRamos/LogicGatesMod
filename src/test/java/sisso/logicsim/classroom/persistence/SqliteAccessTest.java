package sisso.logicsim.classroom.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Proxy;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.persistence.SqliteAccess.*;

class SqliteAccessTest {
    @TempDir Path directory;

    @Test void connectionConfigurationFailureClosesTheConnectionAndPreservesTheCause() throws Exception {
        try (var probe = new ClassroomJdbcProbe()) {
            var failure = new IllegalStateException("configuration failed");
            probe.before = sql -> {
                if (sql.startsWith("PRAGMA busy_timeout")) throw failure;
            };
            assertSame(failure, assertThrows(IllegalStateException.class,
                    () -> new SqliteAccess(directory.resolve("failed.db")).connect()));
            assertEquals(1, probe.counts().get("closed"));
            assertEquals(0, probe.counts().get("active"));
        }
    }

    @Test void sharedConfigurationBindingsAndPaginationPreserveDataAndBoundResults() throws Exception {
        Class.forName("org.sqlite.JDBC");
        var database = new SqliteAccess(directory.resolve("classroom.db"));
        UUID id = UUID.randomUUID();
        try (var connection = database.connect()) {
            assertEquals(1, one(connection, "PRAGMA foreign_keys", row -> row.getInt(1)).orElseThrow());
            assertEquals(5000, one(connection, "PRAGMA busy_timeout", row -> row.getInt(1)).orElseThrow());
            execute(connection, "CREATE TABLE sample(id TEXT, position INTEGER, name TEXT)");
            transaction(connection, Transaction.WRITE, c -> {
                for (int i = 0; i < 21; i++) execute(c, "INSERT INTO sample VALUES(?,?,?)", id, i, i == 0 ? null : "literal ' text");
                return null;
            });
            var page = page(connection, "SELECT position FROM sample WHERE id=? ORDER BY position LIMIT 21", row -> row.getInt(1), 0, id);
            assertEquals(20, page.items().size());
            assertTrue(page.hasNext());
            assertThrows(UnsupportedOperationException.class, () -> page.items().clear());
            assertEquals("literal ' text", one(connection, "SELECT name FROM sample WHERE position=?", row -> row.getString(1), 1).orElseThrow());
            assertEquals(1, one(connection, "SELECT count(*) FROM sample WHERE name IS NULL", row -> row.getInt(1)).orElseThrow());
        }
    }

    @Test void rollbackFailureIsSuppressedAndWriteActionFailureRemainsPrimary() throws Exception {
        var statements = new ArrayList<String>();
        var rollbackFailure = new SQLException("rollback failed");
        var actionFailure = new IllegalArgumentException("invalid operation");
        var connection = connection(statements, "ROLLBACK", rollbackFailure);
        assertSame(actionFailure, assertThrows(IllegalArgumentException.class,
                () -> transaction(connection, Transaction.WRITE, c -> { throw actionFailure; })));
        assertEquals(List.of("BEGIN IMMEDIATE", "ROLLBACK"), statements);
        assertArrayEquals(new Throwable[]{rollbackFailure}, actionFailure.getSuppressed());
    }

    @Test void failedCommitAndUnexpectedErrorBothRollBackWithoutLosingTheOriginalFailure() throws Exception {
        var statements = new ArrayList<String>();
        var commitFailure = new SQLException("commit failed");
        var connection = connection(statements, "COMMIT", commitFailure);
        assertSame(commitFailure, assertThrows(SQLException.class,
                () -> transaction(connection, Transaction.READ, c -> 42)));
        assertEquals(List.of("BEGIN DEFERRED", "COMMIT", "ROLLBACK"), statements);

        statements.clear();
        var error = new AssertionError("action invariant");
        assertSame(error, assertThrows(AssertionError.class,
                () -> transaction(connection, Transaction.WRITE, c -> { throw error; })));
        assertEquals(List.of("BEGIN IMMEDIATE", "ROLLBACK"), statements);
    }

    private static Connection connection(List<String> statements, String failOn, SQLException failure) {
        var statement = (Statement) Proxy.newProxyInstance(SqliteAccessTest.class.getClassLoader(), new Class<?>[]{Statement.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("execute")) {
                        String sql = (String) args[0];
                        statements.add(sql);
                        if (sql.equals(failOn)) throw failure;
                        return false;
                    }
                    if (method.getName().equals("close")) return null;
                    throw new UnsupportedOperationException(method.getName());
                });
        return (Connection) Proxy.newProxyInstance(SqliteAccessTest.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("createStatement")) return statement;
                    throw new UnsupportedOperationException(method.getName());
                });
    }
}
