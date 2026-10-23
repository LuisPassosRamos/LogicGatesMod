package sisso.logicsim.circuit;

import java.util.Objects;

public record CircuitNode(Kind kind, boolean sourceOn, HorizontalDirection outputDirection,
                          int unavailableInputMask) {
    public enum Kind {
        LEVER, CABLE, LAMP, AND, OR, NOT, NAND, NOR, XOR, XNOR;
        public boolean isGate() { return this == AND || this == OR || this == XOR || isInverting(); }
        public boolean isInverting() { return this == NOT || this == NAND || this == NOR || this == XNOR; }
        public boolean isNonMonotonic() { return this == XOR || isInverting(); }
    }

    public CircuitNode(Kind kind, boolean sourceOn) { this(kind, sourceOn, null, 0); }

    public CircuitNode {
        Objects.requireNonNull(kind);
        if (kind != Kind.LEVER && sourceOn) {
            throw new IllegalArgumentException("Only levers can be sources");
        }
        if (kind.isGate()) {
            Objects.requireNonNull(outputDirection, "Gate output direction");
            if (unavailableInputMask < 0 || unavailableInputMask > 15
                    || (unavailableInputMask & ~inputMask(kind, outputDirection)) != 0) {
                throw new IllegalArgumentException("Invalid unavailable input mask");
            }
        } else if (outputDirection != null || unavailableInputMask != 0) {
            throw new IllegalArgumentException("Only gates have directional inputs and outputs");
        }
    }

    public boolean isGate() { return kind.isGate(); }
    public static int inputMask(Kind kind, HorizontalDirection output) {
        return kind == Kind.NOT ? output.opposite().bit() : 15 ^ output.bit();
    }
    public static CircuitNode gate(Kind kind, HorizontalDirection output) { return gate(kind, output, 0); }
    public static CircuitNode gate(Kind kind, HorizontalDirection output, int unavailableInputs) {
        if (!kind.isGate()) throw new IllegalArgumentException("Expected a logic gate");
        return new CircuitNode(kind, false, output, unavailableInputs);
    }

    public static CircuitNode lever(boolean on) { return new CircuitNode(Kind.LEVER, on); }
    public static CircuitNode cable() { return new CircuitNode(Kind.CABLE, false); }
    public static CircuitNode lamp() { return new CircuitNode(Kind.LAMP, false); }
}
