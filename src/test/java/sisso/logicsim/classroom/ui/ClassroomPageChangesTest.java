package sisso.logicsim.classroom.ui;

import org.junit.jupiter.api.Test;
import sisso.logicsim.classroom.ClassroomRuntime.Change;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomPageChangesTest {
    @Test void unrelatedChangesDuringIoDoNotForceAnotherQueryButANewlyRelevantContextDoes() {
        var changes=new ClassroomPageChanges(); UUID first=UUID.randomUUID(), next=UUID.randomUUID();
        changes.add(new Change(first,UUID.randomUUID(),false));
        assertFalse(changes.drain(change->next.equals(change.classroom())));
        changes.add(new Change(next,UUID.randomUUID(),false));
        assertTrue(changes.drain(change->next.equals(change.classroom())));
        assertFalse(changes.drain(change->true));
    }
    @Test void anOverflowRequestsOneConservativeRefreshAndClearingDropsOnlyPreQueryChanges() {
        var changes=new ClassroomPageChanges();
        for(int i=0;i<1000;i++) changes.add(new Change(UUID.randomUUID(),UUID.randomUUID(),false));
        assertTrue(changes.drain(change->false)); assertFalse(changes.drain(change->false));
        changes.add(new Change(null,UUID.randomUUID(),true)); changes.clear(); assertFalse(changes.drain(change->true));
    }
}
