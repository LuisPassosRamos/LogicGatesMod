package sisso.logicsim.classroom;

import com.hypixel.hytale.server.core.event.events.permissions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomAuthorizationTest {
    @TempDir Path dir;
    @Test void nativeEventsRecheckEffectivePermissionAndNeverPublishDataOrMotdChanges() throws Exception {
        UUID actor=UUID.randomUUID(), other=UUID.randomUUID(); var allowed=new AtomicBoolean(true);
        var a=new ArrayList<Boolean>(); var secondPage=new ArrayList<Boolean>(); var b=new ArrayList<Boolean>();
        var data=new ArrayList<ClassroomRuntime.Change>();
        try(var runtime=new ClassroomRuntime(dir,id->allowed.get(),failure->fail(failure))) {
            runtime.listen(null,data::add); // Presence receives this channel; authorization must not reach it.
            var first=runtime.listenAuthorization(actor,a::add);
            runtime.listenAuthorization(actor,secondPage::add);
            runtime.listenAuthorization(other,b::add);
            var adapter=new ClassroomPermissions(runtime);
            adapter.playerChanged(new PlayerPermissionChangeEvent.PermissionsAdded(actor,Set.of("*")));
            allowed.set(false);
            adapter.playerChanged(new PlayerPermissionChangeEvent.PermissionsRemoved(actor,Set.of("*")));
            adapter.playerChanged(new PlayerGroupEvent.Removed(actor,"hytale:Admin"));
            allowed.set(true);
            adapter.playerChanged(new PlayerGroupEvent.Added(actor,"hytale:Admin"));
            assertEquals(List.of(true,false,false,true),a); assertEquals(a,secondPage); assertTrue(b.isEmpty());
            adapter.groupChanged(new GroupPermissionChangeEvent.Added("Teachers",Set.of(ClassroomRuntime.TEACH_PERMISSION)));
            allowed.set(false);
            adapter.groupChanged(new GroupPermissionChangeEvent.Removed("Teachers",Set.of(ClassroomRuntime.TEACH_PERMISSION)));
            assertEquals(List.of(true,false),b); assertEquals(6,a.size()); assertEquals(a,secondPage);
            assertTrue(data.isEmpty());
            first.close(); runtime.forget(actor);
            runtime.authorizationChanged(actor); assertEquals(6,a.size()); assertEquals(6,secondPage.size());
            runtime.authorizationChangedForOpenPages(); assertEquals(3,b.size());
        }
    }
    @Test void callbacksAreRemovedOnCloseAndOneBrokenSubscriberDoesNotPreventOthers() {
        UUID actor=UUID.randomUUID(); var values=new ArrayList<Boolean>(); var failures=new ArrayList<Throwable>();
        var runtime=new ClassroomRuntime(dir,id->true,failures::add);
        runtime.listenAuthorization(actor,value->{ throw new IllegalStateException("injected subscriber failure"); });
        runtime.listenAuthorization(actor,values::add);
        runtime.authorizationChanged(actor);
        assertEquals(List.of(true),values); assertEquals(1,failures.size());
        runtime.close(); runtime.authorizationChangedForOpenPages(); runtime.authorizationChanged(actor);
        runtime.listenAuthorization(actor,values::add); runtime.authorizationChanged(actor);
        assertEquals(List.of(true),values);
    }
}
