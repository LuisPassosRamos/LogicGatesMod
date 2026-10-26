package sisso.logicsim.classroom.domain;

import sisso.logicsim.classroom.domain.ClassroomViews.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

/** Pure read policy. Teaching authority comes from the service's native permission predicate, never a UI field. */
public final class ClassroomReadPolicy {
    private ClassroomReadPolicy() { }
    public static Home authorize(Home home) {
        if (teacher(home) && !home.canTeach()) throw new ClassroomException(FORBIDDEN);
        return home;
    }
    public static boolean teacher(Home home) { return home.member()!=null && home.member().role()==Role.TEACHER; }
    public static Kind kind(Home home,Kind requested) {
        authorize(home);
        if (teacher(home)) {
            if (!home.classroom().teacherId().equals(home.member().playerId())) throw new ClassroomException(FORBIDDEN);
            return requested==Kind.BROWSE || requested==Kind.DETAIL ? Kind.MEMBERS : requested;
        }
        if (home.member()!=null) return Kind.DETAIL;
        return requested==Kind.BROWSE || requested==Kind.INTAKES || requested==Kind.SETTINGS && home.canTeach() ? requested : Kind.BROWSE;
    }
    public static int page(int page) { if (page<0 || page>100000) throw new ClassroomException(NOT_FOUND); return page; }
    public static String query(String query) { return query==null ? "" : query.strip().substring(0,Math.min(query.strip().length(),128)); }
}
