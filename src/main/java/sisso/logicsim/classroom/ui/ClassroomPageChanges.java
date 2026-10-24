package sisso.logicsim.classroom.ui;

import sisso.logicsim.classroom.ClassroomRuntime.Change;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/** Keep a bounded batch while I/O is pending; test it against the resulting page context. */
final class ClassroomPageChanges {
    private static final int LIMIT=128;
    private final Set<Change> changes=ConcurrentHashMap.newKeySet();
    private final AtomicBoolean overflow=new AtomicBoolean();
    void add(Change change) {
        if(changes.size()>=LIMIT) overflow.set(true);
        else changes.add(change);
    }
    boolean drain(Predicate<Change> interested) {
        boolean relevant=overflow.getAndSet(false);
        for(var change:changes) if(changes.remove(change)) relevant=interested.test(change) || relevant;
        return relevant;
    }
    void clear() { changes.clear(); overflow.set(false); }
}
