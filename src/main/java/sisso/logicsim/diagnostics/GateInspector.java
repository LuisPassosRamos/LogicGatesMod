package sisso.logicsim.diagnostics;

import sisso.logicsim.circuit.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import static sisso.logicsim.diagnostics.GateInspection.*;

/** Physical port readings from a completed snapshot, using the evaluator's compatibility rules. */
public final class GateInspector {
    private GateInspector() { }

    public static Map<String, HorizontalDirection> inputFaces(CircuitNode node) {
        if (!node.isGate()) throw new IllegalArgumentException("Expected a gate");
        var faces = new LinkedHashMap<String, HorizontalDirection>();
        int front = node.outputDirection().ordinal();
        if (node.kind() != CircuitNode.Kind.NOT) {
            faces.put("A", HorizontalDirection.values()[(front + 3) & 3]);
            faces.put("B", HorizontalDirection.values()[(front + 1) & 3]);
        }
        faces.put("C", node.outputDirection().opposite());
        return java.util.Collections.unmodifiableMap(faces);
    }

    public static Optional<GateInspection> inspect(CircuitSnapshot snapshot, BlockPosition position) {
        var node = snapshot.nodes().get(position);
        if (node == null || !node.isGate()) return Optional.empty();
        var inputs = new ArrayList<Input>();
        inputFaces(node).forEach((label, direction) -> {
            var next = position.neighbor(direction);
            var neighbor = snapshot.nodes().get(next);
            InputState state;
            if ((node.unavailableInputMask() & direction.bit()) != 0) state = InputState.UNAVAILABLE;
            else if (neighbor == null) state = InputState.ABSENT;
            else if (!CircuitConnections.canConnect(node, direction, neighbor)) state = InputState.INCOMPATIBLE;
            else {
                boolean on = neighbor.kind() == CircuitNode.Kind.CABLE ? snapshot.result().poweredCables().contains(next)
                        : neighbor.kind() == CircuitNode.Kind.LEVER ? neighbor.sourceOn()
                        : snapshot.result().gateOutputs().getOrDefault(next, false);
                state = on ? InputState.ON : InputState.OFF;
            }
            inputs.add(new Input(label, direction, next, state));
        });
        Reason reason = node.unavailableInputMask() != 0 ? Reason.SECTION_UNAVAILABLE
                : snapshot.result().feedbackGates().contains(position) ? Reason.INDETERMINATE_FEEDBACK
                : inputs.stream().noneMatch(i -> i.state() == InputState.ON || i.state() == InputState.OFF)
                ? Reason.NO_INPUTS : Reason.LOGIC;
        return Optional.of(new GateInspection(position, node.kind(), node.outputDirection(),
                snapshot.result().gateOutputs().getOrDefault(position, false), reason, inputs));
    }
}
