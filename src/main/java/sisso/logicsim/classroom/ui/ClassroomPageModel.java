package sisso.logicsim.classroom.ui;

import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.ClassroomRuntime.Change;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

/** World-thread state, without Hytale dependencies. Selected revisions remain those actually presented to the user. */
final class ClassroomPageModel {
    Screen screen;
    Kind kind = Kind.MEMBERS;
    String query = "";
    int page;
    Object selected;
    long editRevision;
    boolean confirming;
    int activityPage;
    boolean managing,technical;
    void revoked() { screen = null; selected = null; confirming = false; managing=false;technical=false;activityPage=0;editRevision = 0; }
    boolean teacher() { return screen != null && screen.home().member() != null && screen.home().member().role() == Role.TEACHER; }
    boolean interested(Change change, UUID actor) {
        return interested(screen,kind,change,actor);
    }
    Predicate<Change> interest(UUID actor) {
        Screen snapshot=screen; Kind section=kind;
        return change -> interested(snapshot,section,change,actor);
    }
    private static boolean interested(Screen screen,Kind kind,Change change,UUID actor) {
        if (actor.equals(change.player())) return true;
        var classroom = screen == null ? null : screen.home().classroom();
        if (classroom != null && (classroom.id().equals(change.classroom()) || classroom.teacherId().equals(change.player()))) return true;
        if (change.directory() && (kind == Kind.BROWSE || kind == Kind.PLAYERS)) return true;
        // Creating any active link can close this class's requests, including ones outside the current page.
        if (change.directory() && change.classroom() != null && screen!=null && screen.home().member()!=null
                && screen.home().member().role()==Role.TEACHER && kind == Kind.INTAKES) return true;
        return screen != null && screen.rows().items().stream().anyMatch(row ->
                row instanceof Member m && m.playerId().equals(change.player())
                || row instanceof Intake i && (i.studentId().equals(change.player()) || i.classroomId().equals(change.classroom())));
    }
    boolean apply(Screen next, boolean reset) {
        boolean lostStudent=screen!=null && screen.studentDetail()!=null && next.studentDetail()==null && selected instanceof Member;
        boolean contextChanged = screen == null || next.kind() != screen.kind()
                || !Objects.equals(classroomId(screen), classroomId(next)) || !Objects.equals(membershipId(screen), membershipId(next));
        if (contextChanged) { selected = null; confirming = false; managing=false;technical=false;activityPage=0;reset = true; }
        screen = next; kind = next.kind(); page = next.rows().number();
        if(lostStudent){selected=null;managing=false;technical=false;confirming=false;activityPage=0;}
        if (selected instanceof Intake previous) {
            var latest=next.rows().items().stream().filter(row->row instanceof Intake i && i.id().equals(previous.id()))
                    .map(Intake.class::cast).findFirst().orElse(null);
            // Terminal processes have no editable operation left. Preserve local fields, but remove stale actions.
            if (latest!=null && latest.state()!=previous.state() && latest.state()!=State.INVITED && latest.state()!=State.REQUESTED) {
                selected=latest; confirming=false;
            }
        }
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
        selected = row.get(); confirming = false; managing=false;technical=false;activityPage=0;return true;
    }
    void navigate(Kind kind) { this.kind = kind; query = ""; page = 0;activityPage=0;selected = null;confirming = false;managing=false;technical=false; }
    static UUID id(Object value) {
        return switch (value) {
            case Summary s -> s.id(); case Member m -> m.id(); case Intake i -> i.id(); case Profile p -> p.id();
            default -> throw new IllegalArgumentException("Unsupported Classroom row");
        };
    }
    private static UUID classroomId(Screen screen) { return screen.home().classroom() == null ? null : screen.home().classroom().id(); }
    private static UUID membershipId(Screen screen) { return screen.home().member() == null ? null : screen.home().member().id(); }
}
