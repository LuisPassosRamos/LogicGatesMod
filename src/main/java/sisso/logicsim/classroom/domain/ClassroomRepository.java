package sisso.logicsim.classroom.domain;

import java.util.Optional;
import java.util.UUID;
import sisso.logicsim.classroom.domain.ClassroomViews.*;

public interface ClassroomRepository {
    Optional<Classroom> findByMember(UUID playerId);
    Classroom create(UUID teacherId, String name, String description);
    Classroom update(UUID teacherId, UUID classroomId, long revision, String name, String description);
    Classroom settings(UUID teacherId, UUID classroomId, long revision, String name, String description, String message);
    Classroom message(UUID teacherId, UUID classroomId, long revision, String message);
    void remember(UUID playerId, String name);
    String playerName(UUID playerId);
    Optional<Member> member(UUID playerId);
    Page<Summary> browse(String query, int page);
    Page<Profile> players(UUID teacherId, String query, int page);
    Page<Member> members(UUID teacherId, int page);
    int studentCount(UUID teacherId);
    Page<Intake> intakes(UUID actor, boolean teacher, int page);
    Intake request(UUID student, UUID classroom, String enrollment);
    Intake invite(UUID teacher, UUID target);
    Intake accept(UUID student, UUID intake, long revision, String enrollment);
    Intake closeIntake(UUID actor, UUID intake, long revision, boolean teacher);
    Member approve(UUID teacher, UUID intake, long revision, String enrollment);
    Member correct(UUID teacher, UUID membership, long revision, String enrollment);
    Member end(UUID actor, UUID membership, long revision, boolean teacher);
}
