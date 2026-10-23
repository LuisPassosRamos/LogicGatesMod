package sisso.logicsim.circuit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Evaluates dependency components without recursion, oscillation or retained state. */
final class CircuitSignals {
    static final int UNKNOWN = -1, OFF = 0, ON = 1;

    static final class Signal {
        final CircuitNode node; // null denotes a cable network (OR of its producers).
        // Operands represent physical input faces: two faces may read the same network.
        final List<Signal> inputs = new ArrayList<>();
        final Set<Signal> dependencies = new LinkedHashSet<>();
        final Set<Signal> readers = new LinkedHashSet<>();
        int value = OFF;
        boolean feedbackBlocked;

        Signal(CircuitNode node) { this.node = node; }
        boolean on() { return value == ON; }
        void addInput(Signal input) {
            inputs.add(input);
            dependencies.add(input);
            input.readers.add(this);
        }

        int compute() {
            if (node != null && node.kind() == CircuitNode.Kind.LEVER) return node.sourceOn() ? ON : OFF;
            if (node != null && (node.unavailableInputMask() != 0 || inputs.isEmpty())) return OFF;
            if (node != null && node.kind() == CircuitNode.Kind.NOT) return invert(inputs.iterator().next().value);
            if (node != null && (node.kind() == CircuitNode.Kind.XOR || node.kind() == CircuitNode.Kind.XNOR)) {
                int parity = OFF;
                for (var input : inputs) {
                    if (input.value == UNKNOWN) return UNKNOWN;
                    parity ^= input.value;
                }
                return node.kind().isInverting() ? invert(parity) : parity;
            }
            boolean conjunction = node != null && (node.kind() == CircuitNode.Kind.AND || node.kind() == CircuitNode.Kind.NAND);
            int result = conjunction ? ON : OFF;
            boolean unknown = false;
            for (var input : inputs) {
                if (conjunction && input.value == OFF || !conjunction && input.value == ON) {
                    result = conjunction ? OFF : ON;
                    unknown = false;
                    break;
                }
                unknown |= input.value == UNKNOWN;
            }
            if (unknown) result = UNKNOWN;
            return node != null && node.kind().isInverting() ? invert(result) : result;
        }
    }

    private record Visit(Signal signal, boolean exit) { }

    private static int invert(int value) { return value == UNKNOWN ? UNKNOWN : value == ON ? OFF : ON; }

    static void solve(List<Signal> signals) {
        // Iterative Kosaraju: source components are produced before their dependents.
        var visited = new HashSet<Signal>();
        var finished = new ArrayList<Signal>();
        var walks = new ArrayDeque<Visit>();
        for (var start : signals) {
            if (visited.contains(start)) continue;
            walks.push(new Visit(start, false));
            while (!walks.isEmpty()) {
                var visit = walks.pop();
                if (visit.exit) finished.add(visit.signal);
                else if (visited.add(visit.signal)) {
                    walks.push(new Visit(visit.signal, true));
                    for (var reader : visit.signal.readers) if (!visited.contains(reader)) walks.push(new Visit(reader, false));
                }
            }
        }
        var assigned = new HashSet<Signal>();
        var pending = new ArrayDeque<Signal>();
        for (int i = finished.size() - 1; i >= 0; i--) {
            var start = finished.get(i);
            if (assigned.contains(start)) continue;
            var component = new LinkedHashSet<Signal>();
            pending.push(start);
            while (!pending.isEmpty()) {
                var signal = pending.pop();
                if (!assigned.add(signal)) continue;
                component.add(signal);
                for (var input : signal.dependencies) if (!assigned.contains(input)) pending.push(input);
            }
            if (component.size() == 1 && !start.dependencies.contains(start)) start.value = start.compute();
            else solveCycle(component);
        }
    }

    private static void solveCycle(Set<Signal> component) {
        boolean nonMonotonic = component.stream().anyMatch(s -> s.node != null && s.node.kind().isNonMonotonic());
        // Positive cycles keep the existing least-false solution. Non-monotonic cycles start unknown;
        // short-circuit logic can still resolve them when external inputs force a definite value.
        for (var signal : component) signal.value = nonMonotonic ? UNKNOWN : OFF;
        var queue = new ArrayDeque<>(component);
        var queued = new HashSet<>(component);
        while (!queue.isEmpty()) {
            var signal = queue.removeFirst();
            queued.remove(signal);
            int next = signal.compute();
            if (next == signal.value) continue;
            if (nonMonotonic && next == UNKNOWN) continue;
            signal.value = next;
            for (var reader : signal.readers) {
                if (component.contains(reader) && queued.add(reader)) queue.addLast(reader);
            }
        }
        // Underdetermined/oscillating feedback is not a latch or a clock in this mod.
        // Only unresolved gates are disabled; externally driven networks keep their known power.
        for (var signal : component) if (signal.value == UNKNOWN) {
            signal.value = OFF;
            signal.feedbackBlocked = signal.node != null && signal.node.isGate();
        }
    }
}
