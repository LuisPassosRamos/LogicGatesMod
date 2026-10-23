package sisso.logicsim.circuit;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Stateless directional circuit evaluation, including inversion and explicit feedback diagnostics. */
public final class CircuitEvaluator {
    public record Result(Map<BlockPosition, Boolean> lamps,
                         Map<BlockPosition, Integer> cableMasks,
                         Map<BlockPosition, Integer> terminalMasks,
                         Set<BlockPosition> poweredCables,
                         Map<BlockPosition, Boolean> gateOutputs,
                         Map<BlockPosition, Integer> gateMasks,
                         Set<BlockPosition> pendingGates,
                         Set<BlockPosition> feedbackGates) {
        public Result {
            lamps = Map.copyOf(lamps);
            cableMasks = Map.copyOf(cableMasks);
            terminalMasks = Map.copyOf(terminalMasks);
            poweredCables = Set.copyOf(poweredCables);
            gateOutputs = Map.copyOf(gateOutputs);
            gateMasks = Map.copyOf(gateMasks);
            pendingGates = Set.copyOf(pendingGates);
            feedbackGates = Set.copyOf(feedbackGates);
        }
    }

    public Result evaluate(Map<BlockPosition, CircuitNode> nodes) {
        var cableMasks = new HashMap<BlockPosition, Integer>();
        var terminalMasks = new HashMap<BlockPosition, Integer>();
        var gateMasks = new HashMap<BlockPosition, Integer>();
        var pending = new HashSet<BlockPosition>();
        for (var entry : nodes.entrySet()) {
            int mask = 0;
            for (var direction : HorizontalDirection.values()) {
                if (CircuitConnections.canConnect(entry.getValue(), direction,
                        nodes.get(entry.getKey().neighbor(direction)))) mask |= direction.bit();
            }
            var node = entry.getValue();
            if (node.kind() == CircuitNode.Kind.CABLE) cableMasks.put(entry.getKey(), mask);
            else if (node.isGate()) {
                gateMasks.put(entry.getKey(), mask);
                if (node.unavailableInputMask() != 0) pending.add(entry.getKey());
            } else terminalMasks.put(entry.getKey(), mask);
        }

        // Every cable component shares one signal. Terminals never merge networks.
        var signals = new HashMap<BlockPosition, CircuitSignals.Signal>();
        var allSignals = new ArrayList<CircuitSignals.Signal>();
        for (var entry : nodes.entrySet()) {
            if (entry.getValue().kind() != CircuitNode.Kind.CABLE || signals.containsKey(entry.getKey())) continue;
            var signal = new CircuitSignals.Signal(null);
            allSignals.add(signal);
            var wires = new ArrayDeque<BlockPosition>();
            signals.put(entry.getKey(), signal);
            wires.add(entry.getKey());
            while (!wires.isEmpty()) {
                var current = wires.removeFirst();
                for (var direction : HorizontalDirection.values()) {
                    var next = current.neighbor(direction);
                    var node = nodes.get(next);
                    if (node != null && node.kind() == CircuitNode.Kind.CABLE && !signals.containsKey(next)) {
                        signals.put(next, signal);
                        wires.addLast(next);
                    }
                }
            }
        }
        for (var entry : nodes.entrySet()) {
            var node = entry.getValue();
            if (node.kind() == CircuitNode.Kind.LEVER || node.isGate()) {
                var output = new CircuitSignals.Signal(node);
                signals.put(entry.getKey(), output);
                allSignals.add(output);
            }
        }
        for (var entry : nodes.entrySet()) {
            var node = entry.getValue();
            var position = entry.getKey();
            if (node.kind() == CircuitNode.Kind.LEVER || node.isGate()) {
                var output = signals.get(position);
                for (var direction : HorizontalDirection.values()) {
                    if (!CircuitConnections.isOutput(node, direction)) continue;
                    var next = position.neighbor(direction);
                    var neighbor = nodes.get(next);
                    if (neighbor != null && neighbor.kind() == CircuitNode.Kind.CABLE) signals.get(next).addInput(output);
                }
            }
            if (node.isGate() && node.unavailableInputMask() == 0) {
                for (var direction : HorizontalDirection.values()) {
                    if (!CircuitConnections.isInput(node, direction)) continue;
                    var next = position.neighbor(direction);
                    if (CircuitConnections.canConnect(node, direction, nodes.get(next))) signals.get(position).addInput(signals.get(next));
                }
            }
        }
        CircuitSignals.solve(allSignals);

        var lamps = new HashMap<BlockPosition, Boolean>();
        var gates = new HashMap<BlockPosition, Boolean>();
        var powered = new HashSet<BlockPosition>();
        var feedback = new HashSet<BlockPosition>();
        for (var entry : nodes.entrySet()) {
            var position = entry.getKey();
            var node = entry.getValue();
            if (node.isGate()) {
                gates.put(position, signals.get(position).on());
                if (signals.get(position).feedbackBlocked) feedback.add(position);
            }
            else if (node.kind() == CircuitNode.Kind.CABLE) {
                if (signals.get(position).on()) powered.add(position);
            } else if (node.kind() == CircuitNode.Kind.LAMP) {
                boolean on = false;
                for (var direction : HorizontalDirection.values()) {
                    var next = position.neighbor(direction);
                    if (CircuitConnections.canConnect(node, direction, nodes.get(next)) && signals.get(next).on()) {
                        on = true;
                        break;
                    }
                }
                lamps.put(position, on);
            }
        }
        return new Result(lamps, cableMasks, terminalMasks, powered, gates, gateMasks, pending, feedback);
    }
}
