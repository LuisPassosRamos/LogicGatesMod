package sisso.logicsim.classroom;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/** One ready notification per network connection, including transitions between worlds. */
final class ClassroomConnections {
    static final class Session {
        final UUID player;
        final Object connection;
        final AtomicBoolean ready = new AtomicBoolean();
        final AtomicLong revision = new AtomicLong();
        volatile UUID classroom;
        Session(UUID player,Object connection) { this.player=player; this.connection=connection; }
    }
    private final ConcurrentHashMap<UUID,Session> sessions=new ConcurrentHashMap<>();
    Session ready(UUID player,Object connection) {
        var session=sessions.compute(player,(id,old)->old!=null && old.connection==connection ? old : new Session(id,connection));
        return session.ready.compareAndSet(false,true) ? session : null;
    }
    boolean current(Session session,long revision) { return sessions.get(session.player)==session && session.revision.get()==revision; }
    boolean disconnect(UUID player,Object connection) {
        var session=sessions.get(player);
        return session!=null && session.connection==connection && sessions.remove(player,session);
    }
    void changed(ClassroomRuntime.Change change) {
        sessions.values().forEach(session->{
            if (session.player.equals(change.player()) || session.classroom!=null && session.classroom.equals(change.classroom())) session.revision.incrementAndGet();
        });
    }
    void clear() { sessions.clear(); }
}
