package sisso.logicsim.classroom.domain;

/** Stable application errors; the Hytale adapter supplies localized presentation. */
public final class ClassroomException extends RuntimeException {
    public enum Reason { FORBIDDEN, ALREADY_MEMBER, INVALID_NAME, INVALID_DESCRIPTION, INVALID_ENROLLMENT,
        DUPLICATE_ENROLLMENT, INVALID_MESSAGE, NOT_FOUND, CLOSED, STALE, UNAVAILABLE }
    private final Reason reason;
    public ClassroomException(Reason reason) { this(reason, null); }
    public ClassroomException(Reason reason, Throwable cause) {
        super(reason.name(), cause);
        this.reason = reason;
    }
    public Reason reason() { return reason; }
}
