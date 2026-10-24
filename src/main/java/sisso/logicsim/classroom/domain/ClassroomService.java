package sisso.logicsim.classroom.domain;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import static sisso.logicsim.classroom.domain.ClassroomException.Reason.*;

/** Pure application policy. The permission predicate is supplied by a trusted server adapter. */
public final class ClassroomService {
    public static final int NAME_LIMIT = 64;
    public static final int DESCRIPTION_LIMIT = 2048;
    private final ClassroomRepository repository;
    private final Predicate<UUID> canTeach;

    public ClassroomService(ClassroomRepository repository, Predicate<UUID> canTeach) {
        this.repository = repository;
        this.canTeach = canTeach;
    }

    public Optional<Classroom> own(UUID actor) {
        authorize(actor);
        var classroom = repository.findByMember(actor);
        if (classroom.isPresent() && !classroom.get().teacherId().equals(actor)) throw new ClassroomException(FORBIDDEN);
        return classroom;
    }

    public Classroom create(UUID actor, String name, String description) {
        authorize(actor);
        return repository.create(actor, name(name), description(description));
    }

    public Classroom update(UUID actor, UUID classroomId, long revision, String name, String description) {
        authorize(actor);
        // Ownership and revision are checked again inside the repository's write transaction.
        return repository.update(actor, classroomId, revision, name(name), description(description));
    }
    public Classroom settings(UUID actor, UUID classroomId, long revision, String name, String description, String message) {
        authorize(actor); identity(classroomId);
        return repository.settings(actor, classroomId, revision, name(name), description(description), multiline(message, INVALID_MESSAGE));
    }

    public Home home(UUID actor) {
        identity(actor);
        var member = repository.member(actor).orElse(null);
        boolean teaching = canTeach.test(actor);
        if (member != null && member.role() == Role.TEACHER && !teaching) throw new ClassroomException(FORBIDDEN);
        var classroom = member == null ? null : repository.findByMember(actor).orElseThrow(() -> new ClassroomException(UNAVAILABLE));
        return new Home(classroom, member, classroom == null ? "" : repository.playerName(classroom.teacherId()), teaching);
    }
    public Screen screen(UUID actor, Kind requested, String query, int page) {
        var home = home(actor);
        Kind kind = requested;
        boolean teacher = home.member() != null && home.member().role() == Role.TEACHER;
        if (home.member() != null && !teacher) kind = Kind.DETAIL;
        else if (teacher && (kind == Kind.BROWSE || kind == Kind.DETAIL)) kind = Kind.MEMBERS;
        else if (!teacher && kind != Kind.BROWSE && kind != Kind.INTAKES && !(kind == Kind.SETTINGS && home.canTeach())) kind = Kind.BROWSE;
        Page<?> rows = switch (kind) {
            case BROWSE -> browse(actor, query, page);
            case MEMBERS -> members(actor, page);
            case INTAKES -> intakes(actor, teacher, page);
            case PLAYERS -> players(actor, query, page);
            case SETTINGS, DETAIL -> new Page<>(java.util.List.of(), 0, false);
        };
        return new Screen(home, kind, rows, teacher ? repository.studentCount(actor) : 0);
    }

    public void remember(UUID actor, String username) {
        identity(actor);
        // The adapter supplies an authenticated username. It is never accepted from a UI event.
        if (username == null || username.isBlank() || username.length() > 128 || username.codePoints().anyMatch(Character::isISOControl)) {
            throw new ClassroomException(INVALID_NAME);
        }
        repository.remember(actor, username);
    }
    public Page<Summary> browse(UUID actor, String query, int page) { identity(actor); return repository.browse(query(query), page(page)); }
    public Page<Profile> players(UUID actor, String query, int page) { authorize(actor); return repository.players(actor, query(query), page(page)); }
    public Page<Member> members(UUID actor, int page) { authorize(actor); return repository.members(actor, page(page)); }
    public Page<Intake> intakes(UUID actor, boolean teacher, int page) {
        if (teacher) authorize(actor); else identity(actor);
        return repository.intakes(actor, teacher, page(page));
    }
    public Intake request(UUID actor, UUID classroom, String enrollment) {
        identity(actor); identity(classroom); return repository.request(actor, classroom, enrollment(enrollment));
    }
    public Intake invite(UUID actor, UUID target) { authorize(actor); identity(target); return repository.invite(actor, target); }
    public Intake accept(UUID actor, UUID intake, long revision, String enrollment) {
        identity(actor); identity(intake); return repository.accept(actor, intake, revision, enrollment(enrollment));
    }
    public Intake closeIntake(UUID actor, UUID intake, long revision, boolean teacher) {
        if (teacher) authorize(actor); else identity(actor);
        identity(intake); return repository.closeIntake(actor, intake, revision, teacher);
    }
    public Member approve(UUID actor, UUID intake, long revision, String enrollment) {
        authorize(actor); identity(intake); return repository.approve(actor, intake, revision, enrollment(enrollment));
    }
    public Member correct(UUID actor, UUID membership, long revision, String enrollment) {
        authorize(actor); identity(membership); return repository.correct(actor, membership, revision, enrollment(enrollment));
    }
    public Member end(UUID actor, UUID membership, long revision, boolean teacher) {
        if (teacher) authorize(actor); else identity(actor);
        identity(membership); return repository.end(actor, membership, revision, teacher);
    }
    public Classroom message(UUID actor, UUID classroom, long revision, String message) {
        authorize(actor); identity(classroom);
        return repository.message(actor, classroom, revision, multiline(message, INVALID_MESSAGE));
    }
    private static String enrollment(String value) {
        if (value == null) throw new ClassroomException(INVALID_ENROLLMENT);
        String result = value.strip();
        if (result.isEmpty() || result.codePointCount(0, result.length()) > 64 || result.codePoints().anyMatch(Character::isISOControl)) {
            throw new ClassroomException(INVALID_ENROLLMENT);
        }
        return result;
    }
    private static void identity(UUID actor) { if (actor == null) throw new ClassroomException(FORBIDDEN); }
    private static int page(int page) { if (page < 0 || page > 100000) throw new ClassroomException(NOT_FOUND); return page; }
    private static String query(String query) { return query == null ? "" : query.strip().substring(0, Math.min(query.strip().length(), 128)); }

    private void authorize(UUID actor) {
        if (actor == null || !canTeach.test(actor)) throw new ClassroomException(FORBIDDEN);
    }

    private String name(String value) {
        if (value == null) throw new ClassroomException(INVALID_NAME);
        String name = value.strip();
        if (name.isEmpty() || name.codePointCount(0, name.length()) > NAME_LIMIT
                || name.codePoints().anyMatch(Character::isISOControl)) throw new ClassroomException(INVALID_NAME);
        return name;
    }

    private String description(String value) {
        return multiline(value, INVALID_DESCRIPTION);
    }
    private static String multiline(String value, ClassroomException.Reason reason) {
        if (value == null) throw new ClassroomException(reason);
        String description = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (description.codePointCount(0, description.length()) > DESCRIPTION_LIMIT
                || description.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
            throw new ClassroomException(reason);
        }
        return description;
    }
}
