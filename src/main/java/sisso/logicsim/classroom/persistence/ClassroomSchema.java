package sisso.logicsim.classroom.persistence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.UUID;

/** Transactional upgrades retain v1 identifiers; consistent pre-upgrade copies stay in plugin data. */
final class ClassroomSchema {
    static final int VERSION = 5;
    static final int APPLICATION = 0x4c47434c;
    private ClassroomSchema() { }
    static void initialize(Connection c, Path path) throws Exception {
        int version = pragma(c, "user_version"), app = pragma(c, "application_id");
        if (version > VERSION || version < 0 || (version > 0 && app != APPLICATION) || (app != 0 && app != APPLICATION)) {
            throw new SQLException("Unsupported Classroom schema or application ID");
        }
        if (version == 0) {
            try (var s = c.createStatement(); var r = s.executeQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'")) {
                if (r.next()) throw new SQLException("Unversioned nonempty Classroom database");
            }
        } else {
            verifyBase(c);
            if (version < VERSION) {
                Path backups = path.getParent().resolve("backups");
                Files.createDirectories(backups);
                try (var s = c.prepareStatement("VACUUM INTO ?")) {
                    s.setString(1, backups.resolve("classroom-v" + version + "-" + UUID.randomUUID() + ".db").toString());
                    s.execute();
                }
            }
        }
        // SQLite's documented table-rebuild procedure keeps dependent FK definitions unchanged.
        // This connection is private to initialization; the transaction and foreign_key_check still protect the upgrade.
        if (version < 4) execute(c, "PRAGMA foreign_keys=OFF");
        execute(c, "BEGIN IMMEDIATE");
        try {
            if (version == 0) {
                execute(c, """
                    CREATE TABLE classrooms(id TEXT PRIMARY KEY NOT NULL, teacher_uuid TEXT NOT NULL UNIQUE,
                      name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 64),
                      description TEXT NOT NULL CHECK(length(description)<=2048), revision INTEGER NOT NULL CHECK(revision>=0))
                    """);
                execute(c, """
                    CREATE TABLE classroom_memberships(id TEXT PRIMARY KEY NOT NULL, classroom_id TEXT NOT NULL REFERENCES classrooms(id),
                      player_uuid TEXT NOT NULL, role TEXT NOT NULL CHECK(role IN ('TEACHER','STUDENT')),
                      active INTEGER NOT NULL CHECK(active IN (0,1)))
                    """);
            }
            if (version < 2) upgradeTwo(c);
            if (version < 3) execute(c, "ALTER TABLE classrooms ADD COLUMN message TEXT NOT NULL DEFAULT '' CHECK(length(message)<=2048)");
            if (version < 4) upgradeFour(c);
            if (version < 5) LearningSchema.create(c);
            execute(c, "PRAGMA application_id=" + APPLICATION);
            execute(c, "PRAGMA user_version=" + VERSION);
            verifyBase(c);
            LearningSchema.verify(c);
            execute(c, "SELECT message,search_name,closed_at FROM classrooms LIMIT 0");
            execute(c, "SELECT enrollment,joined_at,ended_at,revision FROM classroom_memberships LIMIT 0");
            execute(c, "SELECT id,classroom_id,student_uuid,origin,state,enrollment,revision,created_at,membership_id FROM classroom_intakes LIMIT 0");
            execute(c, "SELECT uuid,name,search_name FROM classroom_players LIMIT 0");
            verifyIndex(c, "one_open_classroom_per_teacher", "teacher_uuid", "closed_at IS NULL");
            verifyIndex(c, "unique_active_enrollment", "classroom_id,enrollment", "active=1 AND role='STUDENT'");
            verifyIndex(c, "one_open_intake", "classroom_id,student_uuid", "state IN ('INVITED','REQUESTED')");
            try (var s = c.createStatement(); var r = s.executeQuery("PRAGMA foreign_key_check")) {
                if (r.next()) throw new SQLException("Broken Classroom foreign key");
            }
            execute(c, "COMMIT");
        } catch (Exception failure) {
            try { execute(c, "ROLLBACK"); } catch (SQLException rollback) { failure.addSuppressed(rollback); }
            throw failure;
        } finally {
            if (version < 4) execute(c, "PRAGMA foreign_keys=ON");
        }
    }
    private static void upgradeFour(Connection c) throws SQLException {
        execute(c, """
            CREATE TABLE classrooms_v4(id TEXT PRIMARY KEY NOT NULL, teacher_uuid TEXT NOT NULL,
              name TEXT NOT NULL CHECK(length(name) BETWEEN 1 AND 64),
              description TEXT NOT NULL CHECK(length(description)<=2048), revision INTEGER NOT NULL CHECK(revision>=0),
              search_name TEXT NOT NULL DEFAULT '', message TEXT NOT NULL DEFAULT '' CHECK(length(message)<=2048),
              closed_at INTEGER CHECK(closed_at IS NULL OR closed_at>=0))
            """);
        execute(c, "INSERT INTO classrooms_v4(id,teacher_uuid,name,description,revision,search_name,message) SELECT id,teacher_uuid,name,description,revision,search_name,message FROM classrooms");
        execute(c, "DROP TABLE classrooms");
        execute(c, "ALTER TABLE classrooms_v4 RENAME TO classrooms");
        execute(c, "CREATE UNIQUE INDEX one_open_classroom_per_teacher ON classrooms(teacher_uuid) WHERE closed_at IS NULL");
    }
    private static void upgradeTwo(Connection c) throws SQLException {
        execute(c, "ALTER TABLE classrooms ADD COLUMN search_name TEXT NOT NULL DEFAULT ''");
        try (var s = c.createStatement(); var r = s.executeQuery("SELECT id,name FROM classrooms");
             var update = c.prepareStatement("UPDATE classrooms SET search_name=? WHERE id=?")) {
            while (r.next()) { update.setString(1, r.getString(2).toLowerCase(Locale.ROOT)); update.setString(2, r.getString(1)); update.executeUpdate(); }
        }
        execute(c, """
            CREATE TABLE classroom_memberships_v2(
              id TEXT PRIMARY KEY NOT NULL, classroom_id TEXT NOT NULL REFERENCES classrooms(id), player_uuid TEXT NOT NULL,
              role TEXT NOT NULL CHECK(role IN ('TEACHER','STUDENT')), active INTEGER NOT NULL CHECK(active IN(0,1)),
              enrollment TEXT COLLATE BINARY,
              joined_at INTEGER NOT NULL, ended_at INTEGER, revision INTEGER NOT NULL DEFAULT 0 CHECK(revision>=0),
              CHECK((role='TEACHER' AND enrollment IS NULL) OR
                (role='STUDENT' AND enrollment IS NOT NULL AND length(enrollment) BETWEEN 1 AND 64)),
              CHECK((active=1 AND ended_at IS NULL) OR (active=0 AND ended_at IS NOT NULL)))
            """);
        // Production v1 created active teachers only. Unexpected rows lack trustworthy enrollment/history: refuse safely.
        execute(c, "INSERT INTO classroom_memberships_v2 SELECT id,classroom_id,player_uuid,role,active,NULL,0,NULL,0 FROM classroom_memberships");
        execute(c, "DROP TABLE classroom_memberships");
        execute(c, "ALTER TABLE classroom_memberships_v2 RENAME TO classroom_memberships");
        execute(c, "CREATE UNIQUE INDEX one_active_classroom_per_player ON classroom_memberships(player_uuid) WHERE active=1");
        execute(c, "CREATE UNIQUE INDEX one_active_teacher_per_classroom ON classroom_memberships(classroom_id) WHERE active=1 AND role='TEACHER'");
        execute(c, "CREATE UNIQUE INDEX unique_active_enrollment ON classroom_memberships(classroom_id,enrollment) WHERE active=1 AND role='STUDENT'");
        execute(c, "CREATE TABLE classroom_players(uuid TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,search_name TEXT NOT NULL)");
        execute(c, """
            CREATE TABLE classroom_intakes(
              id TEXT PRIMARY KEY NOT NULL, classroom_id TEXT NOT NULL REFERENCES classrooms(id), student_uuid TEXT NOT NULL,
              origin TEXT NOT NULL CHECK(origin IN('REQUEST','INVITE')),
              state TEXT NOT NULL CHECK(state IN('INVITED','REQUESTED','APPROVED','REJECTED','DECLINED','CANCELLED','SUPERSEDED')),
              enrollment TEXT, revision INTEGER NOT NULL DEFAULT 0 CHECK(revision>=0), created_at INTEGER NOT NULL,
              membership_id TEXT REFERENCES classroom_memberships(id),
              CHECK(state NOT IN('REQUESTED','APPROVED') OR (enrollment IS NOT NULL AND length(enrollment) BETWEEN 1 AND 64)),
              CHECK((state='APPROVED' AND membership_id IS NOT NULL) OR (state<>'APPROVED' AND membership_id IS NULL)))
            """);
        execute(c, "CREATE UNIQUE INDEX one_open_intake ON classroom_intakes(classroom_id,student_uuid) WHERE state IN ('INVITED','REQUESTED')");
        execute(c, "CREATE INDEX intake_student ON classroom_intakes(student_uuid,created_at)");
        execute(c, "CREATE INDEX membership_classroom ON classroom_memberships(classroom_id,active)");
    }
    private static void verifyBase(Connection c) throws SQLException {
        execute(c, "SELECT id,teacher_uuid,name,description,revision FROM classrooms LIMIT 0");
        execute(c, "SELECT id,classroom_id,player_uuid,role,active FROM classroom_memberships LIMIT 0");
        verifyIndex(c, "one_active_classroom_per_player", "player_uuid", "active=1");
        verifyIndex(c, "one_active_teacher_per_classroom", "classroom_id", "active=1 AND role='TEACHER'");
    }
    private static void verifyIndex(Connection c, String name, String columns, String filter) throws SQLException {
        try (var s = c.prepareStatement("SELECT sql FROM sqlite_master WHERE type='index' AND name=?")) {
            s.setString(1, name);
            try (var r = s.executeQuery()) {
                if (!r.next()) throw new SQLException("Missing Classroom constraint " + name);
                String sql = r.getString(1).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
                String suffix = ("(" + columns + ")WHERE" + filter).replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
                if (!sql.startsWith("createuniqueindex") || !sql.endsWith(suffix)) throw new SQLException("Invalid Classroom constraint " + name);
            }
        }
    }
    private static int pragma(Connection c, String key) throws SQLException {
        try (var s = c.createStatement(); var r = s.executeQuery("PRAGMA " + key)) { return r.next() ? r.getInt(1) : -1; }
    }
    private static void execute(Connection c, String sql) throws SQLException { try (var s = c.createStatement()) { s.execute(sql); } }
}
