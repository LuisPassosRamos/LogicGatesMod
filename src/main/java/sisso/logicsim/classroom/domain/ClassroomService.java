package sisso.logicsim.classroom.domain;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Predicate;
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
        if (value == null) throw new ClassroomException(INVALID_DESCRIPTION);
        String description = value.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (description.codePointCount(0, description.length()) > DESCRIPTION_LIMIT
                || description.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t')) {
            throw new ClassroomException(INVALID_DESCRIPTION);
        }
        return description;
    }
}
