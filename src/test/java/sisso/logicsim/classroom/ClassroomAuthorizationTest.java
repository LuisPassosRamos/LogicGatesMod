package sisso.logicsim.classroom;

import com.hypixel.hytale.server.core.event.events.permissions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
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
            assertEquals(List.of(true,false,true),a); assertEquals(a,secondPage); assertTrue(b.isEmpty());
            adapter.groupChanged(new GroupPermissionChangeEvent.Added("Teachers",Set.of(ClassroomRuntime.TEACH_PERMISSION)));
            allowed.set(false);
            adapter.groupChanged(new GroupPermissionChangeEvent.Removed("Teachers",Set.of(ClassroomRuntime.TEACH_PERMISSION)));
            assertEquals(List.of(true,false),b); assertEquals(4,a.size()); assertEquals(a,secondPage);
            assertTrue(data.isEmpty());
            first.close(); runtime.forget(actor);
            runtime.authorizationChanged(actor); assertEquals(4,a.size()); assertEquals(4,secondPage.size());
            runtime.authorizationChangedForOpenPages(); assertEquals(2,b.size());
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
    @Test void groupChangesCheckEachOpenOwnerOnceAndUnchangedPermissionDoesNotRefreshPages() throws Exception {
        var checks=new AtomicInteger(); var callbacks=new AtomicInteger(); var allowed=new AtomicBoolean(true);
        var subscriptions=new ArrayList<AutoCloseable>();
        try(var runtime=new ClassroomRuntime(dir,id->{checks.incrementAndGet();return allowed.get();},failure->fail(failure))) {
            for(int i=0;i<1000;i++) {
                UUID owner=UUID.randomUUID();
                subscriptions.add(runtime.listenAuthorization(owner,value->callbacks.incrementAndGet()));
                subscriptions.add(runtime.listenAuthorization(owner,value->callbacks.incrementAndGet()));
            }
            runtime.authorizationChangedForOpenPages(); assertEquals(1000,checks.get()); assertEquals(2000,callbacks.get());
            runtime.authorizationChangedForOpenPages(); assertEquals(2000,checks.get()); assertEquals(2000,callbacks.get());
            allowed.set(false); runtime.authorizationChangedForOpenPages();
            assertEquals(3000,checks.get()); assertEquals(4000,callbacks.get());
            runtime.authorizationChanged(UUID.randomUUID()); assertEquals(3000,checks.get());
            for(var subscription:subscriptions) subscription.close();
            runtime.authorizationChangedForOpenPages(); assertEquals(3000,checks.get());
        }
    }
    @Test void dataFiltersRunBeforeCallbacksAndPresenceStillReceivesAllCommittedChanges() throws Exception {
        UUID owner=UUID.randomUUID(), other=UUID.randomUUID(); var all=new AtomicInteger(); var own=new AtomicInteger();
        try(var runtime=new ClassroomRuntime(dir,id->true,failure->fail(failure))) {
            runtime.listen(null,change->all.incrementAndGet());
            var subscription=runtime.listen(owner,change->owner.equals(change.player()),change->own.incrementAndGet());
            runtime.remember(other,"Other").join(); assertEquals(1,all.get()); assertEquals(0,own.get());
            runtime.remember(owner,"Owner").join(); assertEquals(2,all.get()); assertEquals(1,own.get());
            subscription.close(); runtime.remember(owner,"Renamed").join(); assertEquals(3,all.get()); assertEquals(1,own.get());
        }
    }
}
