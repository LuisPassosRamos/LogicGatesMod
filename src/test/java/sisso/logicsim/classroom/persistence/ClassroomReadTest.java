package sisso.logicsim.classroom.persistence;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import sisso.logicsim.classroom.ClassroomUiFixture;
import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import java.nio.file.Path;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

class ClassroomReadTest {
    @TempDir Path dir;
    final UUID teacher=UUID.randomUUID(), student=UUID.randomUUID(), observer=UUID.randomUUID(), freeTeacher=UUID.randomUUID();
    final AtomicBoolean allowed=new AtomicBoolean(true);
    ClassroomJdbcProbe probe;
    SqliteClassroomRepository repository;
    ClassroomService service;
    Classroom classroom;
    Member joined;
    Intake pending;
    @BeforeEach void setup() throws Exception {
        probe=new ClassroomJdbcProbe(); repository=new SqliteClassroomRepository(dir.resolve("classroom.db"));
        service=new ClassroomService(repository,id->id.equals(freeTeacher) || id.equals(teacher) && allowed.get());
        service.remember(teacher,"Professor Átila"); service.remember(student,"Aluno"); service.remember(observer,"Observer");
        classroom=service.create(teacher,"A","Description");
        var request=service.request(student,classroom.id(),"00123"); joined=service.approve(teacher,request.id(),request.revision(),"00123");
        pending=service.request(observer,classroom.id(),"002"); probe.reset();
    }
    @AfterEach void restoreDriver() throws Exception { if(probe!=null) probe.close(); }
    // Frozen composition of the rc.9 API: comparison must not reuse the new aggregate read policy.
    Home previousHome(UUID actor,boolean canTeach) {
        var member=repository.member(actor).orElse(null);
        if(member!=null && member.role()==Role.TEACHER && !canTeach) throw new ClassroomException(FORBIDDEN);
        var room=member==null?null:repository.findByMember(actor).orElseThrow(()->new ClassroomException(UNAVAILABLE));
        return new Home(room,member,room==null?"":repository.playerName(room.teacherId()),canTeach);
    }
    Screen previousScreen(UUID actor,Kind kind,String query,int page,boolean canTeach) {
        var home=previousHome(actor,canTeach); boolean teaching=home.member()!=null && home.member().role()==Role.TEACHER;
        if(home.member()!=null && !teaching) kind=Kind.DETAIL;
        else if(teaching && (kind==Kind.BROWSE || kind==Kind.DETAIL)) kind=Kind.MEMBERS;
        else if(!teaching && kind!=Kind.BROWSE && kind!=Kind.INTAKES && !(kind==Kind.SETTINGS && home.canTeach())) kind=Kind.BROWSE;
        Page<?> rows=switch(kind) {
            case BROWSE->service.browse(actor,query,page); case MEMBERS->service.members(actor,page);
            case INTAKES->service.intakes(actor,teaching,page); case PLAYERS->service.players(actor,query,page);
            case SETTINGS,DETAIL->new Page<>(List.of(),0,false);
        };
        return new Screen(home,kind,rows,teaching?repository.studentCount(actor):0);
    }
    @Test void homeKeepsNamesAndUuidFallbackWithOneSelectAndOneClosedConnection() {
        for(var actor:List.of(teacher,student,observer,freeTeacher)) {
            var expected=previousHome(actor,actor.equals(teacher)||actor.equals(freeTeacher)); probe.reset();
            assertEquals(expected,service.home(actor)); assertEquals(1,probe.connections.get()); assertEquals(1,probe.selects.get()); assertEquals(1,probe.closed.get());
        }
        var unremembered=service.create(freeTeacher,"Fallback",""); probe.reset();
        var home=service.home(freeTeacher); assertEquals(freeTeacher.toString(),home.teacherName());
        assertEquals(freeTeacher.toString(),home.member().name()); assertEquals(unremembered,home.classroom());
        assertEquals(1,probe.selects.get()); assertEquals(0,probe.active.get());
    }
    @Test void everyRoleAndTabMatchesPreviousCompositionWithOneConnection() {
        for(var actor:List.of(teacher,student,observer,freeTeacher)) for(var kind:Kind.values()) for(int page:new int[]{0,1}) {
            var expected=previousScreen(actor,kind,"  a  ",page,actor.equals(teacher)||actor.equals(freeTeacher)); probe.reset();
            var actual=service.screen(actor,kind,"  a  ",page); assertEquals(expected,actual,actor+" "+kind+" "+page);
            assertEquals(1,probe.connections.get()); assertEquals(1,probe.closed.get()); assertEquals(0,probe.active.get());
            assertTrue(probe.selects.get()<=3); assertEquals(1,probe.begins.get()); assertEquals(1,probe.commits.get());
            assertEquals(0,probe.rollbacks.get());
        }
    }
    @Test void memberScreenMeetsStructuralTargetAndPermissionIsCheckedInTheService() {
        var calls=new AtomicInteger(); var counted=new ClassroomService(repository,id->{calls.incrementAndGet(); return true;});
        var screen=counted.screen(teacher,Kind.MEMBERS,"",0);
        assertEquals(1,screen.studentCount()); assertEquals("00123",((Member)screen.rows().items().getFirst()).enrollment());
        assertEquals(1,calls.get()); assertEquals(1,probe.connections.get()); assertEquals(3,probe.selects.get());
        assertEquals(1,probe.closed.get());
    }
    @Test void revokedTeacherAndInvalidListPageRollBackBeforeReturningPrivateRows() {
        allowed.set(false); assertEquals(FORBIDDEN,assertThrows(ClassroomException.class,()->service.screen(teacher,Kind.MEMBERS,"",0)).reason());
        assertEquals(1,probe.selects.get()); assertEquals(1,probe.rollbacks.get()); assertEquals(0,probe.active.get());
        allowed.set(true); probe.reset();
        assertEquals(NOT_FOUND,assertThrows(ClassroomException.class,()->service.screen(teacher,Kind.MEMBERS,"",-1)).reason());
        assertEquals(1,probe.selects.get()); assertEquals(1,probe.rollbacks.get()); assertEquals(1,probe.closed.get());
        // As in rc.9, a student's DETAIL ignores list query/page parameters.
        assertEquals(Kind.DETAIL,service.screen(student,Kind.PLAYERS,null,-1).kind());
    }
    @Test void paginatedRowsCountsAndScopeRemainEquivalentOnTheExistingFixture() throws Exception {
        Path fixture=dir.resolve("fixture"); ClassroomUiFixture.create(fixture,teacher,student);
        repository=new SqliteClassroomRepository(fixture.resolve("classroom.db")); service=new ClassroomService(repository,id->id.equals(teacher));
        for(var kind:List.of(Kind.MEMBERS,Kind.INTAKES,Kind.PLAYERS)) for(int page:new int[]{0,1}) {
            var expected=previousScreen(teacher,kind,"",page,true); probe.reset(); var actual=service.screen(teacher,kind,"",page);
            assertEquals(expected,actual); assertEquals(25,actual.studentCount()); assertEquals(1,probe.connections.get()); assertEquals(3,probe.selects.get());
        }
        assertEquals(20,service.screen(teacher,Kind.MEMBERS,"",0).rows().items().size());
        assertEquals(5,service.screen(teacher,Kind.MEMBERS,"",1).rows().items().size());
        service.end(student,service.home(student).member().id(),0,false);
        var history=service.screen(student,Kind.INTAKES,"",1); assertEquals(previousScreen(student,Kind.INTAKES,"",1,false),history);
        assertTrue(history.rows().items().stream().allMatch(row->row instanceof Intake i && i.studentId().equals(student)));
    }
    @Test void selectFailureRollsBackAndReleasesTheConnectionForAWritingOperation() {
        probe.before=sql->{if(sql.startsWith("SELECT count(*)")) throw new SQLException("injected read failure");};
        assertEquals(UNAVAILABLE,assertThrows(ClassroomException.class,()->service.screen(teacher,Kind.MEMBERS,"",0)).reason());
        assertEquals(1,probe.rollbacks.get()); assertEquals(1,probe.closed.get()); assertEquals(0,probe.active.get());
        probe.before=sql->{};
        assertEquals("Still writable",service.update(teacher,classroom.id(),0,"Still writable","").name());
        assertEquals(1,service.screen(teacher,Kind.MEMBERS,"",0).studentCount());
    }
    @Test void nativePermissionAloneCannotAuthorizeAClassroomOwnedBySomeoneElse() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("classroom.db"));var s=c.createStatement()) {
            s.execute("UPDATE classrooms SET teacher_uuid='"+freeTeacher+"'");
        }
        probe.reset();
        assertEquals(FORBIDDEN,assertThrows(ClassroomException.class,()->service.screen(teacher,Kind.SETTINGS,"",0)).reason());
        assertEquals(1,probe.selects.get()); assertEquals(1,probe.rollbacks.get()); assertEquals(1,probe.closed.get());
    }
    @Test void inconsistentClosedClassroomIsUnavailableRatherThanAnUnlinkedHome() throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("classroom.db"));var s=c.createStatement()) {
            s.execute("UPDATE classrooms SET closed_at=123");
        }
        assertEquals(UNAVAILABLE,assertThrows(ClassroomException.class,()->service.home(teacher)).reason());
        assertEquals(UNAVAILABLE,assertThrows(ClassroomException.class,()->service.screen(student,Kind.DETAIL,"",0)).reason());
        assertEquals(0,probe.active.get());
    }
    @ParameterizedTest @ValueSource(strings={"approve","leave","disband"})
    void screenIsOneSnapshotWhileAnotherConnectionChangesMemberships(String operation) throws Exception {
        var writer=new ClassroomService(new SqliteClassroomRepository(dir.resolve("classroom.db")),id->true);
        var expected=service.screen(teacher,Kind.MEMBERS,"",0);
        var readReady=new CountDownLatch(1); var commitReady=new CountDownLatch(1); var paused=new AtomicBoolean();
        probe.afterSelect=()->{
            if(Thread.currentThread().getName().equals("snapshot-reader") && paused.compareAndSet(false,true)) {
                readReady.countDown();
                try { if(!commitReady.await(4,TimeUnit.SECONDS)) throw new AssertionError("Writer did not reach COMMIT"); }
                catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
            }
        };
        // Signal before COMMIT: waiting for a committed writer while retaining a read lock would deadlock in DELETE journal mode.
        probe.before=sql->{if(Thread.currentThread().getName().equals("snapshot-writer") && sql.equals("COMMIT")) commitReady.countDown();};
        try(var pool=Executors.newFixedThreadPool(2)) {
            var reading=pool.submit(()->{Thread.currentThread().setName("snapshot-reader"); return service.screen(teacher,Kind.MEMBERS,"",0);});
            assertTrue(readReady.await(4,TimeUnit.SECONDS));
            var writing=pool.submit(()->{
                Thread.currentThread().setName("snapshot-writer");
                return switch(operation) {
                    case "approve"->writer.approve(teacher,pending.id(),pending.revision(),"002");
                    case "leave"->writer.end(student,joined.id(),joined.revision(),false);
                    default->writer.disband(teacher,classroom.id(),classroom.revision());
                };
            });
            assertEquals(expected,reading.get(10,TimeUnit.SECONDS)); writing.get(10,TimeUnit.SECONDS);
        }
        probe.before=sql->{}; probe.afterSelect=()->{}; assertEquals(0,probe.active.get());
        var after=service.screen(teacher,Kind.MEMBERS,"",0);
        if(operation.equals("disband")) { assertNull(after.home().member()); assertEquals(Kind.BROWSE,after.kind()); }
        else assertEquals(operation.equals("approve")?2:0,after.studentCount());
    }
}
