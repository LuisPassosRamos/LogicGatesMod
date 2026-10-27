package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import sisso.logicsim.classroom.domain.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import sisso.logicsim.classroom.learning.LearningViews.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

class LearningStoreTest {
    @TempDir Path dir;
    UUID teacher=UUID.randomUUID(),student=UUID.randomUUID(),other=UUID.randomUUID();
    AtomicBoolean permission=new AtomicBoolean(true);
    ClassroomService classes;
    LearningStore store;
    SubmissionFiles files;
    Classroom classroom;
    UUID membership;
    Clock clock=Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"),ZoneOffset.UTC);
    @BeforeEach void setup() {
        var repo=new SqliteClassroomRepository(dir.resolve("classroom.db"));
        classes=new ClassroomService(repo,id->id.equals(teacher)&&permission.get());
        classroom=classes.create(teacher,"Learning",""); classes.remember(student,"Student");
        var request=classes.request(student,classroom.id(),"00123"); membership=classes.approve(teacher,request.id(),request.revision(),"00123").id();
        store=new LearningStore(dir.resolve("classroom.db"),id->id.equals(teacher)&&permission.get(),LearningLimits.DEFAULT,clock);
        files=new SubmissionFiles(dir.resolve("submissions"),LearningLimits.DEFAULT);
    }
    Activity published() { var a=store.create(teacher);a=store.save(teacher,a.id(),a.revision(),new Draft("A","Statement",clock.millis()+60000,1000L));return store.publish(teacher,a.id(),a.revision()); }
    AreaSnapshot snapshot() { return new AreaSnapshot(1,2,1,1,List.of(new AreaSnapshot.Block(0,0,0,"Rock_Stone",0,0,null))); }
    Submission submit(Activity a) { return store.submit(student,a.id(),a.published().number(),membership,UUID.randomUUID(),files.write(snapshot())); }
    @Test void deletingIncompleteAndArchivedDraftsUpdatesCatalogAndSurvivesReopen() {
        var blank=store.create(teacher,Draft.empty(),false);
        var kept=store.create(teacher,new Draft("Keep","",null,null),false);
        assertEquals(2,store.catalog(teacher,LearningStore.Filter.ALL,0).overview().drafts());
        store.deleteDraft(teacher,blank.id(),blank.revision());
        assertEquals(NOT_FOUND,assertThrows(ClassroomException.class,()->store.activity(teacher,blank.id())).reason());
        assertEquals(1,store.catalog(teacher,LearningStore.Filter.DRAFT,0).overview().drafts());
        var archived=store.archive(teacher,kept.id(),kept.revision(),true);
        store.deleteDraft(teacher,archived.id(),archived.revision());
        var reopened=new LearningStore(dir.resolve("classroom.db"),id->id.equals(teacher),LearningLimits.DEFAULT,clock);
        assertTrue(reopened.activities(teacher,LearningStore.Filter.ALL,0).items().isEmpty());
        assertTrue(reopened.activities(teacher,LearningStore.Filter.ARCHIVED,0).items().isEmpty());
        assertTrue(reopened.contentIds().isEmpty());
    }
    @Test void draftDeletionRejectsForeignStudentsRevokedPermissionAndStaleConfirmation() {
        var draft=store.create(teacher,Draft.empty(),false);
        assertEquals(FORBIDDEN,assertThrows(ClassroomException.class,()->store.deleteDraft(student,draft.id(),draft.revision())).reason());
        var repo=new SqliteClassroomRepository(dir.resolve("classroom.db"));
        new ClassroomService(repo,id->true).create(other,"Other","");
        var both=new LearningStore(dir.resolve("classroom.db"),id->true,LearningLimits.DEFAULT,clock);
        assertEquals(NOT_FOUND,assertThrows(ClassroomException.class,()->both.deleteDraft(other,draft.id(),draft.revision())).reason());
        var changed=store.save(teacher,draft.id(),draft.revision(),new Draft("Changed","Keep",null,1000L));
        assertEquals(STALE,assertThrows(ClassroomException.class,()->store.deleteDraft(teacher,draft.id(),draft.revision())).reason());
        permission.set(false);
        assertEquals(FORBIDDEN,assertThrows(ClassroomException.class,()->store.deleteDraft(teacher,changed.id(),changed.revision())).reason());
        permission.set(true);assertEquals(changed,store.activity(teacher,changed.id()));
    }
    @Test void publishedActivityCannotBeDeletedEvenWithPrivateEditsAndArchivedHistory() {
        var activity=published();var submitted=submit(activity);var graded=store.grade(teacher,submitted.id(),0,500,"Keep grade");
        var edited=store.save(teacher,activity.id(),activity.revision(),Draft.empty());
        assertEquals(PUBLISHED_ACTIVITY,assertThrows(LearningException.class,()->store.deleteDraft(teacher,edited.id(),edited.revision())).code());
        var archived=store.archive(teacher,edited.id(),edited.revision(),true);
        assertEquals(PUBLISHED_ACTIVITY,assertThrows(LearningException.class,()->store.deleteDraft(teacher,archived.id(),archived.revision())).code());
        assertEquals(graded,store.submission(teacher,submitted.id()));
        assertEquals(activity.published(),store.publication(teacher,activity.id(),1));
        assertEquals(snapshot(),files.read(submitted.content(),submitted.checksum()));
    }
    @Test void concurrentPublicationAndDeletionCannotRemovePublishedHistory() throws Exception {
        var draft=store.create(teacher,new Draft("Race","Statement",clock.millis()+60000,1000L),false);
        var gate=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var deletion=pool.submit(()->{gate.await();try{store.deleteDraft(teacher,draft.id(),draft.revision());return true;}catch(ClassroomException conflict){assertTrue(Set.of(STALE,NOT_FOUND).contains(conflict.reason()));return false;}});
            var publication=pool.submit(()->{gate.await();try{store.publish(teacher,draft.id(),draft.revision());return true;}catch(ClassroomException conflict){assertEquals(NOT_FOUND,conflict.reason());return false;}});
            gate.countDown();boolean deleted=deletion.get(),published=publication.get();assertNotEquals(deleted,published);
            if(published)assertNotNull(store.activity(teacher,draft.id()).published());
            else assertEquals(NOT_FOUND,assertThrows(ClassroomException.class,()->store.activity(teacher,draft.id())).reason());
        }
    }
    @Test void firstExplicitSaveAndPublicationAreAtomicAndCatalogKeepsStudentMetadataPrivate() {
        var invalid=new Draft("New","Statement",clock.millis()-1,1000L);
        assertThrows(LearningException.class,()->store.create(teacher,invalid,true));
        assertTrue(store.activities(teacher,LearningStore.Filter.ALL,0).items().isEmpty());
        var privateDraft=store.create(teacher,Draft.empty(),false);
        assertNotNull(privateDraft.draft());assertNull(privateDraft.published());
        var published=store.create(teacher,new Draft("Published","D",clock.millis()+60000,1000L),true);
        var sent=submit(published);store.grade(teacher,sent.id(),0,0,"Feedback");
        var teacherCatalog=store.catalog(teacher,LearningStore.Filter.ALL,0);
        assertEquals(2,teacherCatalog.rows().items().size());assertEquals(1,teacherCatalog.overview().published());assertEquals(1,teacherCatalog.overview().drafts());
        var studentCatalog=store.catalog(student,LearningStore.Filter.DRAFT,0);
        assertEquals(1,studentCatalog.rows().items().size());assertEquals(0,studentCatalog.overview().drafts());
        assertEquals(1,studentCatalog.overview().delivered());assertEquals(0L,studentCatalog.progress().get(published.id()).score());
        assertThrows(ClassroomException.class,()->store.create(student,Draft.empty(),false));
    }
    @Test void verificationCriteriaArePrivateVersionedAndUseTheDeliveredPublicationAfterRestart(){
        var rules=new VerificationRules(Map.of(sisso.logicsim.circuit.CircuitNode.Kind.AND,1,sisso.logicsim.circuit.CircuitNode.Kind.LEVER,2),SubmissionVerifierTest.AND);
        var a=store.create(teacher);a=store.saveAndPublish(teacher,a.id(),a.revision(),new Draft("Logic","AND",clock.millis()+60000,1000L,rules));
        var first=submit(a);var changed=new VerificationRules(Map.of(sisso.logicsim.circuit.CircuitNode.Kind.AND,0),"");
        a=store.save(teacher,a.id(),a.revision(),new Draft("Changed","Private",clock.millis()+120000,1000L,changed));
        assertEquals(rules,store.activity(student,a.id()).published().rules());assertNull(store.activity(student,a.id()).draft());
        a=store.publish(teacher,a.id(),a.revision());var second=submit(a);
        new SqliteClassroomRepository(dir.resolve("classroom.db"));
        assertEquals(rules,store.verification(teacher,first.id()).publication().rules());assertEquals(changed,store.verification(teacher,second.id()).publication().rules());
        assertNull(store.submission(student,first.id()).assessment());
        assertThrows(ClassroomException.class,()->store.verification(student,first.id()));assertThrows(ClassroomException.class,()->store.verification(other,first.id()));
        permission.set(false);assertThrows(ClassroomException.class,()->store.verification(teacher,first.id()));
    }
    @Test void invalidTablePublicationRollsBackSavedFieldsAndRevisionButIncompletePrivateDraftCanBeSaved(){
        var a=published();var invalid=new Draft("New","Incomplete",clock.millis()+120000,2000L,new VerificationRules(Map.of(),"00 -> 0"));
        final var before=a;
        assertEquals(INVALID_RULES,assertThrows(LearningException.class,()->store.saveAndPublish(teacher,before.id(),before.revision(),invalid)).code());
        assertEquals(before,store.activity(teacher,a.id()));
        a=store.save(teacher,a.id(),a.revision(),invalid);assertEquals("00 -> 0",a.draft().rules().table());
        assertEquals("",store.activity(student,a.id()).published().rules().table());
    }
    @Test void draftsArePrivateAndPublishedVersionsAreImmutable() {
        var a=store.create(teacher); assertEquals(1000L,a.draft().value()); assertTrue(store.activities(student,LearningStore.Filter.ALL,0).items().isEmpty());
        assertThrows(ClassroomException.class,()->store.activity(student,a.id()));
        var first=published(); var edited=store.save(teacher,first.id(),first.revision(),new Draft("Edited","New",clock.millis()+120000,2000L));
        assertEquals("A",store.activity(student,first.id()).published().title()); assertNull(store.activity(student,first.id()).draft());
        var second=store.publish(teacher,edited.id(),edited.revision()); assertEquals(2,second.published().number());
        assertEquals("A",store.publication(teacher,first.id(),1).title()); assertEquals(1000,store.publication(teacher,first.id(),1).value());
    }
    @Test void incompleteDraftsCanBeSavedButCannotPublishAndDeadlineMustBeFuture() {
        var a=store.create(teacher);
        assertEquals(INVALID_DRAFT,assertThrows(LearningException.class,()->store.publish(teacher,a.id(),0)).code());
        var b=store.save(teacher,a.id(),0,new Draft("T","D",clock.millis(),1000L));
        assertEquals(FUTURE_DEADLINE,assertThrows(LearningException.class,()->store.publish(teacher,b.id(),b.revision())).code());
    }
    @Test void restoreKeepsTheSamePublicationEvenWhenDeadlineHasPassed() {
        var a=published(); var archived=store.archive(teacher,a.id(),a.revision(),true);
        assertTrue(store.activities(student,LearningStore.Filter.ALL,0).items().isEmpty());
        var restored=store.archive(teacher,a.id(),archived.revision(),false);
        assertEquals(a.published(),restored.published());
    }
    @Test void gradeIsAttachedToExactlyOneRevisionAndZeroIsAnAssignedGrade() {
        var a=published(); var first=submit(a); assertNull(first.assessment());
        var zero=store.grade(teacher,first.id(),0,0,"Try again"); assertEquals(0,zero.assessment().score());
        var next=submit(a); assertNull(next.assessment()); assertEquals(2,next.revision());
        assertEquals(0,store.submission(student,first.id()).assessment().score());
        assertEquals(INVALID_SCORE,assertThrows(LearningException.class,()->store.grade(teacher,next.id(),0,1001,"")).code());
        assertEquals(STALE,assertThrows(ClassroomException.class,()->store.grade(teacher,first.id(),0,100,"")).reason());
        assertEquals(1000,store.students(teacher,a.id(),0).items().getFirst().latest().value());
    }
    @Test void submissionsAfterDeadlineAreAcceptedAndMarkedLate() {
        var a=published();
        var late=new LearningStore(dir.resolve("classroom.db"),id->true,LearningLimits.DEFAULT,Clock.offset(clock,Duration.ofMinutes(2)));
        var one=late.submit(student,a.id(),1,membership,UUID.randomUUID(),files.write(snapshot()));
        var two=late.submit(student,a.id(),1,membership,UUID.randomUUID(),files.write(snapshot()));
        assertTrue(one.late()); assertTrue(two.late()); assertEquals(2,two.revision());
    }
    @Test void changedPublicationOrMembershipCannotCommitAnOldCapture() {
        var a=published(); var content=files.write(snapshot());
        var edited=store.save(teacher,a.id(),a.revision(),new Draft("New","D",clock.millis()+120000,2000L));
        store.publish(teacher,a.id(),edited.revision());
        assertEquals(STALE,assertThrows(ClassroomException.class,()->store.submit(student,a.id(),1,membership,UUID.randomUUID(),content)).reason());
        classes.end(student,membership,0,false);
        assertEquals(FORBIDDEN,assertThrows(ClassroomException.class,()->store.submit(student,a.id(),2,membership,UUID.randomUUID(),content)).reason());
    }
    @Test void revocationAndForeignRolesAreRejected() {
        var a=published(); var s=submit(a);
        assertThrows(ClassroomException.class,()->store.create(student)); assertThrows(ClassroomException.class,()->store.submission(other,s.id()));
        permission.set(false);
        assertThrows(ClassroomException.class,()->store.grade(teacher,s.id(),0,100,"").assessment());
        assertThrows(ClassroomException.class,()->store.activities(teacher,LearningStore.Filter.ALL,0));
        assertNotNull(store.submission(student,s.id()));
    }
    @Test void independentWritersAllocateDistinctRevisionsAndOldRecordsSurviveRestart() throws Exception {
        var a=published(); var one=files.write(snapshot());var two=files.write(snapshot());
        try(var pool=Executors.newFixedThreadPool(2)) {
            var x=pool.submit(()->store.submit(student,a.id(),1,membership,UUID.randomUUID(),one));
            var y=pool.submit(()->store.submit(student,a.id(),1,membership,UUID.randomUUID(),two));
            assertEquals(Set.of(1,2),Set.of(x.get().revision(),y.get().revision()));
        }
        new SqliteClassroomRepository(dir.resolve("classroom.db"));
        assertEquals(2,store.submissions(student,a.id(),student,0).items().size());
        assertEquals(snapshot(),files.read(one.id(),one.checksum()));
    }
    @Test void activityPaginationAndHistoricalStudentsAreScoped() {
        for(int i=0;i<25;i++) store.create(teacher);
        assertEquals(20,store.activities(teacher,LearningStore.Filter.ALL,0).items().size());
        assertEquals(5,store.activities(teacher,LearningStore.Filter.ALL,1).items().size());
        var a=published(); var s=submit(a); classes.end(student,membership,0,false);
        var rows=store.students(teacher,a.id(),0).items();assertEquals(1,rows.size());assertFalse(rows.getFirst().active()); assertEquals(s,rows.getFirst().latest());
        assertEquals(student,store.student(teacher,student).id());assertFalse(store.student(teacher,student).active());
        assertNull(store.student(teacher,other));assertThrows(ClassroomException.class,()->store.student(student,student));
    }
    @Test void reservationIsDurableExclusiveAndRemovedSeparatelyFromTheSubmission() {
        var a=published(); var s=submit(a); UUID world=UUID.randomUUID();
        var r=store.reserve(teacher,s.id(),world,0,64,0); assertEquals(List.of(r),store.reviews());
        var info=store.reviewInfo(teacher,r.id());assertEquals(a.published().title(),info.activity());assertEquals(s.revision(),info.revision());
        assertThrows(ClassroomException.class,()->store.reviewInfo(student,r.id()));
        assertNull(store.reviewInfo(teacher,UUID.randomUUID()));
        assertThrows(LearningException.class,()->store.reserve(teacher,s.id(),world,20,64,20));
        store.removedReview(r.id()); assertTrue(store.reviews().isEmpty());assertNotNull(store.submission(student,s.id()));
    }
    @Test void contentChecksumExpansionAndBoundsAreEnforced() throws Exception {
        var content=files.write(snapshot()); Path path=dir.resolve("submissions").resolve(content.id()+".json.gz");
        Files.writeString(path,"tampered");assertEquals(CORRUPT_CONTENT,assertThrows(LearningException.class,()->files.read(content.id(),content.checksum())).code());
        assertThrows(LearningException.class,()->files.write(new AreaSnapshot(1,33,1,1,snapshot().blocks())));
        assertThrows(LearningException.class,()->files.write(new AreaSnapshot(1,2,1,1,List.of(snapshot().blocks().getFirst(),snapshot().blocks().getFirst()))));
    }
    @Test void moneyAndBrasiliaDatesAreExact() {
        assertEquals(1000L,LearningViews.points("10"));assertEquals(123L,LearningViews.points("1,23"));
        assertThrows(LearningException.class,()->LearningViews.points("1.234"));
        assertEquals(clock.millis(),LearningViews.deadline("2026-10-26 09:00"));
        assertThrows(LearningException.class,()->LearningViews.deadline("2026-02-30 10:00"));
        assertThrows(LearningException.class,()->new Draft("A","D",null,0L));
        assertThrows(LearningException.class,()->LearningViews.points("10.000"));
        assertThrows(LearningException.class,()->LearningViews.points("1e2"));
    }
    @Test void gradeUsesSubmittedPublicationValueAfterRepublicationAndNeverChangesAnotherRevision() {
        var a=published();var first=submit(a);
        var draft=store.save(teacher,a.id(),a.revision(),new Draft("New title","New statement",clock.millis()+120000,2000L));
        var secondPublication=store.publish(teacher,a.id(),draft.revision());var second=submit(secondPublication);
        assertEquals(2,second.publication());assertEquals(2000,second.value());
        assertEquals(INVALID_SCORE,assertThrows(LearningException.class,()->store.grade(teacher,first.id(),0,1500,"")).code());
        store.grade(teacher,first.id(),0,1000,"Old revision only");assertNull(store.submission(student,second.id()).assessment());
        assertEquals("Statement",store.publication(student,a.id(),1).description());
    }
    @Test void quotaRefusesOnlyTheNewRevisionAndStartupRecoveryPreservesReferencedFiles() throws Exception {
        var limits=new LearningLimits(32,16,32,1024,1024,8,900000);
        var limited=new LearningStore(dir.resolve("classroom.db"),id->true,limits,clock);var a=published();
        var accepted=new ArrayList<Submission>();boolean saturated=false;
        for(int i=0;i<30;i++) {
            var content=files.write(snapshot());
            try{accepted.add(limited.submit(student,a.id(),1,membership,UUID.randomUUID(),content));}
            catch(LearningException e){assertEquals(STORAGE_LIMIT,e.code());saturated=true;break;}
        }
        assertTrue(saturated);assertFalse(accepted.isEmpty());
        var orphan=files.write(snapshot());var unfinished=dir.resolve("submissions").resolve(UUID.randomUUID()+".tmp");Files.writeString(unfinished,"partial");
        var unrelated=dir.resolve("submissions/readme.txt");Files.writeString(unrelated,"keep");
        files.recover(store.contentIds());assertFalse(Files.exists(unfinished));assertTrue(Files.exists(unrelated));
        assertThrows(LearningException.class,()->files.read(orphan.id(),orphan.checksum()));
        for(var s:accepted)assertEquals(snapshot(),files.read(s.content(),s.checksum()));
        assertEquals(accepted.size(),store.submissions(student,a.id(),student,0).items().size());
    }
    @Test void failedContentWriteDoesNotCreateMetadataAndClosedClassroomRejectsAStagedCapture() throws Exception {
        var a=published();var staged=files.write(snapshot());var badRoot=dir.resolve("not-a-directory");Files.writeString(badRoot,"blocked");
        assertThrows(LearningException.class,()->new SubmissionFiles(badRoot,LearningLimits.DEFAULT).write(snapshot()));
        assertTrue(store.submissions(student,a.id(),student,0).items().isEmpty());
        classes.disband(teacher,classroom.id(),classroom.revision());
        assertEquals(FORBIDDEN,assertThrows(ClassroomException.class,()->store.submit(student,a.id(),1,membership,UUID.randomUUID(),staged)).reason());
    }
    @Test void cancelledSelectionCannotCommitAndRepeatedRequestIsIdempotent() {
        var a=published();var content=files.write(snapshot());var request=UUID.randomUUID();
        assertEquals(NO_SELECTION,assertThrows(LearningException.class,()->store.submit(student,a.id(),1,membership,request,content,()->false)).code());
        assertTrue(store.submissions(student,a.id(),student,0).items().isEmpty());
        var first=store.submit(student,a.id(),1,membership,request,content);
        assertEquals(first,store.submit(student,a.id(),1,membership,request,content));
        assertEquals(1,store.submissions(student,a.id(),student,0).items().size());
    }
}
