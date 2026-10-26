package sisso.logicsim.classroom.learning;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.time.Clock;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;

/** In-memory selection authority belongs to a connection, world and activity, never just an item. */
public final class AreaSessions {
    public record Point(int x,int y,int z) { }
    public record Bounds(int x,int y,int z,int width,int height,int depth) {
        public Bounds {
            if(width<1 || height<1 || depth<1 || width>64 || height>64 || depth>64
                    || (long)x+width-1>Integer.MAX_VALUE || (long)y+height-1>Integer.MAX_VALUE || (long)z+depth-1>Integer.MAX_VALUE)throw new LearningException(AREA_LIMIT);
        }
        public static Bounds between(Point a,Point b,LearningLimits limits) {
            long w=Math.abs((long)a.x-b.x)+1,h=Math.abs((long)a.y-b.y)+1,d=Math.abs((long)a.z-b.z)+1;
            if(w>limits.width() || h>limits.height() || d>limits.depth()) throw new LearningException(AREA_LIMIT);
            return new Bounds(Math.min(a.x,b.x),Math.min(a.y,b.y),Math.min(a.z,b.z),(int)w,(int)h,(int)d);
        }
    }
    public static final class Session {
        public final UUID token=UUID.randomUUID(),actor,world,activity,membership,submission;
        public final Object connection;
        public final int publication;
        public final long expires;
        public volatile Point first,second;
        Session(UUID actor,UUID world,Object connection,UUID activity,UUID membership,int publication,UUID submission,long expires) {
            this.actor=actor;this.world=world;this.connection=connection;this.activity=activity;this.membership=membership;
            this.publication=publication;this.submission=submission;this.expires=expires;
        }
    }
    private final Map<UUID,Session> sessions=new ConcurrentHashMap<>();
    private final Clock clock;
    public AreaSessions() { this(Clock.systemUTC()); }
    public AreaSessions(Clock clock) { this.clock=clock; }
    public Session begin(UUID actor,UUID world,Object connection,UUID activity,UUID membership,int publication,UUID submission,LearningLimits limits) {
        var session=new Session(actor,world,connection,activity,membership,publication,submission,clock.millis()+limits.selectionMillis());
        sessions.put(actor,session);return session;
    }
    public Session current(UUID actor,UUID world,Object connection) {
        var s=sessions.get(actor); if(s==null) return null;
        if(!s.world.equals(world) || s.connection!=connection || clock.millis()>=s.expires) { sessions.remove(actor,s); return null; }
        return s;
    }
    public Session require(UUID actor,UUID world,Object connection) {
        var s=current(actor,world,connection);if(s==null)throw new LearningException(NO_SELECTION);return s;
    }
    public Bounds bounds(Session session,LearningLimits limits) {
        if(clock.millis()>=session.expires) throw new LearningException(SELECTION_EXPIRED);
        if(session.first==null || session.second==null) throw new LearningException(NO_SELECTION);
        return Bounds.between(session.first,session.second,limits);
    }
    public boolean active(Session session) { return sessions.get(session.actor)==session && clock.millis()<session.expires; }
    public void cancel(UUID actor) { sessions.remove(actor); }
    public boolean complete(Session session) { return sessions.remove(session.actor,session); }
    public void clear() { sessions.clear(); }
}
