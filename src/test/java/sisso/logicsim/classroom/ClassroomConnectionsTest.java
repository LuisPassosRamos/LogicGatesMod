package sisso.logicsim.classroom;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomConnectionsTest {
    @Test void worldTransitionsDoNotRepeatReadyButReconnectDoes() {
        var connections=new ClassroomConnections(); UUID player=UUID.randomUUID(); Object first=new Object(), next=new Object();
        var session=connections.ready(player,first); assertNotNull(session);
        assertNull(connections.ready(player,first));
        assertTrue(connections.disconnect(player,first));
        var reconnected=connections.ready(player,next); assertNotNull(reconnected);
        assertFalse(connections.current(session,0)); assertTrue(connections.current(reconnected,0));
        assertFalse(connections.disconnect(player,first)); assertTrue(connections.current(reconnected,0));
    }
    @Test void membershipOrClassChangesInvalidateQueuedMessagesButUnrelatedChangesDoNot() {
        var connections=new ClassroomConnections(); UUID player=UUID.randomUUID(), classroom=UUID.randomUUID();
        var session=connections.ready(player,new Object()); session.classroom=classroom;
        connections.changed(new ClassroomRuntime.Change(UUID.randomUUID(),UUID.randomUUID(),false));
        assertTrue(connections.current(session,0));
        connections.changed(new ClassroomRuntime.Change(classroom,UUID.randomUUID(),false));
        assertFalse(connections.current(session,0)); assertTrue(connections.current(session,1));
        connections.changed(new ClassroomRuntime.Change(null,player,false)); assertFalse(connections.current(session,1));
        connections.clear(); assertFalse(connections.current(session,2));
    }
}
