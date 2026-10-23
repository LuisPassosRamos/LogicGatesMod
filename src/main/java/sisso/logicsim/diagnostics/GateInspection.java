package sisso.logicsim.diagnostics;

import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.CircuitNode;
import sisso.logicsim.circuit.HorizontalDirection;
import java.util.List;

public record GateInspection(BlockPosition position, CircuitNode.Kind kind, HorizontalDirection outputDirection,
                             boolean outputOn, Reason reason, List<Input> inputs) {
    public enum Reason { LOGIC, NO_INPUTS, SECTION_UNAVAILABLE, INDETERMINATE_FEEDBACK }
    public enum InputState { ABSENT, OFF, ON, UNAVAILABLE, INCOMPATIBLE }
    public record Input(String label, HorizontalDirection direction, BlockPosition neighbor, InputState state) { }
    public GateInspection { inputs = List.copyOf(inputs); }
}
