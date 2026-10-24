package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

class ClassroomDisbandTest {
    @TempDir Path dir;
    final UUID teacher=UUID.randomUUID(), student=UUID.randomUUID(), applicant=UUID.randomUUID(), invited=UUID.randomUUID();
    Path db() { return dir.resolve("classroom.db"); }
    ClassroomService service() { return new ClassroomService(new SqliteClassroomRepository(db()),id->true); }
    long scalar(String sql) throws Exception {
        try(var c=DriverManager.getConnection("jdbc:sqlite:"+db());var s=c.createStatement();var r=s.executeQuery(sql)) {
            assertTrue(r.next()); return r.getLong(1);
        }
    }
    static void expect(ClassroomException.Reason reason,Runnable action) {
        assertEquals(reason,assertThrows(ClassroomException.class,action::run).reason());
    }
    @Test void closureReleasesEveryRoleCancelsOnlyPendingIntakesAndKeepsHistoryAfterRestart() throws Exception {
        var s=service(); var c=s.create(teacher,"Original","Description");
        var request=s.request(student,c.id(),"00123"); var member=s.approve(teacher,request.id(),0,"00123");
        var pending=s.request(applicant,c.id(),"002"); s.remember(invited,"Invited"); var invite=s.invite(teacher,invited);
        var closed=s.disband(teacher,c.id(),0); assertEquals(1,closed.revision());
        var reopened=service();
        for(var actor:List.of(teacher,student,applicant,invited)) assertNull(reopened.home(actor).member());
        assertTrue(reopened.browse(student,"",0).items().isEmpty());
        assertEquals(State.APPROVED,reopened.intakes(student,false,0).items().getFirst().state());
        assertEquals(State.CANCELLED,reopened.intakes(applicant,false,0).items().getFirst().state());
        assertEquals(State.CANCELLED,reopened.intakes(invited,false,0).items().getFirst().state());
        assertEquals(2,scalar("SELECT count(*) FROM classroom_memberships WHERE active=0 AND ended_at IS NOT NULL AND revision=1"));
        assertEquals(1,scalar("SELECT count(*) FROM classrooms WHERE closed_at IS NOT NULL AND name='Original' AND description='Description'"));
        assertEquals(1,scalar("SELECT count(*) FROM classroom_memberships WHERE id='"+member.id()+"' AND enrollment='00123'"));
        assertEquals(0,scalar("SELECT count(*) FROM pragma_foreign_key_check"));
        expect(NOT_FOUND,()->reopened.request(applicant,c.id(),"002"));
        expect(STALE,()->reopened.accept(invited,invite.id(),invite.revision(),"003"));
        expect(CLOSED,()->reopened.accept(invited,invite.id(),invite.revision()+1,"003"));
        var next=reopened.create(teacher,"Replacement",""); assertNotEquals(c.id(),next.id());
        expect(FORBIDDEN,()->reopened.disband(teacher,c.id(),closed.revision()));
        expect(FORBIDDEN,()->reopened.approve(teacher,pending.id(),pending.revision(),"002"));
        assertEquals(next,reopened.own(teacher).orElseThrow());
        var again=reopened.request(student,next.id(),"00123"); reopened.approve(teacher,again.id(),again.revision(),"00123");
        assertEquals(next.id(),reopened.home(student).classroom().id());
    }
    @Test void permissionOwnershipAndPresentedRevisionAreRequired() {
        var allowed=new AtomicBoolean(true); var repo=new SqliteClassroomRepository(db());
        var s=new ClassroomService(repo,id->allowed.get()); var c=s.create(teacher,"Original","");
        expect(FORBIDDEN,()->s.disband(UUID.randomUUID(),c.id(),0));
        expect(FORBIDDEN,()->s.disband(teacher,UUID.randomUUID(),0));
        allowed.set(false); expect(FORBIDDEN,()->s.disband(teacher,c.id(),0)); allowed.set(true);
        var changed=s.update(teacher,c.id(),0,"Changed","");
        expect(STALE,()->s.disband(teacher,c.id(),0)); assertEquals(changed,s.own(teacher).orElseThrow());
        s.disband(teacher,c.id(),changed.revision()); expect(FORBIDDEN,()->s.disband(teacher,c.id(),changed.revision()));
    }
    @Test void transactionFailureRollsBackClassLinksAndIntakesTogether() throws Exception {
        var s=service(); var c=s.create(teacher,"Original",""); s.request(applicant,c.id(),"002");
        try(var conn=DriverManager.getConnection("jdbc:sqlite:"+db());var q=conn.createStatement()) {
            q.execute("CREATE TRIGGER fail_disband BEFORE UPDATE ON classroom_intakes BEGIN SELECT RAISE(ABORT,'injected'); END");
        }
        expect(UNAVAILABLE,()->s.disband(teacher,c.id(),0)); assertEquals(c,s.own(teacher).orElseThrow());
        assertEquals(State.REQUESTED,s.intakes(applicant,false,0).items().getFirst().state());
        assertEquals(1,scalar("SELECT count(*) FROM classroom_memberships WHERE active=1"));
    }
    @Test void concurrentApprovalAndClosureCannotLeaveALinkInAClosedClass() throws Exception {
        var first=service(); var c=first.create(teacher,"Original",""); var request=first.request(student,c.id(),"00123"); var second=service();
        var start=new CountDownLatch(1);
        try(var executor=Executors.newFixedThreadPool(2)) {
            Supplier<Object> close=()->first.disband(teacher,c.id(),0);
            Supplier<Object> approve=()->second.approve(teacher,request.id(),0,"00123");
            var a=CompletableFuture.supplyAsync(()->race(start,close),executor);
            var b=CompletableFuture.supplyAsync(()->race(start,approve),executor); start.countDown();
            assertInstanceOf(Classroom.class,a.get(10,TimeUnit.SECONDS));
            var result=b.get(10,TimeUnit.SECONDS); assertTrue(result instanceof Member || result==FORBIDDEN);
        }
        assertNull(first.home(student).member()); assertNull(first.home(teacher).member());
        assertEquals(0,scalar("SELECT count(*) FROM classroom_memberships WHERE active=1"));
        assertEquals(0,scalar("SELECT count(*) FROM classroom_intakes WHERE state IN('INVITED','REQUESTED')"));
    }
    private static Object race(CountDownLatch start,Supplier<Object> operation) {
        try { assertTrue(start.await(5,TimeUnit.SECONDS)); return operation.get(); }
        catch(ClassroomException failure) { return failure.reason(); }
        catch(InterruptedException failure) { Thread.currentThread().interrupt(); throw new AssertionError(failure); }
    }
    @Test void runtimeNotifiesOnceAfterTheWholeClosureIsCommitted() throws Exception {
        var changes=new CopyOnWriteArrayList<ClassroomRuntime.Change>();
        try(var runtime=new ClassroomRuntime(dir,id->true,failure->fail(failure))) {
            var c=runtime.create(teacher,"Original","").get(5,TimeUnit.SECONDS);
            var request=runtime.request(student,c.id(),"00123").get(5,TimeUnit.SECONDS);
            runtime.approve(teacher,request.id(),0,"00123").get(5,TimeUnit.SECONDS);
            try(var listener=runtime.listen(student,change->{
                try { assertEquals(0,scalar("SELECT count(*) FROM classroom_memberships WHERE active=1")); }
                catch(Exception failure) { throw new AssertionError(failure); }
                changes.add(change);
            })) {
                runtime.disband(teacher,c.id(),0).get(5,TimeUnit.SECONDS);
                assertEquals(List.of(new ClassroomRuntime.Change(c.id(),teacher,true)),changes);
                assertNull(runtime.home(student).get(5,TimeUnit.SECONDS).member());
            }
        }
    }
}
