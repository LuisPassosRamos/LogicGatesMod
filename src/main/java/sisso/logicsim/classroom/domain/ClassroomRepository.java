package sisso.logicsim.classroom.domain;

import java.util.Optional;
import java.util.UUID;

public interface ClassroomRepository {
    Optional<Classroom> findByMember(UUID playerId);
    Classroom create(UUID teacherId, String name, String description);
    Classroom update(UUID teacherId, UUID classroomId, long revision, String name, String description);
}
