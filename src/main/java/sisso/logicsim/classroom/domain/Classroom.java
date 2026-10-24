package sisso.logicsim.classroom.domain;

import java.util.UUID;

/** An immutable classroom revision; names never identify players. */
public record Classroom(UUID id, UUID teacherId, String name, String description, long revision) { }
