package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

class ClassroomRepositoryTest {
    @TempDir Path directory;
    private final UUID teacher = UUID.randomUUID();
    private Path database() { return directory.resolve("classroom.db"); }
    private SqliteClassroomRepository repository() { return new SqliteClassroomRepository(database()); }
    private ClassroomService service() { return new ClassroomService(repository(), actor -> true); }

    @Test void createAndEditSurviveReopeningWithLiteralTextAndTechnicalUuids() {
        var service = service();
        var original = service.create(teacher, "  Turma 'A'  ", " %server.fake\r\n<b>literal</b> ");
        assertEquals("Turma 'A'", original.name());
        assertEquals("%server.fake\n<b>literal</b>", original.description());
        assertEquals(teacher, original.teacherId());
        assertEquals(0, original.revision());
        var saved = service.update(teacher, original.id(), 0, "Nova turma", "Descrição\ncom acento");
        assertEquals(1, saved.revision());
        assertEquals(saved, new ClassroomService(repository(), actor -> true).own(teacher).orElseThrow());
        assertTrue(repository().findByMember(UUID.randomUUID()).isEmpty());
        assertEquals(1, scalar("PRAGMA user_version"));
        assertEquals(1, scalar("SELECT count(*) FROM classroom_memberships WHERE role='TEACHER' AND active=1"));
    }

    @Test void permissionRevocationAndForeignOwnershipRejectReadsAndWrites() {
        var allowed = new AtomicBoolean(false);
        var repository = repository();
        var service = new ClassroomService(repository, actor -> allowed.get());
        expect(FORBIDDEN, () -> service.create(teacher, "A", ""));
        allowed.set(true);
        var classroom = service.create(teacher, "A", "");
        expect(FORBIDDEN, () -> service.update(UUID.randomUUID(), classroom.id(), 0, "B", ""));
        expect(FORBIDDEN, () -> service.update(teacher, UUID.randomUUID(), 0, "B", ""));
        allowed.set(false);
        expect(FORBIDDEN, () -> service.own(teacher));
        expect(FORBIDDEN, () -> service.update(teacher, classroom.id(), 0, "B", ""));
        assertEquals(classroom, repository.findByMember(teacher).orElseThrow());
    }

    @Test void serverValidatesNameAndDescriptionBeforeWriting() {
        var service = service();
        for (String name : new String[]{null, "", " \t ", "a".repeat(65), "A\nB", "A\u0000B"}) {
            expect(INVALID_NAME, () -> service.create(teacher, name, ""));
        }
        for (String description : new String[]{null, "a".repeat(2049), "A\u0000B"}) {
            expect(INVALID_DESCRIPTION, () -> service.create(teacher, "A", description));
        }
        assertEquals(0, scalar("SELECT count(*) FROM classrooms"));
        var classroom = service.create(teacher, "a".repeat(64), "a".repeat(2048));
        expect(INVALID_NAME, () -> service.update(teacher, classroom.id(), 0, " ", ""));
        assertEquals(classroom, service.own(teacher).orElseThrow());
    }

    @Test void existingStudentLinkAlsoPreventsCreatingATeacherClassroom() throws Exception {
        var service = service();
        var classroom = service.create(UUID.randomUUID(), "Existing class", "");
        sql("INSERT INTO classroom_memberships VALUES('" + UUID.randomUUID() + "','" + classroom.id() + "','" + teacher + "','STUDENT',1)");
        expect(ALREADY_MEMBER, () -> service.create(teacher, "Second link", ""));
        expect(FORBIDDEN, () -> service.own(teacher));
        assertEquals(1, scalar("SELECT count(*) FROM classrooms"));
    }

    @Test void concurrentCreatesAcrossIndependentRepositoriesProduceOneLink() throws Exception {
        var first = service();
        var second = service();
        var results = race(() -> first.create(teacher, "A", ""), () -> second.create(teacher, "B", ""));
        assertEquals(1, results.stream().filter(Classroom.class::isInstance).count());
        assertEquals(1, results.stream().filter(ALREADY_MEMBER::equals).count());
        assertEquals(1, scalar("SELECT count(*) FROM classrooms"));
        assertEquals(1, scalar("SELECT count(*) FROM classroom_memberships WHERE active=1"));
    }

    @Test void concurrentEditsRejectTheStaleRevisionInsteadOfLosingAnUpdate() throws Exception {
        var first = service();
        var second = service();
        var original = first.create(teacher, "A", "");
        var results = race(() -> first.update(teacher, original.id(), 0, "B", ""),
                () -> second.update(teacher, original.id(), 0, "C", ""));
        assertEquals(1, results.stream().filter(Classroom.class::isInstance).count());
        assertEquals(1, results.stream().filter(STALE::equals).count());
        assertEquals(1, first.own(teacher).orElseThrow().revision());
    }

    @Test void membershipFailureRollsBackClassroomAndCanBeRetried() throws Exception {
        var service = service();
        sql("CREATE TRIGGER fail_link BEFORE INSERT ON classroom_memberships BEGIN SELECT RAISE(FAIL,'injected failure'); END");
        expect(UNAVAILABLE, () -> service.create(teacher, "A", ""));
        assertEquals(0, scalar("SELECT count(*) FROM classrooms"));
        assertEquals(0, scalar("SELECT count(*) FROM classroom_memberships"));
        sql("DROP TRIGGER fail_link");
        assertEquals("A", service.create(teacher, "A", "").name());
    }

    @Test void incompatibleSchemaIsPreservedAndNotReplaced() throws Exception {
        repository();
        sql("PRAGMA user_version=2");
        byte[] before = Files.readAllBytes(database());
        expect(UNAVAILABLE, this::repository);
        assertArrayEquals(before, Files.readAllBytes(database()));
    }

    @Test void unversionedNonemptyAndCorruptDatabasesArePreserved() throws Exception {
        Class.forName("org.sqlite.JDBC");
        sql("CREATE TABLE unrelated(data TEXT)");
        byte[] before = Files.readAllBytes(database());
        expect(UNAVAILABLE, this::repository);
        assertArrayEquals(before, Files.readAllBytes(database()));
        Files.writeString(database(), "This is not a SQLite database");
        before = Files.readAllBytes(database());
        expect(UNAVAILABLE, this::repository);
        assertArrayEquals(before, Files.readAllBytes(database()));
    }

    @Test void incompleteVersionedSchemaIsRejected() throws Exception {
        repository();
        sql("DROP INDEX one_active_classroom_per_player");
        expect(UNAVAILABLE, this::repository);
    }

    @Test void runtimeSerializesInitializationAndReportsFailureWithoutThrowingAtStartup() throws Exception {
        try (var runtime = new ClassroomRuntime(directory, actor -> true, failure -> fail(failure))) {
            var classroom = runtime.create(teacher, "Async", "").get(5, TimeUnit.SECONDS);
            assertEquals(classroom, runtime.own(teacher).get(5, TimeUnit.SECONDS).orElseThrow());
        }
        Path blocked = directory.resolve("blocked");
        Files.writeString(blocked, "preserve");
        var failures = new java.util.concurrent.CopyOnWriteArrayList<Throwable>();
        var runtime = new ClassroomRuntime(blocked, actor -> true, failures::add);
        try {
            var failure = assertThrows(java.util.concurrent.ExecutionException.class,
                    () -> runtime.own(teacher).get(5, TimeUnit.SECONDS));
            assertEquals(UNAVAILABLE, ((ClassroomException) failure.getCause()).reason());
            assertEquals(1, failures.size());
            assertEquals("preserve", Files.readString(blocked));
        } finally { runtime.close(); }
        assertTrue(runtime.own(teacher).isCompletedExceptionally());
    }

    private void expect(ClassroomException.Reason reason, Runnable action) {
        assertEquals(reason, assertThrows(ClassroomException.class, action::run).reason());
    }
    private int scalar(String query) {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database());
             var statement = connection.createStatement(); var rows = statement.executeQuery(query)) {
            assertTrue(rows.next());
            return rows.getInt(1);
        } catch (Exception failure) { throw new AssertionError(failure); }
    }
    private void sql(String query) throws Exception {
        try (var connection = DriverManager.getConnection("jdbc:sqlite:" + database()); var statement = connection.createStatement()) {
            statement.execute(query);
        }
    }
    private List<Object> race(Supplier<Classroom> first, Supplier<Classroom> second) throws Exception {
        var start = new CountDownLatch(1);
        var a = CompletableFuture.supplyAsync(() -> attempt(start, first));
        var b = CompletableFuture.supplyAsync(() -> attempt(start, second));
        start.countDown();
        return List.of(a.get(10, TimeUnit.SECONDS), b.get(10, TimeUnit.SECONDS));
    }
    private Object attempt(CountDownLatch start, Supplier<Classroom> action) {
        try { start.await(); return action.get(); }
        catch (ClassroomException expected) { return expected.reason(); }
        catch (InterruptedException interrupted) { Thread.currentThread().interrupt(); throw new AssertionError(interrupted); }
    }
}
