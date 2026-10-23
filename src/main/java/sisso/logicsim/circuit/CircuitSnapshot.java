package sisso.logicsim.circuit;

import java.util.Map;

/** Immutable last completed evaluation; inspection never changes source intention. */
public record CircuitSnapshot(Map<BlockPosition, CircuitNode> nodes, CircuitEvaluator.Result result) {
    public CircuitSnapshot { nodes = Map.copyOf(nodes); java.util.Objects.requireNonNull(result); }
    public static CircuitSnapshot empty() { return new CircuitSnapshot(Map.of(), new CircuitEvaluator().evaluate(Map.of())); }
}
