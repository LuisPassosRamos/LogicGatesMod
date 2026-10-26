package sisso.logicsim.classroom.persistence;

import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

/** Transactions protect invariants across independent connections/worlds, not only our I/O queue. */
public final class SqliteClassroomRepository implements ClassroomRepository {
    public static final int SCHEMA_VERSION = ClassroomSchema.VERSION;
    public static final int PAGE_SIZE = 20;
    private final String url;
    private static final String MEMBER = "SELECT m.*,coalesce(p.name,m.player_uuid) AS player_name FROM classroom_memberships m LEFT JOIN classroom_players p ON p.uuid=m.player_uuid ";
    private static final String INTAKE = "SELECT i.*,c.name AS classroom_name,coalesce(p.name,i.student_uuid) AS student_name FROM classroom_intakes i JOIN classrooms c ON c.id=i.classroom_id LEFT JOIN classroom_players p ON p.uuid=i.student_uuid ";
    public SqliteClassroomRepository(Path path) {
        path = path.toAbsolutePath().normalize();
        url = "jdbc:sqlite:" + path;
        try {
            Files.createDirectories(path.getParent());
            Class.forName("org.sqlite.JDBC");
            try (var c = connect()) { ClassroomSchema.initialize(c, path); }
        } catch (Exception failure) { throw new ClassroomException(UNAVAILABLE, failure); }
    }
    private Connection connect() throws SQLException {
        var c = DriverManager.getConnection(url);
        try (var s = c.createStatement()) { s.execute("PRAGMA foreign_keys=ON"); s.execute("PRAGMA busy_timeout=5000"); }
        catch (SQLException failure) { c.close(); throw failure; }
        return c;
    }
    @Override public Home readHome(UUID actor, boolean canTeach) {
        return read(c -> home(c,actor,canTeach));
    }
    @Override public Screen readScreen(UUID actor, Kind requested, String query, int number, boolean canTeach) {
        return transaction("BEGIN DEFERRED",c -> {
            var home=home(c,actor,canTeach);
            Kind kind=ClassroomReadPolicy.kind(home,requested);
            boolean teacher=ClassroomReadPolicy.teacher(home);
            int page=kind==Kind.SETTINGS || kind==Kind.DETAIL ? 0 : ClassroomReadPolicy.page(number);
            UUID scope=teacher ? home.classroom().id() : actor;
            Page<?> rows=switch(kind) {
                case BROWSE -> browse(c,ClassroomReadPolicy.query(query),page);
                case MEMBERS -> members(c,scope,page);
                case INTAKES -> intakes(c,scope,teacher,page);
                case PLAYERS -> players(c,actor,ClassroomReadPolicy.query(query),page);
                case SETTINGS, DETAIL -> new Page<>(List.of(),0,false);
            };
            var counts=home.classroom()==null ? new Counts(0,0) : counts(c,home.classroom().id(),teacher);
            return new Screen(home,kind,rows,counts.students(),counts.activities());
        });
    }
    private record Counts(int students,int activities) { }
    private Counts counts(Connection c,UUID classroom,boolean teacher) throws SQLException {
        // Separate aggregates avoid multiplying activities by memberships; share the screen's read snapshot.
        return one(c,"""
            SELECT count(*) AS students,
              (SELECT count(*) FROM learning_activities a WHERE a.classroom_id=? AND a.archived=0
                AND (?=1 OR a.current_version IS NOT NULL)) AS activities
            FROM classroom_memberships WHERE classroom_id=? AND active=1 AND role='STUDENT' AND ?=1
            """,r->new Counts(r.getInt("students"),r.getInt("activities")),classroom,teacher?1:0,classroom,teacher?1:0).orElseThrow();
    }
    private Home home(Connection c,UUID actor,boolean canTeach) throws SQLException {
        var home=one(c,"""
            SELECT m.*,coalesce(p.name,m.player_uuid) AS player_name,
              c.id AS context_id,c.teacher_uuid AS context_teacher,c.name AS context_name,
              c.description AS context_description,c.revision AS context_revision,c.message AS context_message,
              c.closed_at AS context_closed_at,coalesce(t.name,c.teacher_uuid) AS teacher_name
            FROM classroom_memberships m LEFT JOIN classrooms c ON c.id=m.classroom_id
            LEFT JOIN classroom_players p ON p.uuid=m.player_uuid
            LEFT JOIN classroom_players t ON t.uuid=c.teacher_uuid
            WHERE m.player_uuid=? AND m.active=1
            """,r -> {
                if (r.getString("context_id")==null || r.getObject("context_closed_at")!=null) throw new ClassroomException(UNAVAILABLE);
                var classroom=new Classroom(uuid(r,"context_id"),uuid(r,"context_teacher"),r.getString("context_name"),
                        r.getString("context_description"),r.getLong("context_revision"),r.getString("context_message"));
                return new Home(classroom,membership(r),r.getString("teacher_name"),canTeach);
            },actor).orElseGet(()->new Home(null,null,"",canTeach));
        return ClassroomReadPolicy.authorize(home);
    }
    @Override public Optional<Classroom> findByMember(UUID player) {
        return read(c -> one(c, "SELECT c.* FROM classrooms c JOIN classroom_memberships m ON m.classroom_id=c.id WHERE m.player_uuid=? AND m.active=1 AND c.closed_at IS NULL", SqliteClassroomRepository::classroom, player));
    }
    @Override public Optional<Member> member(UUID player) { return read(c -> active(c, player)); }
    private Optional<Member> active(Connection c, UUID player) throws SQLException {
        return one(c, MEMBER + "WHERE m.player_uuid=? AND m.active=1", SqliteClassroomRepository::membership, player);
    }
    private Classroom owned(Connection c, UUID actor) throws SQLException {
        return one(c, "SELECT c.* FROM classrooms c JOIN classroom_memberships m ON m.classroom_id=c.id WHERE c.teacher_uuid=? AND m.player_uuid=? AND m.role='TEACHER' AND m.active=1 AND c.closed_at IS NULL",
                SqliteClassroomRepository::classroom, actor, actor).orElseThrow(() -> new ClassroomException(FORBIDDEN));
    }
    @Override public Classroom create(UUID teacher, String name, String description) {
        return transaction(c -> {
            eligible(c, teacher);
            UUID id = UUID.randomUUID();
            execute(c, "INSERT INTO classrooms(id,teacher_uuid,name,description,revision,search_name) VALUES(?,?,?,?,0,?)", id, teacher, name, description, lower(name));
            execute(c, "INSERT INTO classroom_memberships(id,classroom_id,player_uuid,role,active,joined_at) VALUES(?,?,?,'TEACHER',1,?)", UUID.randomUUID(), id, teacher, now());
            supersede(c, teacher);
            return new Classroom(id, teacher, name, description, 0);
        });
    }
    @Override public Classroom update(UUID teacher, UUID id, long revision, String name, String description) {
        return transaction(c -> {
            var old = owned(c, teacher); sameClass(old, id); revision(old.revision(), revision);
            execute(c, "UPDATE classrooms SET name=?,description=?,search_name=?,revision=revision+1 WHERE id=?", name, description, lower(name), id);
            return new Classroom(id, teacher, name, description, revision + 1, old.message());
        });
    }
    @Override public Classroom message(UUID teacher, UUID id, long revision, String message) {
        return transaction(c -> {
            var old = owned(c, teacher); sameClass(old, id); revision(old.revision(), revision);
            execute(c, "UPDATE classrooms SET message=?,revision=revision+1 WHERE id=?", message, id);
            return new Classroom(id, teacher, old.name(), old.description(), revision + 1, message);
        });
    }
    @Override public Classroom settings(UUID teacher, UUID id, long revision, String name, String description, String message) {
        return transaction(c -> {
            var old = owned(c, teacher); sameClass(old, id); revision(old.revision(), revision);
            execute(c, "UPDATE classrooms SET name=?,description=?,search_name=?,message=?,revision=revision+1 WHERE id=?", name, description, lower(name), message, id);
            return new Classroom(id, teacher, name, description, revision + 1, message);
        });
    }
    @Override public Classroom disband(UUID teacher, UUID id, long revision) {
        return transaction(c -> {
            var old = owned(c, teacher); sameClass(old, id); revision(old.revision(), revision);
            long ended = now();
            execute(c, "UPDATE classrooms SET closed_at=?,revision=revision+1 WHERE id=?", ended, id);
            execute(c, "UPDATE classroom_memberships SET active=0,ended_at=?,revision=revision+1 WHERE classroom_id=? AND active=1", ended, id);
            execute(c, "UPDATE classroom_intakes SET state='CANCELLED',revision=revision+1 WHERE classroom_id=? AND state IN('INVITED','REQUESTED')", id);
            return new Classroom(id, teacher, old.name(), old.description(), revision + 1, old.message());
        });
    }
    @Override public void remember(UUID id, String name) {
        transaction(c -> { execute(c, "INSERT INTO classroom_players(uuid,name,search_name) VALUES(?,?,?) ON CONFLICT(uuid) DO UPDATE SET name=excluded.name,search_name=excluded.search_name", id, name, lower(name)); return null; });
    }
    @Override public String playerName(UUID id) {
        return read(c -> one(c, "SELECT name FROM classroom_players WHERE uuid=?", r -> r.getString(1), id).orElse(id.toString()));
    }
    @Override public Page<Summary> browse(String query, int page) { return read(c -> browse(c,query,page)); }
    private Page<Summary> browse(Connection c,String query,int page) throws SQLException {
        return page(c,"""
            SELECT c.id,c.name,c.description,c.teacher_uuid,coalesce(p.name,c.teacher_uuid) AS teacher_name
            FROM classrooms c LEFT JOIN classroom_players p ON p.uuid=c.teacher_uuid
            WHERE c.closed_at IS NULL AND (instr(c.search_name,?)>0 OR instr(coalesce(p.search_name,''),?)>0)
            ORDER BY c.search_name,c.id LIMIT 21 OFFSET ?
            """, r -> new Summary(uuid(r,"id"),r.getString("name"),r.getString("description"),uuid(r,"teacher_uuid"),r.getString("teacher_name")), page, lower(query), lower(query), page * PAGE_SIZE);
    }
    @Override public Page<Profile> players(UUID teacher, String query, int page) {
        return read(c -> { owned(c,teacher); return players(c,teacher,query,page); });
    }
    private Page<Profile> players(Connection c,UUID teacher,String query,int page) throws SQLException {
        return page(c,"""
            SELECT p.* FROM classroom_players p WHERE instr(p.search_name,?)>0 AND p.uuid<>?
            AND NOT EXISTS(SELECT 1 FROM classroom_memberships m WHERE m.player_uuid=p.uuid AND m.active=1)
            ORDER BY p.search_name,p.uuid LIMIT 21 OFFSET ?
            """, r -> new Profile(uuid(r,"uuid"),r.getString("name")), page, lower(query), teacher, page * PAGE_SIZE);
    }
    @Override public Page<Member> members(UUID teacher, int page) { return read(c -> members(c,owned(c,teacher).id(),page)); }
    private Page<Member> members(Connection c,UUID classroom,int page) throws SQLException {
        return page(c,MEMBER + "WHERE m.classroom_id=? AND m.role='STUDENT' AND m.active=1 ORDER BY coalesce(p.search_name,m.player_uuid),m.id LIMIT 21 OFFSET ?",
                SqliteClassroomRepository::membership,page,classroom,page * PAGE_SIZE);
    }
    @Override public int studentCount(UUID teacher) { return read(c -> studentCount(c,owned(c,teacher).id())); }
    private int studentCount(Connection c,UUID classroom) throws SQLException {
        return one(c,"SELECT count(*) FROM classroom_memberships WHERE classroom_id=? AND role='STUDENT' AND active=1",r -> r.getInt(1),classroom).orElse(0);
    }
    @Override public Page<Intake> intakes(UUID actor, boolean teacher, int page) {
        return read(c -> intakes(c,teacher ? owned(c,actor).id() : actor,teacher,page));
    }
    private Page<Intake> intakes(Connection c,UUID id,boolean teacher,int page) throws SQLException {
        String scope=teacher ? "i.classroom_id=?" : "i.student_uuid=?";
        String states=teacher ? " AND i.state IN('INVITED','REQUESTED') ORDER BY i.created_at,i.id" : " ORDER BY i.created_at DESC,i.id";
        return page(c,INTAKE + "WHERE " + scope + states + " LIMIT 21 OFFSET ?",SqliteClassroomRepository::intake,page,id,page * PAGE_SIZE);
    }
    @Override public Intake request(UUID student, UUID classroom, String enrollment) {
        return transaction(c -> {
            eligible(c, student);
            one(c, "SELECT * FROM classrooms WHERE id=? AND closed_at IS NULL", SqliteClassroomRepository::classroom, classroom).orElseThrow(() -> new ClassroomException(NOT_FOUND));
            var pending = open(c, classroom, student);
            if (pending.isPresent()) {
                var i = pending.get();
                if (i.state() == State.INVITED) return accept(c, student, i.id(), i.revision(), enrollment);
                return i; // A repeated request must not overwrite a draft under review.
            }
            UUID id = UUID.randomUUID();
            execute(c, "INSERT INTO classroom_intakes(id,classroom_id,student_uuid,origin,state,enrollment,created_at) VALUES(?,?,?,'REQUEST','REQUESTED',?,?)", id, classroom, student, enrollment, now());
            return intake(c, id);
        });
    }
    @Override public Intake invite(UUID teacher, UUID target) {
        return transaction(c -> {
            var classroom = owned(c, teacher); eligible(c, target);
            one(c, "SELECT uuid FROM classroom_players WHERE uuid=?", r -> r.getString(1), target).orElseThrow(() -> new ClassroomException(NOT_FOUND));
            var pending = open(c, classroom.id(), target);
            if (pending.isPresent()) return pending.get();
            UUID id = UUID.randomUUID();
            execute(c, "INSERT INTO classroom_intakes(id,classroom_id,student_uuid,origin,state,created_at) VALUES(?,?,?,'INVITE','INVITED',?)", id, classroom.id(), target, now());
            return intake(c, id);
        });
    }
    @Override public Intake accept(UUID student, UUID id, long revision, String enrollment) {
        return transaction(c -> accept(c, student, id, revision, enrollment));
    }
    private Intake accept(Connection c, UUID student, UUID id, long expected, String enrollment) throws SQLException {
        var i = intake(c, id); student(i, student); eligible(c, student);
        if (i.state() == State.REQUESTED) return i;
        revision(i.revision(), expected); state(i, State.INVITED);
        execute(c, "UPDATE classroom_intakes SET state='REQUESTED',enrollment=?,revision=revision+1 WHERE id=?", enrollment, id);
        return intake(c, id);
    }
    @Override public Intake closeIntake(UUID actor, UUID id, long expected, boolean teacher) {
        return transaction(c -> {
            var i = intake(c, id);
            if (teacher) sameClass(owned(c, actor), i.classroomId()); else student(i, actor);
            if (!open(i)) return i;
            revision(i.revision(), expected);
            String next = teacher ? (i.state() == State.INVITED ? "CANCELLED" : "REJECTED") : (i.state() == State.INVITED ? "DECLINED" : "CANCELLED");
            execute(c, "UPDATE classroom_intakes SET state=?,revision=revision+1 WHERE id=?", next, id);
            return intake(c, id);
        });
    }
    @Override public Member approve(UUID teacher, UUID id, long expected, String enrollment) {
        return transaction(c -> {
            var i = intake(c, id); sameClass(owned(c, teacher), i.classroomId());
            if (i.state() == State.APPROVED) {
                return one(c, MEMBER + "WHERE m.id=(SELECT membership_id FROM classroom_intakes WHERE id=?)", SqliteClassroomRepository::membership, id).orElseThrow(() -> new ClassroomException(UNAVAILABLE));
            }
            revision(i.revision(), expected); state(i, State.REQUESTED); eligible(c, i.studentId());
            uniqueEnrollment(c, i.classroomId(), enrollment, null);
            UUID membership = UUID.randomUUID();
            execute(c, "INSERT INTO classroom_memberships(id,classroom_id,player_uuid,role,active,enrollment,joined_at) VALUES(?,?,?,'STUDENT',1,?,?)", membership, i.classroomId(), i.studentId(), enrollment, now());
            execute(c, "UPDATE classroom_intakes SET state='APPROVED',enrollment=?,membership_id=?,revision=revision+1 WHERE id=?", enrollment, membership, id);
            supersede(c, i.studentId());
            return membership(c, membership);
        });
    }
    @Override public Member correct(UUID teacher, UUID id, long expected, String enrollment) {
        return transaction(c -> {
            var m = membership(c, id); sameClass(owned(c, teacher), m.classroomId());
            if (m.role() != Role.STUDENT || !m.active()) throw new ClassroomException(CLOSED);
            revision(m.revision(), expected); uniqueEnrollment(c, m.classroomId(), enrollment, id);
            execute(c, "UPDATE classroom_memberships SET enrollment=?,revision=revision+1 WHERE id=?", enrollment, id);
            return membership(c, id);
        });
    }
    @Override public Member end(UUID actor, UUID id, long expected, boolean teacher) {
        return transaction(c -> {
            var m = membership(c, id);
            if (teacher) sameClass(owned(c, actor), m.classroomId());
            else if (!m.playerId().equals(actor)) throw new ClassroomException(FORBIDDEN);
            if (m.role() != Role.STUDENT) throw new ClassroomException(FORBIDDEN);
            if (!m.active()) return m;
            revision(m.revision(), expected);
            execute(c, "UPDATE classroom_memberships SET active=0,ended_at=?,revision=revision+1 WHERE id=?", now(), id);
            return membership(c, id);
        });
    }
    private void uniqueEnrollment(Connection c, UUID classroom, String enrollment, UUID exclude) throws SQLException {
        if (one(c, "SELECT id FROM classroom_memberships WHERE classroom_id=? AND enrollment=? COLLATE BINARY AND active=1 AND role='STUDENT' AND id<>?", r -> r.getString(1), classroom, enrollment, exclude == null ? "" : exclude).isPresent()) throw new ClassroomException(DUPLICATE_ENROLLMENT);
    }
    private void eligible(Connection c, UUID student) throws SQLException { if (active(c, student).isPresent()) throw new ClassroomException(ALREADY_MEMBER); }
    private void supersede(Connection c, UUID student) throws SQLException { execute(c, "UPDATE classroom_intakes SET state='SUPERSEDED',revision=revision+1 WHERE student_uuid=? AND state IN('INVITED','REQUESTED')", student); }
    private Optional<Intake> open(Connection c, UUID classroom, UUID student) throws SQLException {
        return one(c, INTAKE + "WHERE i.classroom_id=? AND i.student_uuid=? AND i.state IN('INVITED','REQUESTED')", SqliteClassroomRepository::intake, classroom, student);
    }
    private Intake intake(Connection c, UUID id) throws SQLException { return one(c, INTAKE + "WHERE i.id=?", SqliteClassroomRepository::intake, id).orElseThrow(() -> new ClassroomException(NOT_FOUND)); }
    private Member membership(Connection c, UUID id) throws SQLException { return one(c, MEMBER + "WHERE m.id=?", SqliteClassroomRepository::membership, id).orElseThrow(() -> new ClassroomException(NOT_FOUND)); }
    private static boolean open(Intake i) { return i.state() == State.INVITED || i.state() == State.REQUESTED; }
    private static void student(Intake i, UUID actor) { if (!i.studentId().equals(actor)) throw new ClassroomException(FORBIDDEN); }
    private static void state(Intake i, State expected) { if (i.state() != expected) throw new ClassroomException(CLOSED); }
    private static void revision(long actual, long expected) { if (actual != expected) throw new ClassroomException(STALE); }
    private static void sameClass(Classroom c, UUID id) { if (!c.id().equals(id)) throw new ClassroomException(FORBIDDEN); }
    private static String lower(String value) { return value.toLowerCase(Locale.ROOT); }
    private static long now() { return System.currentTimeMillis(); }
    private static UUID uuid(ResultSet r, String field) throws SQLException {
        try { return UUID.fromString(r.getString(field)); } catch (RuntimeException e) { throw new SQLException("Invalid Classroom UUID", e); }
    }
    private static Classroom classroom(ResultSet r) throws SQLException {
        return new Classroom(uuid(r,"id"),uuid(r,"teacher_uuid"),r.getString("name"),r.getString("description"),r.getLong("revision"),r.getString("message"));
    }
    private static Member membership(ResultSet r) throws SQLException {
        long end = r.getLong("ended_at"); Long endedAt = r.wasNull() ? null : end;
        return new Member(uuid(r,"id"),uuid(r,"classroom_id"),uuid(r,"player_uuid"),r.getString("player_name"),Role.valueOf(r.getString("role")),r.getString("enrollment"),r.getInt("active")==1,r.getLong("joined_at"),endedAt,r.getLong("revision"));
    }
    private static Intake intake(ResultSet r) throws SQLException {
        return new Intake(uuid(r,"id"),uuid(r,"classroom_id"),r.getString("classroom_name"),uuid(r,"student_uuid"),r.getString("student_name"),Origin.valueOf(r.getString("origin")),State.valueOf(r.getString("state")),r.getString("enrollment"),r.getLong("revision"),r.getLong("created_at"));
    }
    private static void bind(PreparedStatement s, Object... args) throws SQLException { for (int n=0;n<args.length;n++) s.setObject(n+1,args[n] instanceof UUID ? args[n].toString() : args[n]); }
    private static void execute(Connection c, String sql, Object... args) throws SQLException {
        try (var s = c.prepareStatement(sql)) { bind(s,args); s.executeUpdate(); }
    }
    private static <T> List<T> query(Connection c, String sql, Row<T> map, Object... args) throws SQLException {
        try (var s=c.prepareStatement(sql)) { bind(s,args); try(var r=s.executeQuery()) { var rows=new ArrayList<T>(); while(r.next()) rows.add(map.read(r)); return rows; } }
    }
    private static <T> Optional<T> one(Connection c, String sql, Row<T> map, Object... args) throws SQLException { return query(c,sql,map,args).stream().findFirst(); }
    private static <T> Page<T> page(Connection c, String sql, Row<T> map, int page, Object... args) throws SQLException {
        var rows=query(c,sql,map,args); return new Page<>(rows.subList(0,Math.min(rows.size(),PAGE_SIZE)),page,rows.size()>PAGE_SIZE);
    }
    private <T> T read(Action<T> action) {
        try(var c=connect()) { return action.run(c); }
        catch(ClassroomException expected) { throw expected; }
        catch(Exception failure) { throw new ClassroomException(UNAVAILABLE,failure); }
    }
    private <T> T transaction(Action<T> action) { return transaction("BEGIN IMMEDIATE",action); }
    private <T> T transaction(String begin,Action<T> action) {
        return read(c -> {
            try(var s=c.createStatement()) {
                s.execute(begin);
                try { T result=action.run(c); s.execute("COMMIT"); return result; }
                catch(Exception failure) { try { s.execute("ROLLBACK"); } catch(SQLException rollback) { failure.addSuppressed(rollback); } throw failure; }
            }
        });
    }
    @FunctionalInterface private interface Row<T> { T read(ResultSet r) throws SQLException; }
    @FunctionalInterface private interface Action<T> { T run(Connection c) throws Exception; }
}
