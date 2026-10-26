package sisso.logicsim.classroom;

import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import sisso.logicsim.classroom.learning.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** One server-wide I/O queue, shared by every world. Initialization failures are isolated. */
public final class ClassroomRuntime implements AutoCloseable {
    public static final String TEACH_PERMISSION = "sisso.logicgates.classroom.teach";
    private final ExecutorService executor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
            new ArrayBlockingQueue<>(256), Thread.ofPlatform().daemon(true).name("LogicGates-Classroom-", 0).factory());
    private final Consumer<Throwable> log;
    private final Predicate<UUID> canTeach;
    private volatile boolean closed;
    private final Set<CompletableFuture<?>> pending = ConcurrentHashMap.newKeySet();
    // Accessed only on the executor. Initialization is enqueued before all operations.
    private ClassroomService service;
    private LearningStore learning;
    private volatile LearningLimits learningLimits=LearningLimits.DEFAULT;
    private SubmissionFiles submissionFiles;
    public LearningLimits learningLimits() { return learningLimits; }
    public boolean teaching(UUID actor) { return canTeach.test(actor); }
    public SubmissionFiles submissionFiles() { return submissionFiles; }
    public <T> CompletableFuture<T> learn(Function<LearningStore,T> operation) { return submit(ignored->operation.apply(learning)); }
    private record LearningListener(UUID owner,boolean catalogOnly,Predicate<UUID> interested,Consumer<UUID> callback) { }
    private final java.util.Map<UUID,LearningListener> learningListeners=new ConcurrentHashMap<>();
    public AutoCloseable listenLearning(UUID owner,Consumer<UUID> callback) {
        return listenLearning(owner,false,id->true,callback);
    }
    public AutoCloseable listenActivityCatalog(UUID owner,Predicate<UUID> interested,Consumer<UUID> callback) {
        return listenLearning(owner,true,interested,callback);
    }
    private AutoCloseable listenLearning(UUID owner,boolean catalogOnly,Predicate<UUID> interested,Consumer<UUID> callback) {
        java.util.Objects.requireNonNull(owner);java.util.Objects.requireNonNull(callback);UUID token=UUID.randomUUID();
        java.util.Objects.requireNonNull(interested);
        if(!closed)learningListeners.put(token,new LearningListener(owner,catalogOnly,interested,callback));
        if(closed)learningListeners.remove(token);
        return ()->learningListeners.remove(token);
    }
    /** Learning metadata does not invalidate administrative screens, MOTD delivery or review authorization. */
    public void learningChanged(UUID classroom) {
        learningChanged(classroom,false);
    }
    /** Committed catalog changes also invalidate role-filtered counts, without touching presence/MOTD. */
    public void activitiesChanged(UUID classroom) { learningChanged(classroom,true); }
    private void learningChanged(UUID classroom,boolean catalog) {
        if(closed)return;
        learningListeners.values().forEach(listener->{try{
            if((catalog || !listener.catalogOnly()) && listener.interested().test(classroom))listener.callback().accept(classroom);
        }catch(RuntimeException failure){log.accept(failure);}});
    }
    public record Change(UUID classroom, UUID player, boolean directory) { }
    private record Listener(UUID owner, Predicate<Change> interested, Consumer<Change> callback) { }
    private final java.util.Map<UUID, Listener> listeners = new ConcurrentHashMap<>();
    private static final class AuthorizationListeners {
        final java.util.Map<UUID, Consumer<Boolean>> callbacks = new ConcurrentHashMap<>();
        Boolean allowed;
    }
    private final java.util.Map<UUID, AuthorizationListeners> authorizationListeners = new ConcurrentHashMap<>();
    public AutoCloseable listenAuthorization(UUID owner, Consumer<Boolean> listener) {
        java.util.Objects.requireNonNull(owner);
        UUID token = UUID.randomUUID();
        java.util.Objects.requireNonNull(listener);
        authorizationListeners.compute(owner,(id,group)->{
            if (closed) return group;
            if (group==null) group=new AuthorizationListeners();
            group.callbacks.put(token,listener); return group;
        });
        return () -> authorizationListeners.computeIfPresent(owner,(id,group)->{
            group.callbacks.remove(token); return group.callbacks.isEmpty() ? null : group;
        });
    }
    /** Native permission events are separate from committed data changes and never resend the MOTD. */
    public void authorizationChanged(UUID player) {
        var group=authorizationListeners.get(player);
        if (group!=null) authorizationChanged(player,group);
    }
    private void authorizationChanged(UUID player,AuthorizationListeners group) {
        boolean allowed;
        java.util.List<Consumer<Boolean>> callbacks;
        synchronized(group) {
            if (closed || group.callbacks.isEmpty()) return;
            allowed=canTeach.test(player);
            if (group.allowed!=null && group.allowed==allowed) return;
            group.allowed=allowed;
            callbacks=java.util.List.copyOf(group.callbacks.values());
        }
        for(var callback:callbacks) {
            try { callback.accept(allowed); } catch (RuntimeException failure) { log.accept(failure); }
        }
    }
    public void authorizationChangedForOpenPages() {
        authorizationListeners.forEach(this::authorizationChanged);
    }
    /** A token belongs to one page, not to the player's name or current list position. */
    public AutoCloseable listen(UUID owner, Consumer<Change> listener) {
        return listen(owner,change->true,listener);
    }
    /** Filter immutable page context before scheduling work on a world. Presence keeps the unfiltered channel. */
    public AutoCloseable listen(UUID owner, Predicate<Change> interested, Consumer<Change> listener) {
        java.util.Objects.requireNonNull(interested); java.util.Objects.requireNonNull(listener);
        UUID token = UUID.randomUUID();
        if (!closed) listeners.put(token, new Listener(owner, interested, listener));
        if (closed) listeners.remove(token);
        return () -> listeners.remove(token);
    }
    public void forget(UUID player) {
        listeners.values().removeIf(listener -> player.equals(listener.owner()));
        learningListeners.values().removeIf(listener->player.equals(listener.owner()));
        authorizationListeners.remove(player);
    }
    private void changed(Change change) {
        listeners.values().forEach(listener -> {
            try { if(listener.interested().test(change)) listener.callback().accept(change); }
            catch (RuntimeException failure) { log.accept(failure); }
        });
    }

    public ClassroomRuntime(Path dataDirectory, Predicate<UUID> canTeach, Consumer<Throwable> log) {
        this.log = log; this.canTeach = canTeach;
        executor.execute(() -> {
            try {
                var repository=new SqliteClassroomRepository(dataDirectory.resolve("classroom.db"));
                learningLimits=LearningLimits.load(dataDirectory.resolve("learning.properties"));
                learning=new LearningStore(dataDirectory.resolve("classroom.db"),canTeach,learningLimits);
                submissionFiles=new SubmissionFiles(dataDirectory.resolve("submissions"),learningLimits);
                service = new ClassroomService(repository, canTeach);
            }
            catch (Exception failure) { log.accept(failure); }
        });
    }

    public CompletableFuture<Optional<Classroom>> own(UUID actor) { return submit(service -> service.own(actor)); }
    public CompletableFuture<Classroom> create(UUID actor, String name, String description) {
        return mutation(service -> service.create(actor, name, description), value -> new Change(value.id(), actor, true));
    }
    public CompletableFuture<Classroom> update(UUID actor, UUID id, long revision, String name, String description) {
        return mutation(service -> service.update(actor, id, revision, name, description), value -> new Change(value.id(), actor, true));
    }
    public CompletableFuture<Classroom> disband(UUID actor, UUID classroom, long revision) {
        return mutation(service -> service.disband(actor, classroom, revision), value -> new Change(value.id(), actor, true));
    }
    public CompletableFuture<Home> home(UUID actor) { return submit(service -> service.home(actor)); }
    public CompletableFuture<Screen> screen(UUID actor, Kind kind, String query, int page) { return submit(service -> service.screen(actor, kind, query, page)); }
    public CompletableFuture<Void> remember(UUID actor, String name) {
        return mutation(service -> { service.remember(actor, name); return null; }, value -> new Change(null, actor, true));
    }
    public CompletableFuture<Page<Summary>> browse(UUID actor, String query, int page) { return submit(service -> service.browse(actor, query, page)); }
    public CompletableFuture<Page<Profile>> players(UUID actor, String query, int page) { return submit(service -> service.players(actor, query, page)); }
    public CompletableFuture<Page<Member>> members(UUID actor, int page) { return submit(service -> service.members(actor, page)); }
    public CompletableFuture<Page<Intake>> intakes(UUID actor, boolean teacher, int page) { return submit(service -> service.intakes(actor, teacher, page)); }
    public CompletableFuture<Intake> request(UUID actor, UUID classroom, String enrollment) {
        return mutation(service -> service.request(actor, classroom, enrollment), ClassroomRuntime::change);
    }
    public CompletableFuture<Intake> invite(UUID actor, UUID target) {
        return mutation(service -> service.invite(actor, target), ClassroomRuntime::change);
    }
    public CompletableFuture<Intake> accept(UUID actor, UUID intake, long revision, String enrollment) {
        return mutation(service -> service.accept(actor, intake, revision, enrollment), ClassroomRuntime::change);
    }
    public CompletableFuture<Intake> closeIntake(UUID actor, UUID intake, long revision, boolean teacher) {
        return mutation(service -> service.closeIntake(actor, intake, revision, teacher), ClassroomRuntime::change);
    }
    public CompletableFuture<Member> approve(UUID actor, UUID intake, long revision, String enrollment) {
        return mutation(service -> service.approve(actor, intake, revision, enrollment), ClassroomRuntime::change);
    }
    public CompletableFuture<Member> correct(UUID actor, UUID membership, long revision, String enrollment) {
        return mutation(service -> service.correct(actor, membership, revision, enrollment), ClassroomRuntime::change);
    }
    public CompletableFuture<Member> end(UUID actor, UUID membership, long revision, boolean teacher) {
        return mutation(service -> service.end(actor, membership, revision, teacher), ClassroomRuntime::change);
    }
    public CompletableFuture<Classroom> message(UUID actor, UUID classroom, long revision, String message) {
        return mutation(service -> service.message(actor, classroom, revision, message), value -> new Change(value.id(), actor, false));
    }
    public CompletableFuture<Classroom> settings(UUID actor, UUID classroom, long revision, String name, String description, String message) {
        return mutation(service -> service.settings(actor, classroom, revision, name, description, message), value -> new Change(value.id(), actor, true));
    }
    private static Change change(Intake i) { return new Change(i.classroomId(), i.studentId(), false); }
    private static Change change(Member m) { return new Change(m.classroomId(), m.playerId(), true); }
    private <T> CompletableFuture<T> mutation(Function<ClassroomService,T> operation, Function<T,Change> change) {
        return submit(service -> { T value = operation.apply(service); changed(change.apply(value)); return value; });
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
        closed = true; listeners.clear(); learningListeners.clear(); authorizationListeners.clear();
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
