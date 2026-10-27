package sisso.logicsim.classroom.persistence;

import sisso.logicsim.classroom.domain.ClassroomViews.Page;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Shared JDBC mechanics. Callers retain authorization, SQL and domain-specific failure handling. */
public final class SqliteAccess {
    public static final int PAGE_SIZE = 20;

    public enum Transaction {
        READ("BEGIN DEFERRED"), WRITE("BEGIN IMMEDIATE");

        private final String statement;

        Transaction(String statement) {
            this.statement = statement;
        }
    }

    @FunctionalInterface
    public interface Action<T> {
        T run(Connection connection) throws Exception;
    }

    @FunctionalInterface
    public interface Row<T> {
        T read(ResultSet result) throws SQLException;
    }

    private final String url;

    public SqliteAccess(Path database) {
        url = "jdbc:sqlite:" + database.toAbsolutePath().normalize();
    }

    /** The caller owns the connection. Failed initialization also closes it. */
    public Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(url);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException | RuntimeException | Error failure) {
            try {
                connection.close();
            } catch (SQLException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
        return connection;
    }

    public static <T> T transaction(Connection connection, Transaction mode, Action<T> action) throws Exception {
        try (var statement = connection.createStatement()) {
            statement.execute(mode.statement);
            try {
                T result = action.run(connection);
                statement.execute("COMMIT");
                return result;
            } catch (Exception | Error failure) {
                try {
                    statement.execute("ROLLBACK");
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    public static void execute(Connection connection, String sql, Object... arguments) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            statement.executeUpdate();
        }
    }

    public static <T> List<T> query(Connection connection, String sql, Row<T> row, Object... arguments) throws SQLException {
        try (var statement = connection.prepareStatement(sql)) {
            bind(statement, arguments);
            try (var result = statement.executeQuery()) {
                var rows = new ArrayList<T>();
                while (result.next()) {
                    rows.add(row.read(result));
                }
                return rows;
            }
        }
    }

    public static <T> Optional<T> one(Connection connection, String sql, Row<T> row, Object... arguments) throws SQLException {
        return query(connection, sql, row, arguments).stream().findFirst();
    }

    public static <T> Page<T> page(Connection connection, String sql, Row<T> row, int number, Object... arguments) throws SQLException {
        var rows = query(connection, sql, row, arguments);
        return new Page<>(rows.subList(0, Math.min(rows.size(), PAGE_SIZE)), number, rows.size() > PAGE_SIZE);
    }

    private static void bind(PreparedStatement statement, Object... arguments) throws SQLException {
        for (int index = 0; index < arguments.length; index++) {
            Object value = arguments[index];
            statement.setObject(index + 1, value instanceof UUID ? value.toString() : value);
        }
    }
}
