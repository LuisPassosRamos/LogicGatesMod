package sisso.logicsim.classroom.ui;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomPageRefreshTest {
    @Test void aBurstQueuesOneWorldTaskAndKeepsEveryAuthorizationRevocation() {
        var refresh=new ClassroomPageRefresh(); var access=new ClassroomPageAccess(); var world=new ArrayList<Runnable>();
        var applied=new AtomicInteger();
        for(int i=0;i<1000;i++) {
            access.changed(i%2==1);
            refresh.schedule(world::add,()->{ assertTrue(access.consumeRevocation()); applied.incrementAndGet(); });
        }
        assertEquals(1,world.size()); assertEquals(500,access.current().revocations()); world.removeFirst().run(); assertEquals(1,applied.get());
        refresh.schedule(world::add,applied::incrementAndGet); assertEquals(1,world.size()); world.removeFirst().run(); assertEquals(2,applied.get());
    }
    @Test void aStoppedWorldDoesNotLeaveTheCoalescerPermanentlyPending() {
        var refresh=new ClassroomPageRefresh(); var world=new ArrayList<Runnable>();
        assertThrows(IllegalStateException.class,()->refresh.schedule(task->{throw new IllegalStateException("stopped");},()->{}));
        refresh.schedule(world::add,()->{}); assertEquals(1,world.size());
    }
}
