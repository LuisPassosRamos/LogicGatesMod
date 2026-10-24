package sisso.logicsim.classroom.domain;

import java.util.List;
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
    public record Screen(Home home, Kind kind, Page<?> rows, int studentCount) { }
    public record Page<T>(List<T> items, int number, boolean hasNext) {
        public Page { items = List.copyOf(items); }
    }
}
