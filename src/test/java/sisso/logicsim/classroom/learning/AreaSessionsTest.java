package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.classroom.learning.AreaSessions.*;

class AreaSessionsTest {
    private static final LearningLimits LIMITS=LearningLimits.DEFAULT;
    @Test void selectionBelongsToConnectionWorldAndActivityAndCancelInvalidatesIt() {
        var sessions=new AreaSessions();var actor=UUID.randomUUID();var world=UUID.randomUUID();var connection=new Object();var activity=UUID.randomUUID();
        var session=sessions.begin(actor,world,connection,activity,UUID.randomUUID(),3,null,LIMITS);
        session.first=new Point(31,96,-1);session.second=new Point(0,110,-32);
        assertEquals(new Bounds(0,96,-32,32,16,32),sessions.bounds(session,LIMITS));assertEquals(activity,session.activity);assertEquals(3,session.publication);
        assertSame(session,sessions.current(actor,world,connection));assertNull(sessions.current(actor,world,new Object()));assertFalse(sessions.active(session));
        var next=sessions.begin(actor,world,connection,activity,UUID.randomUUID(),3,null,LIMITS);
        assertNull(sessions.current(actor,UUID.randomUUID(),connection));assertFalse(sessions.active(next));
        var last=sessions.begin(actor,world,connection,activity,UUID.randomUUID(),3,null,LIMITS);sessions.cancel(actor);assertFalse(sessions.active(last));
    }
    @Test void expiredSelectionCannotBeCapturedAndCoordinateArithmeticCannotOverflow() {
        var now=Clock.fixed(Instant.EPOCH,ZoneOffset.UTC);var sessions=new AreaSessions(Clock.offset(now,Duration.ofMinutes(16)));
        var expired=new Session(UUID.randomUUID(),UUID.randomUUID(),new Object(),UUID.randomUUID(),UUID.randomUUID(),1,null,15*60000);
        assertEquals(LearningException.Code.SELECTION_EXPIRED,assertThrows(LearningException.class,()->sessions.bounds(expired,LIMITS)).code());
        assertThrows(LearningException.class,()->Bounds.between(new Point(Integer.MIN_VALUE,0,0),new Point(Integer.MAX_VALUE,0,0),LIMITS));
        assertThrows(LearningException.class,()->Bounds.between(new Point(0,0,0),new Point(0,16,0),LIMITS));
    }
    @Test void floorCornersIncludeTheCircuitLayerInEitherClickOrderAndUnevenGround() {
        var sessions=new AreaSessions();
        var session=sessions.begin(UUID.randomUUID(),UUID.randomUUID(),new Object(),UUID.randomUUID(),UUID.randomUUID(),1,null,LIMITS);
        session.first=new Point(-3,96,-8);session.second=new Point(4,96,-2);
        assertEquals(new Bounds(-3,96,-8,8,2,7),sessions.bounds(session,LIMITS));
        var first=session.first;session.first=session.second;session.second=first;
        assertEquals(new Bounds(-3,96,-8,8,2,7),sessions.bounds(session,LIMITS));
        session.second=new Point(-3,98,-8);
        assertEquals(new Bounds(-3,96,-8,8,4,7),sessions.bounds(session,LIMITS));
        session.first=session.second;
        assertEquals(new Bounds(-3,98,-8,1,2,1),sessions.bounds(session,LIMITS));
    }
    @Test void extraLayerCountsTowardHeightAndCannotOverflowOrBeSilentlyClamped() {
        var sessions=new AreaSessions();
        var session=sessions.begin(UUID.randomUUID(),UUID.randomUUID(),new Object(),UUID.randomUUID(),UUID.randomUUID(),1,null,LIMITS);
        session.first=new Point(0,0,0);session.second=new Point(31,14,31);
        assertEquals(new Bounds(0,0,0,32,16,32),sessions.bounds(session,LIMITS));
        session.second=new Point(31,15,31);
        assertEquals(LearningException.Code.AREA_LIMIT,assertThrows(LearningException.class,()->sessions.bounds(session,LIMITS)).code());
        session.first=new Point(0,Integer.MAX_VALUE,0);session.second=session.first;
        assertEquals(LearningException.Code.AREA_LIMIT,assertThrows(LearningException.class,()->sessions.bounds(session,LIMITS)).code());
    }
    @Test void exactBoundsAndTeacherPasteDimensionsDoNotGainAnotherLayer() {
        var point=new Point(1,96,2);
        assertEquals(new Bounds(1,96,2,1,1,1),Bounds.between(point,point,LIMITS));
        var sessions=new AreaSessions();
        var paste=sessions.begin(UUID.randomUUID(),UUID.randomUUID(),new Object(),UUID.randomUUID(),UUID.randomUUID(),1,UUID.randomUUID(),LIMITS);
        paste.first=point;paste.second=new Point(3,97,5);
        assertEquals(new Bounds(1,96,2,3,2,4),sessions.bounds(paste,LIMITS));
    }
}
