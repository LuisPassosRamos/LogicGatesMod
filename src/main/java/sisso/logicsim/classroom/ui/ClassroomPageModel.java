package sisso.logicsim.classroom.ui;

import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.ClassroomRuntime.Change;
import java.util.Objects;
import java.util.UUID;

/** World-thread state, without Hytale dependencies. Selected revisions remain those actually presented to the user. */
final class ClassroomPageModel {
    Screen screen;
    Kind kind = Kind.MEMBERS;
    String query = "";
    int page;
    Object selected;
    long editRevision;
    boolean confirming;
    boolean teacher() { return screen != null && screen.home().member() != null && screen.home().member().role() == Role.TEACHER; }
    boolean interested(Change change, UUID actor) {
        if (actor.equals(change.player())) return true;
        var classroom = screen == null ? null : screen.home().classroom();
        if (classroom != null && (classroom.id().equals(change.classroom()) || classroom.teacherId().equals(change.player()))) return true;
        if (change.directory() && (kind == Kind.BROWSE || kind == Kind.PLAYERS)) return true;
        // Creating any active link can close this class's requests, including ones outside the current page.
        if (change.directory() && change.classroom() != null && teacher() && kind == Kind.INTAKES) return true;
        return screen != null && screen.rows().items().stream().anyMatch(row ->
                row instanceof Member m && m.playerId().equals(change.player())
                || row instanceof Intake i && i.studentId().equals(change.player()));
    }
    boolean apply(Screen next, boolean reset) {
        boolean contextChanged = screen == null || next.kind() != screen.kind()
                || !Objects.equals(classroomId(screen), classroomId(next)) || !Objects.equals(membershipId(screen), membershipId(next));
        if (contextChanged) { selected = null; confirming = false; reset = true; }
        screen = next; kind = next.kind(); page = next.rows().number();
        if (selected != null && next.rows().items().stream().noneMatch(row -> id(row).equals(id(selected)))) {
            // Absence from one page may mean reordering/pagination. Keep the draft and old UUID/revision until explicit reload.
            confirming = false;
        }
        if (reset) {
            confirming = false;
            if (selected != null) selected = next.rows().items().stream().filter(row -> id(row).equals(id(selected))).findFirst().orElse(null);
            editRevision = next.home().classroom() == null ? 0 : next.home().classroom().revision();
        }
        return reset;
    }
    boolean select(String target) {
        if (screen == null || target == null) return false;
        final UUID uuid;
        try { uuid = UUID.fromString(target); } catch (IllegalArgumentException invalid) { return false; }
        var row = screen.rows().items().stream().filter(value -> id(value).equals(uuid)).findFirst();
        if (row.isEmpty()) return false;
        selected = row.get(); confirming = false; return true;
    }
    void navigate(Kind kind) { this.kind = kind; query = ""; page = 0; selected = null; confirming = false; }
    static UUID id(Object value) {
        return switch (value) {
            case Summary s -> s.id(); case Member m -> m.id(); case Intake i -> i.id(); case Profile p -> p.id();
            default -> throw new IllegalArgumentException("Unsupported Classroom row");
        };
    }
    private static UUID classroomId(Screen screen) { return screen.home().classroom() == null ? null : screen.home().classroom().id(); }
    private static UUID membershipId(Screen screen) { return screen.home().member() == null ? null : screen.home().member().id(); }
}
