package sisso.logicsim.classroom.ui;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/** One queued world task per notification channel; authorization generations are recorded separately. */
final class ClassroomPageRefresh {
    private final AtomicBoolean queued=new AtomicBoolean();
    void schedule(Consumer<Runnable> executor,Runnable work) {
        if(!queued.compareAndSet(false,true)) return;
        try { executor.accept(()->{ queued.set(false); work.run(); }); }
        catch(RuntimeException failure) { queued.set(false); throw failure; }
    }
}
