package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

class ClassroomAdmissionsTest {
    @TempDir Path dir;
    final UUID teacher = UUID.randomUUID(), other = UUID.randomUUID(), student = UUID.randomUUID();
    ClassroomService service() { return new ClassroomService(new SqliteClassroomRepository(dir.resolve("classroom.db")), id -> id.equals(teacher) || id.equals(other)); }
    Classroom classroom(ClassroomService s) {
        s.remember(teacher,"Professor Átila"); s.remember(student,"Aluno");
        return s.create(teacher,"Álgebra A","Descrição %server.fake");
    }
    @Test void requestRequiresApprovalAndPreservesLiteralEnrollmentAndNamesAcrossReopening() {
        var s=service(); var c=classroom(s);
        var i=s.request(student,c.id(),"  00123  ");
        assertNull(s.home(student).member());
        assertEquals(State.REQUESTED,i.state());
        assertEquals(i.id(),s.request(student,c.id(),"different").id());
        var m=s.approve(teacher,i.id(),i.revision(),"00123");
        assertEquals("00123",m.enrollment());
        assertEquals(m.id(),s.approve(teacher,i.id(),i.revision(),"00123").id());
        var reopened=service();
        assertEquals(m,reopened.home(student).member());
        assertEquals(1,reopened.members(teacher,0).items().size());
        reopened.remember(student,"Novo nome");
        assertEquals(m.playerId(),reopened.members(teacher,0).items().getFirst().playerId());
        assertEquals("Novo nome",reopened.members(teacher,0).items().getFirst().name());
    }
    @Test void invitePersistsAndAcceptOnlyCreatesARequest() {
        var s=service(); classroom(s);
        var i=s.invite(teacher,student);
        assertNull(i.enrollment()); assertEquals(State.INVITED,i.state());
        assertEquals(i.id(),s.invite(teacher,student).id());
        s=service(); var pending=s.intakes(student,false,0).items().getFirst();
        var accepted=s.accept(student,pending.id(),pending.revision(),"00Ab C");
        assertEquals(Origin.INVITE,accepted.origin()); assertEquals(State.REQUESTED,accepted.state());
        assertNull(s.home(student).member());
        assertEquals("00Ab C",s.approve(teacher,accepted.id(),accepted.revision(),"00Ab C").enrollment());
    }
    @Test void bothOriginsConvergeAndFirstLinkClosesOtherIntakesIncludingTeacherCreation() {
        var s=service(); var a=classroom(s); var b=s.create(other,"B","");
        var invite=s.invite(teacher,student);
        var request=s.request(student,a.id(),"001");
        assertEquals(invite.id(),request.id()); assertEquals(State.REQUESTED,request.state());
        assertEquals(request.id(),s.invite(teacher,student).id());
        s.request(student,b.id(),"002");
        s.approve(teacher,request.id(),request.revision(),"001");
        assertTrue(s.intakes(student,false,0).items().stream().noneMatch(i->i.state()==State.INVITED || i.state()==State.REQUESTED));
        assertTrue(s.intakes(other,true,0).items().isEmpty());
        expect(ALREADY_MEMBER,()->s.request(student,b.id(),"003"));
        expect(FORBIDDEN,()->s.create(student,"Illegal", ""));
    }
    @Test void creationAsTeacherSupersedesTheirPendingStudentRequests() {
        var s=service(); var a=classroom(s);
        s.request(other,a.id(),"01"); s.create(other,"B","");
        assertTrue(s.intakes(other,false,0).items().stream().noneMatch(i->i.state()==State.INVITED || i.state()==State.REQUESTED));
        assertTrue(s.intakes(teacher,true,0).items().isEmpty());
    }
    @Test void pendingEnrollmentsAreNotReservedButActiveOnesAreUniqueAndCaseSensitive() {
        var s=service(); var c=classroom(s); UUID second=UUID.randomUUID();
        var a=s.request(student,c.id(),"001a"); var b=s.request(second,c.id(),"001a");
        s.approve(teacher,a.id(),a.revision(),"001a");
        expect(DUPLICATE_ENROLLMENT,()->s.approve(teacher,b.id(),b.revision(),"001a"));
        var m=s.approve(teacher,b.id(),b.revision(),"001A");
        expect(DUPLICATE_ENROLLMENT,()->s.correct(teacher,m.id(),m.revision(),"001a"));
        assertEquals("001A",s.home(second).member().enrollment());
    }
    @Test void rejectsEmptyOrOversizedEnrollmentWithoutCreatingAProcess() {
        var s=service(); var c=classroom(s);
        for(String value:new String[]{null,""," \t ","a".repeat(65),"1\n2","x\u0000y"}) expect(INVALID_ENROLLMENT,()->s.request(student,c.id(),value));
        assertTrue(s.intakes(teacher,true,0).items().isEmpty());
        var i=s.request(student,c.id(),"a".repeat(64));
        assertEquals(64,s.approve(teacher,i.id(),i.revision(),i.enrollment()).enrollment().length());
    }
    @Test void ownershipAndPermissionAreCheckedOnEveryPrivateOperation() {
        var allowed=new AtomicBoolean(true);
        var s=new ClassroomService(new SqliteClassroomRepository(dir.resolve("classroom.db")),id->allowed.get());
        var c=classroom(s); s.create(other,"Other",""); var i=s.request(student,c.id(),"01");
        expect(FORBIDDEN,()->s.approve(other,i.id(),0,"01"));
        expect(FORBIDDEN,()->s.closeIntake(other,i.id(),0,true));
        expect(FORBIDDEN,()->s.accept(other,i.id(),0,"01"));
        expect(FORBIDDEN,()->s.closeIntake(other,i.id(),0,false));
        allowed.set(false);
        expect(FORBIDDEN,()->s.members(teacher,0)); expect(FORBIDDEN,()->s.intakes(teacher,true,0));
        expect(FORBIDDEN,()->s.players(teacher,"",0)); expect(FORBIDDEN,()->s.approve(teacher,i.id(),0,"01"));
        assertEquals(i,s.intakes(student,false,0).items().getFirst());
        assertEquals(Kind.BROWSE,s.screen(student,Kind.MEMBERS,"",0).kind());
    }
    @Test void cancellationRejectionAndRemovalKeepHistoryAndRequireNewProcessesForReentry() {
        var s=service(); var c=classroom(s); var i=s.request(student,c.id(),"001");
        s.closeIntake(teacher,i.id(),0,true);
        expect(CLOSED,()->s.approve(teacher,i.id(),1,"001"));
        var next=s.request(student,c.id(),"001"); assertNotEquals(i.id(),next.id());
        var m=s.approve(teacher,next.id(),0,"001");
        expect(FORBIDDEN,()->s.end(other,m.id(),0,false));
        var corrected=s.correct(teacher,m.id(),0,"009");
        expect(STALE,()->s.end(teacher,m.id(),0,true));
        var ended=s.end(student,m.id(),corrected.revision(),false);
        assertFalse(ended.active()); assertNotNull(ended.endedAt()); assertNull(s.home(student).member());
        assertFalse(s.approve(teacher,next.id(),0,"001").active());
        var again=s.request(student,c.id(),"009"); var newMember=s.approve(teacher,again.id(),0,"009");
        assertNotEquals(m.id(),newMember.id());
        s.end(teacher,m.id(),ended.revision(),true); // an old confirmation cannot remove the new link
        assertEquals(newMember.id(),s.home(student).member().id());
    }
    @Test void inviteRefusalAndTeacherCancellationPersist() {
        var s=service(); classroom(s); var i=s.invite(teacher,student);
        assertEquals(State.DECLINED,s.closeIntake(student,i.id(),0,false).state());
        var next=s.invite(teacher,student);
        assertEquals(State.CANCELLED,s.closeIntake(teacher,next.id(),0,true).state());
        assertEquals(Set.of(State.DECLINED,State.CANCELLED),service().intakes(student,false,0).items().stream().map(Intake::state).collect(java.util.stream.Collectors.toSet()));
        expect(NOT_FOUND,()->s.invite(teacher,UUID.randomUUID()));
    }
    @Test void twoTeachersApprovingSameStudentCommitOneLinkAndCloseTheOtherProcess() throws Exception {
        var s=service(); var a=classroom(s); var b=s.create(other,"B","");
        var x=s.request(student,a.id(),"001"); var y=s.request(student,b.id(),"002");
        var second=service(); var start=new CountDownLatch(1);
        var first=CompletableFuture.supplyAsync(()->attempt(start,()->s.approve(teacher,x.id(),0,"001")));
        var next=CompletableFuture.supplyAsync(()->attempt(start,()->second.approve(other,y.id(),0,"002")));
        start.countDown();
        var results=List.of(first.get(10,TimeUnit.SECONDS),next.get(10,TimeUnit.SECONDS));
        assertEquals(1,results.stream().filter(Member.class::isInstance).count());
        assertEquals(1,s.members(teacher,0).items().size()+s.members(other,0).items().size());
        assertTrue(s.intakes(student,false,0).items().stream().noneMatch(i->i.state()==State.INVITED || i.state()==State.REQUESTED));
    }
    @Test void approvalFailureRollsBackMembershipAndOtherPendingProcesses() throws Exception {
        var s=service(); var a=classroom(s); var b=s.create(other,"B","");
        var i=s.request(student,a.id(),"001"); s.request(student,b.id(),"002");
        sql("CREATE TRIGGER fail_approval BEFORE UPDATE ON classroom_intakes WHEN NEW.state='APPROVED' BEGIN SELECT RAISE(FAIL,'injected'); END");
        expect(UNAVAILABLE,()->s.approve(teacher,i.id(),0,"001"));
        assertNull(s.home(student).member()); assertEquals(2,s.intakes(student,false,0).items().size());
        sql("DROP TRIGGER fail_approval"); s.approve(teacher,i.id(),0,"001");
    }
    @Test void concurrentStudentsCannotClaimTheSameEnrollment() throws Exception {
        var s=service(); var c=classroom(s); UUID secondStudent=UUID.randomUUID();
        var a=s.request(student,c.id(),"00123"); var b=s.request(secondStudent,c.id(),"00123"); var second=service();
        var start=new CountDownLatch(1);
        var first=CompletableFuture.supplyAsync(()->attempt(start,()->s.approve(teacher,a.id(),0,"00123")));
        var next=CompletableFuture.supplyAsync(()->attempt(start,()->second.approve(teacher,b.id(),0,"00123")));
        start.countDown(); var results=List.of(first.get(10,TimeUnit.SECONDS),next.get(10,TimeUnit.SECONDS));
        assertEquals(1,results.stream().filter(Member.class::isInstance).count());
        assertEquals(1,results.stream().filter(DUPLICATE_ENROLLMENT::equals).count());
    }
    @Test void approvalAndTeacherCreationForTheSameUuidCannotCommitTwoRoles() throws Exception {
        var s=service(); var c=classroom(s); var i=s.request(other,c.id(),"00123"); var second=service();
        var start=new CountDownLatch(1);
        var first=CompletableFuture.supplyAsync(()->attempt(start,()->s.approve(teacher,i.id(),0,"00123")));
        var next=CompletableFuture.supplyAsync(()->attempt(start,()->second.create(other,"Other","")));
        start.countDown(); var results=List.of(first.get(10,TimeUnit.SECONDS),next.get(10,TimeUnit.SECONDS));
        assertEquals(1,results.stream().filter(value->value instanceof Member || value instanceof Classroom).count());
        assertNotNull(s.home(other).member());
        assertTrue(s.intakes(other,false,0).items().stream().noneMatch(value->value.state()==State.REQUESTED || value.state()==State.INVITED));
    }
    @Test void publicPagesExposeOnlySummariesAndSearchIsLiteralAndUnicodeAware() {
        var s=service(); var c=classroom(s); s.request(student,c.id(),"SECRET-00123");
        assertEquals(c.id(),s.browse(student,"ÁLGEBRA",0).items().getFirst().id());
        assertTrue(s.browse(student,"%",0).items().isEmpty());
        assertFalse(s.browse(student,"",0).items().toString().contains("SECRET"));
        for(int n=0;n<22;n++) { UUID id=UUID.randomUUID(); s.remember(id,"Player "+n); }
        assertEquals(20,s.players(teacher,"",0).items().size()); assertTrue(s.players(teacher,"",0).hasNext());
        assertFalse(s.players(teacher,"",1).hasNext());
        assertThrows(UnsupportedOperationException.class,()->s.browse(student,"",0).items().clear());
    }
    @Test void messageUsesRevisionPreservesLiteralTextAndDoesNotLeakInPublicListing() {
        var s=service(); var c=classroom(s); var changed=s.message(teacher,c.id(),0,"  %server.fake\r\n<b>Olá</b>  ");
        assertEquals("%server.fake\n<b>Olá</b>",service().own(teacher).orElseThrow().message());
        expect(STALE,()->s.update(teacher,c.id(),0,"Other",""));
        expect(INVALID_MESSAGE,()->s.message(teacher,c.id(),changed.revision(),"x".repeat(2049)));
        assertFalse(s.browse(student,"",0).items().toString().contains("<b>Olá"));
        assertEquals("",s.message(teacher,c.id(),1,"").message());
    }
    private Object attempt(CountDownLatch start,Supplier<?> action) {
        try { start.await(); return action.get(); } catch(ClassroomException expected) { return expected.reason(); }
        catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new AssertionError(e); }
    }
    private void sql(String sql) throws Exception { try(var c=DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("classroom.db"));var s=c.createStatement()){s.execute(sql);} }
    private void expect(ClassroomException.Reason reason,Runnable action) { assertEquals(reason,assertThrows(ClassroomException.class,action::run).reason()); }
}
