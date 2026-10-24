package sisso.logicsim.classroom.ui;

import java.util.concurrent.atomic.AtomicReference;

/** Invalidations arrive on permission threads; only the world thread consumes revocations. */
final class ClassroomPageAccess {
    record Stamp(long generation, long revocations) { }
    private final AtomicReference<Stamp> stamp = new AtomicReference<>(new Stamp(0, 0));
    private long consumedRevocations;
    Stamp current() { return stamp.get(); }
    void changed(boolean canTeach) {
        stamp.updateAndGet(old -> new Stamp(old.generation()+1, old.revocations()+(canTeach ? 0 : 1)));
    }
    boolean current(Stamp request) { return stamp.get() == request; }
    boolean consumeRevocation() {
        long revocations = stamp.get().revocations();
        if (revocations == consumedRevocations) return false;
        consumedRevocations = revocations;
        return true;
    }
}
