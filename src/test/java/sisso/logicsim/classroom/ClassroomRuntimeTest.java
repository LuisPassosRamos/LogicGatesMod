package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomRuntimeTest {
    @TempDir Path dir;
    @Test void learningInvalidationsDoNotNotifyAdministrativeOrAuthorizationListenersAndForgetUnsubscribes() throws Exception {
        UUID actor=UUID.randomUUID(),classroom=UUID.randomUUID();var admin=new CopyOnWriteArrayList<ClassroomRuntime.Change>();var learning=new CopyOnWriteArrayList<UUID>();var authorization=new CopyOnWriteArrayList<Boolean>();
        try(var runtime=new ClassroomRuntime(dir,id->true,failure->{throw new AssertionError(failure);});
            var data=runtime.listen(actor,admin::add);var study=runtime.listenLearning(actor,learning::add);var auth=runtime.listenAuthorization(actor,authorization::add)) {
            runtime.learningChanged(classroom);assertEquals(List.of(classroom),learning);assertTrue(admin.isEmpty());assertTrue(authorization.isEmpty());
            runtime.forget(actor);runtime.learningChanged(classroom);assertEquals(1,learning.size());
        }
    }
    @Test void notificationsFollowCommitFailuresDoNotNotifyAndDisconnectRemovesOnlyOwnedListeners() throws Exception {
        UUID teacher=UUID.randomUUID(), student=UUID.randomUUID();
        var owned=new CopyOnWriteArrayList<ClassroomRuntime.Change>(); var global=new CopyOnWriteArrayList<ClassroomRuntime.Change>();
        var failures=new CopyOnWriteArrayList<Throwable>();
        try(var runtime=new ClassroomRuntime(dir,id->true,failures::add)) {
            var first=runtime.listen(teacher,change->{
                try(var c=DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("classroom.db"));var s=c.createStatement();var rows=s.executeQuery("SELECT count(*) FROM classrooms")) {
                    assertEquals(1,rows.getInt(1)); // The notification can observe committed data on another connection.
                } catch(Exception e) { throw new AssertionError(e); }
                owned.add(change);
            });
            var second=runtime.listen(null,global::add);
            var classroom=runtime.create(teacher,"A","").get(5,TimeUnit.SECONDS);
            assertEquals(1,owned.size()); assertEquals(classroom.id(),owned.getFirst().classroom());
            try(var c=DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("classroom.db"));var s=c.createStatement()) {
                s.execute("CREATE TRIGGER fail_request BEFORE INSERT ON classroom_intakes BEGIN SELECT RAISE(FAIL,'injected'); END");
            }
            assertThrows(ExecutionException.class,()->runtime.request(student,classroom.id(),"00123").get(5,TimeUnit.SECONDS));
            assertEquals(1,global.size()); assertEquals(1,failures.size());
            runtime.forget(teacher);
            runtime.update(teacher,classroom.id(),0,"B","").get(5,TimeUnit.SECONDS);
            assertEquals(1,owned.size()); assertEquals(2,global.size());
            first.close(); second.close();
            runtime.update(teacher,classroom.id(),1,"C","").get(5,TimeUnit.SECONDS);
            assertEquals(2,global.size());
        }
    }
    @Test void settingsAreOneRevisionAndInvalidMessageDoesNotPartiallySaveName() throws Exception {
        UUID teacher=UUID.randomUUID();
        try(var runtime=new ClassroomRuntime(dir,id->true,failure->fail(failure))) {
            var c=runtime.create(teacher,"A","Old").get(5,TimeUnit.SECONDS);
            assertThrows(ExecutionException.class,()->runtime.settings(teacher,c.id(),0,"B","New","x".repeat(2049)).get(5,TimeUnit.SECONDS));
            assertEquals(c,runtime.own(teacher).get(5,TimeUnit.SECONDS).orElseThrow());
            var saved=runtime.settings(teacher,c.id(),0,"B","New","Hello").get(5,TimeUnit.SECONDS);
            assertEquals(1,saved.revision()); assertEquals("B",saved.name()); assertEquals("Hello",saved.message());
        }
    }
}
