package sisso.logicsim.classroom.domain;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Immutable use-case results. Public listings cannot contain private student records. */
public final class ClassroomViews {
    private ClassroomViews() { }
    public enum Role { TEACHER, STUDENT }
    public enum Origin { REQUEST, INVITE }
    public enum State { INVITED, REQUESTED, APPROVED, REJECTED, DECLINED, CANCELLED, SUPERSEDED }
    public enum Kind { MEMBERS, BROWSE, INTAKES, PLAYERS, SETTINGS, DETAIL }
    public record Profile(UUID id, String name) { }
    public record Summary(UUID id, String name, String description, UUID teacherId, String teacherName) { }
    public record Member(UUID id, UUID classroomId, UUID playerId, String name, Role role,
                         String enrollment, boolean active, long joinedAt, Long endedAt, long revision) { }
    public record Intake(UUID id, UUID classroomId, String classroomName, UUID studentId, String studentName,
                         Origin origin, State state, String enrollment, long revision, long createdAt) { }
    public record Home(Classroom classroom, Member member, String teacherName, boolean canTeach) { }
    public record Progress(int delivered, int total) {
        public int pending() { return Math.max(0, total-delivered); }
    }
    public record Overview(int published, int drafts, int awaitingAssessment, int delivered) {
        public static final Overview EMPTY = new Overview(0,0,0,0);
    }
    /** Metadata only: a saved area is never opened to build a progress screen. */
    public record ActivityProgress(UUID activity, String title, long deadline, long value, UUID submission,
                                   int revision, boolean late, Long score, long submittedValue) { }
    public record StudentDetail(Member member, Page<ActivityProgress> activities) { }
    public record Screen(Home home, Kind kind, Page<?> rows, int studentCount, int activityCount,
                         Overview overview, Map<UUID,Progress> progress, StudentDetail studentDetail) {
        public Screen { progress=Map.copyOf(progress); }
        public Screen(Home home,Kind kind,Page<?> rows,int studentCount,int activityCount) {
            this(home,kind,rows,studentCount,activityCount,Overview.EMPTY,Map.of(),null);
        }
        public Screen(Home home,Kind kind,Page<?> rows,int studentCount) { this(home,kind,rows,studentCount,0); }
    }
    public record Page<T>(List<T> items, int number, boolean hasNext) {
        public Page { items = List.copyOf(items); }
    }
}
