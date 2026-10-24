package sisso.logicsim.classroom.persistence;

import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

/** Each operation owns its connection. BEGIN IMMEDIATE serializes cross-connection writers. */
public final class SqliteClassroomRepository implements ClassroomRepository {
    private static final int APPLICATION_ID = 0x4c47434c; // LGCL
    public static final int SCHEMA_VERSION = 1;
    private final String url;

    public SqliteClassroomRepository(Path path) {
        path = path.toAbsolutePath().normalize();
        url = "jdbc:sqlite:" + path;
        try {
            Files.createDirectories(path.getParent());
            Class.forName("org.sqlite.JDBC");
            transaction(connection -> {
                int version = pragma(connection, "user_version");
                int application = pragma(connection, "application_id");
                if (version > SCHEMA_VERSION || (application != 0 && application != APPLICATION_ID)) {
                    throw new SQLException("Unsupported Classroom database version or application ID");
                }
                if (version == 0) {
                    try (var statement = connection.createStatement();
                         var tables = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                        if (tables.next()) throw new SQLException("Refusing to migrate an unversioned non-empty database");
                    }
                    try (var statement = connection.createStatement()) {
                        statement.execute("""
                                CREATE TABLE classrooms (
                                  id TEXT PRIMARY KEY NOT NULL,
                                  teacher_uuid TEXT NOT NULL UNIQUE,
                                  name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 64),
                                  description TEXT NOT NULL CHECK(length(description) <= 2048),
                                  revision INTEGER NOT NULL CHECK(revision >= 0))
                                """);
                        statement.execute("""
                                CREATE TABLE classroom_memberships (
                                  id TEXT PRIMARY KEY NOT NULL,
                                  classroom_id TEXT NOT NULL REFERENCES classrooms(id),
                                  player_uuid TEXT NOT NULL,
                                  role TEXT NOT NULL CHECK(role IN ('TEACHER','STUDENT')),
                                  active INTEGER NOT NULL CHECK(active IN (0,1)))
                                """);
                        statement.execute("CREATE UNIQUE INDEX one_active_classroom_per_player ON classroom_memberships(player_uuid) WHERE active=1");
                        statement.execute("CREATE UNIQUE INDEX one_active_teacher_per_classroom ON classroom_memberships(classroom_id) WHERE active=1 AND role='TEACHER'");
                        statement.execute("PRAGMA application_id=" + APPLICATION_ID);
                        statement.execute("PRAGMA user_version=" + SCHEMA_VERSION);
                    }
                } else if (application != APPLICATION_ID) {
                    throw new SQLException("Classroom database application ID missing");
                }
                // Detect damaged/incomplete v1 schemas instead of serving an empty replacement.
                try (var statement = connection.createStatement()) {
                    statement.executeQuery("SELECT id,teacher_uuid,name,description,revision FROM classrooms LIMIT 0").close();
                    statement.executeQuery("SELECT id,classroom_id,player_uuid,role,active FROM classroom_memberships LIMIT 0").close();
                    for (String index : new String[]{"one_active_classroom_per_player", "one_active_teacher_per_classroom"}) {
                        try (var rows = statement.executeQuery("SELECT name FROM sqlite_master WHERE type='index' AND name='" + index + "'")) {
                            if (!rows.next()) throw new SQLException("Missing Classroom constraint " + index);
                        }
                    }
                    try (var rows = statement.executeQuery("PRAGMA foreign_key_check")) {
                        if (rows.next()) throw new SQLException("Broken Classroom foreign key");
                    }
                }
                return null;
            });
        } catch (Exception failure) {
            throw new ClassroomException(UNAVAILABLE, failure);
        }
    }

    private Connection connect() throws SQLException {
        var connection = DriverManager.getConnection(url);
        try (var statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys=ON");
            statement.execute("PRAGMA busy_timeout=5000");
        } catch (SQLException failure) {
            connection.close();
            throw failure;
        }
        return connection;
    }

    private static int pragma(Connection connection, String name) throws SQLException {
        try (var statement = connection.createStatement(); var rows = statement.executeQuery("PRAGMA " + name)) {
            if (!rows.next()) throw new SQLException("Missing PRAGMA " + name);
            return rows.getInt(1);
        }
    }

    @Override public Optional<Classroom> findByMember(UUID playerId) {
        try (var connection = connect()) { return findByMember(connection, playerId); }
        catch (SQLException failure) { throw new ClassroomException(UNAVAILABLE, failure); }
    }

    private Optional<Classroom> findByMember(Connection connection, UUID playerId) throws SQLException {
        try (var statement = connection.prepareStatement("""
                SELECT c.* FROM classrooms c JOIN classroom_memberships m ON m.classroom_id=c.id
                WHERE m.player_uuid=? AND m.active=1
                """)) {
            statement.setString(1, playerId.toString());
            try (var rows = statement.executeQuery()) { return rows.next() ? Optional.of(read(rows)) : Optional.empty(); }
        }
    }

    @Override public Classroom create(UUID teacherId, String name, String description) {
        return transaction(connection -> {
            if (findByMember(connection, teacherId).isPresent()) throw new ClassroomException(ALREADY_MEMBER);
            var classroom = new Classroom(UUID.randomUUID(), teacherId, name, description, 0);
            try (var statement = connection.prepareStatement("INSERT INTO classrooms VALUES(?,?,?,?,0)")) {
                statement.setString(1, classroom.id().toString());
                statement.setString(2, teacherId.toString());
                statement.setString(3, name);
                statement.setString(4, description);
                statement.executeUpdate();
            }
            try (var statement = connection.prepareStatement("INSERT INTO classroom_memberships VALUES(?,?,?,'TEACHER',1)")) {
                statement.setString(1, UUID.randomUUID().toString());
                statement.setString(2, classroom.id().toString());
                statement.setString(3, teacherId.toString());
                statement.executeUpdate();
            }
            return classroom;
        });
    }

    @Override public Classroom update(UUID teacherId, UUID classroomId, long revision, String name, String description) {
        return transaction(connection -> {
            var current = findByMember(connection, teacherId).orElseThrow(() -> new ClassroomException(FORBIDDEN));
            if (!current.id().equals(classroomId) || !current.teacherId().equals(teacherId)) throw new ClassroomException(FORBIDDEN);
            if (current.revision() != revision) throw new ClassroomException(STALE);
            try (var statement = connection.prepareStatement("UPDATE classrooms SET name=?,description=?,revision=revision+1 WHERE id=? AND revision=?")) {
                statement.setString(1, name);
                statement.setString(2, description);
                statement.setString(3, classroomId.toString());
                statement.setLong(4, revision);
                if (statement.executeUpdate() != 1) throw new ClassroomException(STALE);
            }
            return new Classroom(classroomId, teacherId, name, description, revision + 1);
        });
    }

    private static Classroom read(ResultSet rows) throws SQLException {
        try {
            return new Classroom(UUID.fromString(rows.getString("id")), UUID.fromString(rows.getString("teacher_uuid")),
                    rows.getString("name"), rows.getString("description"), rows.getLong("revision"));
        } catch (IllegalArgumentException malformed) { throw new SQLException("Invalid persisted Classroom UUID", malformed); }
    }

    private <T> T transaction(SqlAction<T> action) {
        try (var connection = connect(); var transaction = connection.createStatement()) {
            transaction.execute("BEGIN IMMEDIATE");
            try {
                T value = action.run(connection);
                transaction.execute("COMMIT");
                return value;
            } catch (Exception failure) {
                try { transaction.execute("ROLLBACK"); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
                if (failure instanceof ClassroomException expected) throw expected;
                throw new ClassroomException(UNAVAILABLE, failure);
            }
        } catch (SQLException failure) { throw new ClassroomException(UNAVAILABLE, failure); }
    }

    @FunctionalInterface private interface SqlAction<T> { T run(Connection connection) throws SQLException; }
}
