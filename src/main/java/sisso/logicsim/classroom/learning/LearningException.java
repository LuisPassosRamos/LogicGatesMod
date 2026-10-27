package sisso.logicsim.classroom.learning;

/** Localizable learning failures; input values are never used as translation keys. */
public final class LearningException extends RuntimeException {
    public enum Code { INVALID_DRAFT, FUTURE_DEADLINE, INVALID_SCORE, NO_DRAFT, ARCHIVED, AREA_LIMIT,
        AREA_EMPTY, AREA_UNLOADED, AREA_UNSUPPORTED, AREA_OCCUPIED, AREA_PROTECTED, NO_SELECTION,
        SELECTION_EXPIRED, BUSY, STORAGE_LIMIT, CORRUPT_CONTENT, INVENTORY_FULL, INVALID_RULES, VERIFICATION_LIMIT }
    private final Code code;
    private String problem="";
    public LearningException(Code code) { super(code.name()); this.code=code; }
    public LearningException(Code code,Throwable cause) { super(code.name(),cause); this.code=code; }
    public LearningException problem(String problem){this.problem=problem;return this;}
    public String problem(){return problem;}
    public Code code() { return code; }
}
