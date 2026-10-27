package sisso.logicsim.classroom.learning;

import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.CircuitEvaluator;
import sisso.logicsim.circuit.CircuitNode;
import sisso.logicsim.circuit.CircuitNode.Kind;

import java.util.*;

import static sisso.logicsim.classroom.learning.LearningException.Code.VERIFICATION_LIMIT;

/** Bounded, stateless assistance. Uses only the immutable submitted topology, never a live world. */
public final class SubmissionVerifier {
    private static final Comparator<BlockPosition> PORT_ORDER = Comparator.comparingInt(BlockPosition::x)
            .thenComparingInt(BlockPosition::y).thenComparingInt(BlockPosition::z);

    public enum Status { COUNTS_ONLY, IO_MISMATCH, COMPLETE }
    public enum Outcome { MATCH, MISMATCH, UNSTABLE, DISCONNECTED }

    public record Component(Kind kind, Integer required, int actual) {
        public boolean matches() {
            return required == null || required == actual;
        }
    }

    public record CaseResult(String inputs, String expected, String actual, Outcome outcome) { }

    public record Report(List<Component> components, List<BlockPosition> inputs, List<BlockPosition> outputs,
                         List<CaseResult> cases, Status status) {
        public Report {
            components = List.copyOf(components);
            inputs = List.copyOf(inputs);
            outputs = List.copyOf(outputs);
            cases = List.copyOf(cases);
        }
    }

    public Report verify(VerificationRules rules, Map<BlockPosition, CircuitNode> source) {
        var nodes = new HashMap<>(source);
        var counts = new EnumMap<Kind, Integer>(Kind.class);
        nodes.values().forEach(node -> counts.merge(node.kind(), 1, Integer::sum));
        var components = Arrays.stream(Kind.values())
                .map(kind -> new Component(kind, rules.quantities().get(kind), counts.getOrDefault(kind, 0))).toList();
        var inputs = ports(nodes, Kind.LEVER);
        var outputs = ports(nodes, Kind.LAMP);
        var table = rules.cases();
        if (table.isEmpty()) return new Report(components, inputs, outputs, List.of(), Status.COUNTS_ONLY);
        if (inputs.size() != table.getFirst().inputs().length() || outputs.size() != table.getFirst().outputs().length()) {
            return new Report(components, inputs, outputs, List.of(), Status.IO_MISMATCH);
        }
        if (nodes.size() > VerificationRules.MAX_COMPONENTS) throw new LearningException(VERIFICATION_LIMIT);

        var cases = new ArrayList<CaseResult>();
        var evaluator = new CircuitEvaluator();
        for (var row : table) {
            if (Thread.currentThread().isInterrupted()) throw new LearningException(VERIFICATION_LIMIT);
            for (int index = 0; index < inputs.size(); index++) {
                nodes.put(inputs.get(index), CircuitNode.lever(row.inputs().charAt(index) == '1'));
            }
            var result = evaluator.evaluate(nodes);
            var actualBits = new StringBuilder();
            outputs.forEach(position -> actualBits.append(Boolean.TRUE.equals(result.lamps().get(position)) ? '1' : '0'));
            String actual = actualBits.toString();
            var outcome = outcome(result, inputs, outputs, row.outputs(), actual);
            cases.add(new CaseResult(row.inputs(), row.outputs(), actual, outcome));
        }
        return new Report(components, inputs, outputs, cases, Status.COMPLETE);
    }

    private static List<BlockPosition> ports(Map<BlockPosition, CircuitNode> nodes, Kind kind) {
        return nodes.entrySet().stream().filter(entry -> entry.getValue().kind() == kind)
                .map(Map.Entry::getKey).sorted(PORT_ORDER).toList();
    }

    private static Outcome outcome(CircuitEvaluator.Result result, List<BlockPosition> inputs,
                                   List<BlockPosition> outputs, String expected, String actual) {
        if (!result.feedbackGates().isEmpty() || !result.pendingGates().isEmpty()) return Outcome.UNSTABLE;
        if (disconnected(result, inputs) || disconnected(result, outputs)) return Outcome.DISCONNECTED;
        return expected.equals(actual) ? Outcome.MATCH : Outcome.MISMATCH;
    }

    private static boolean disconnected(CircuitEvaluator.Result result, List<BlockPosition> ports) {
        return ports.stream().anyMatch(position -> result.terminalMasks().getOrDefault(position, 0) == 0);
    }
}
