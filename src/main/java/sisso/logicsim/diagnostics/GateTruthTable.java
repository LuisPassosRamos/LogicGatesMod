package sisso.logicsim.diagnostics;

import sisso.logicsim.circuit.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** Teaching examples use the actual evaluator, including optional-input behavior. */
public final class GateTruthTable {
    public record Row(List<Boolean> inputs, boolean output) { public Row { inputs = List.copyOf(inputs); } }
    private GateTruthTable() { }

    public static List<Row> rows(CircuitNode.Kind kind, int count) {
        if (!kind.isGate() || count < 1 || count > (kind == CircuitNode.Kind.NOT ? 1 : 3)) {
            throw new IllegalArgumentException("Invalid input count for " + kind);
        }
        var center = new BlockPosition(0, 0, 0);
        var gate = CircuitNode.gate(kind, HorizontalDirection.EAST);
        var faces = List.copyOf(GateInspector.inputFaces(gate).values());
        var rows = new ArrayList<Row>();
        for (int mask = 0; mask < (1 << count); mask++) {
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            nodes.put(center, gate);
            var inputs = new ArrayList<Boolean>();
            for (int i = 0; i < count; i++) {
                boolean on = (mask & (1 << (count - i - 1))) != 0;
                inputs.add(on);
                nodes.put(center.neighbor(faces.get(i)), CircuitNode.lever(on));
            }
            rows.add(new Row(inputs, new CircuitEvaluator().evaluate(nodes).gateOutputs().get(center)));
        }
        return List.copyOf(rows);
    }
}
