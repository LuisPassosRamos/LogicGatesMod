package sisso.logicsim.classroom;

import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** One server-wide I/O queue, shared by every world. Initialization failures are isolated. */
public final class ClassroomRuntime implements AutoCloseable {
    public static final String TEACH_PERMISSION = "sisso.logicgates.classroom.teach";
    private final ExecutorService executor = Executors.newSingleThreadExecutor(
            Thread.ofPlatform().daemon(true).name("LogicGates-Classroom-", 0).factory());
    private final Consumer<Throwable> log;
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    // Accessed only on the executor. Initialization is enqueued before all operations.
    private ClassroomService service;

    public ClassroomRuntime(Path dataDirectory, Predicate<UUID> canTeach, Consumer<Throwable> log) {
        this.log = log;
        executor.execute(() -> {
            try { service = new ClassroomService(new SqliteClassroomRepository(dataDirectory.resolve("classroom.db")), canTeach); }
            catch (Exception failure) { log.accept(failure); }
        });
    }

    public CompletableFuture<Optional<Classroom>> own(UUID actor) { return submit(service -> service.own(actor)); }
    public CompletableFuture<Classroom> create(UUID actor, String name, String description) {
        return submit(service -> service.create(actor, name, description));
    }
    public CompletableFuture<Classroom> update(UUID actor, UUID id, long revision, String name, String description) {
        return submit(service -> service.update(actor, id, revision, name, description));
    }

    private synchronized <T> CompletableFuture<T> submit(Function<ClassroomService, T> operation) {
        try {
            var future = CompletableFuture.supplyAsync(() -> {
                if (service == null) throw new ClassroomException(ClassroomException.Reason.UNAVAILABLE);
                try { return operation.apply(service); }
                catch (ClassroomException failure) {
                    if (failure.reason() == ClassroomException.Reason.UNAVAILABLE) log.accept(failure);
                    throw failure;
                }
            }, executor);
            pending.add(future);
            future.whenComplete((value, failure) -> pending.remove(future));
            return future;
        } catch (RejectedExecutionException closed) {
            return CompletableFuture.failedFuture(new ClassroomException(ClassroomException.Reason.UNAVAILABLE, closed));
        }
    }

    @Override public void close() {
        synchronized (this) { executor.shutdown(); }
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                log.accept(new IllegalStateException("Classroom I/O did not stop within five seconds"));
            }
        } catch (InterruptedException interrupted) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            if (!executor.isTerminated()) pending.forEach(future -> future.completeExceptionally(
                    new ClassroomException(ClassroomException.Reason.UNAVAILABLE)));
        }
    }
}
