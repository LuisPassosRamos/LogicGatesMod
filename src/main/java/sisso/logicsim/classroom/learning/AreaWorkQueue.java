package sisso.logicsim.classroom.learning;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static sisso.logicsim.classroom.learning.LearningException.Code.BUSY;

/** Bounded heavy work, separate from the metadata queue; no waiting on a world thread. */
public final class AreaWorkQueue implements AutoCloseable {
    private final ThreadPoolExecutor executor;
    private final Map<UUID,CompletableFuture<?>> actors=new ConcurrentHashMap<>();
    private final Set<CompletableFuture<?>> pending=ConcurrentHashMap.newKeySet();
    public AreaWorkQueue(int capacity) {
        executor=new ThreadPoolExecutor(1,1,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(capacity),
                Thread.ofPlatform().daemon(true).name("LogicGates-Area-",0).factory());
    }
    public <T> CompletableFuture<T> submit(UUID actor,Supplier<T> work) {
        var result=new CompletableFuture<T>();
        if(actors.putIfAbsent(actor,result)!=null)return CompletableFuture.failedFuture(new LearningException(BUSY));
        pending.add(result);
        result.whenComplete((v,e)->{actors.remove(actor,result);pending.remove(result);});
        try { executor.execute(()->{
            if(result.isDone())return;
            try{T value=work.get();actors.remove(actor,result);result.complete(value);}
            catch(Throwable e){actors.remove(actor,result);result.completeExceptionally(e);}
        }); }
        catch(RejectedExecutionException e) { result.completeExceptionally(new LearningException(BUSY,e)); }
        return result;
    }
    @Override public void close() {
        executor.shutdownNow(); pending.forEach(f->f.completeExceptionally(new LearningException(BUSY))); actors.clear();
    }
}
