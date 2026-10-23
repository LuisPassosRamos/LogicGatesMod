package sisso.logicsim.circuit;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.circuit.CircuitNode.Kind.*;
import static sisso.logicsim.circuit.HorizontalDirection.*;

class ParityGateTest {
    private final CircuitEvaluator evaluator = new CircuitEvaluator();
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 10, z); }
    private static BlockPosition rotate(BlockPosition offset, HorizontalDirection output) {
        int x = offset.x(), z = offset.z();
        return switch (output) {
            case EAST -> p(x, z);
            case SOUTH -> p(-z, x);
            case WEST -> p(-x, -z);
            case NORTH -> p(z, -x);
        };
    }

    @Test void eachPhysicalFaceCountsEvenWhenTwoOrThreeInputsShareOneCableNetwork() {
        for (var kind : new CircuitNode.Kind[]{XOR, XNOR}) for (var output : HorizontalDirection.values()) {
            for (int count : new int[]{2, 3}) for (boolean on : new boolean[]{false, true}) {
                var nodes = new HashMap<BlockPosition, CircuitNode>();
                nodes.put(p(0, 0), CircuitNode.gate(kind, output));
                for (var offset : new BlockPosition[]{p(-1, 0), p(-1, -1), p(0, -1)}) {
                    nodes.put(rotate(offset, output), CircuitNode.cable());
                }
                if (count == 3) for (var offset : new BlockPosition[]{p(-1, 1), p(0, 1)}) {
                    nodes.put(rotate(offset, output), CircuitNode.cable());
                }
                nodes.put(rotate(p(-2, -1), output), CircuitNode.lever(on));
                var lamp = rotate(p(1, 0), output);
                nodes.put(lamp, CircuitNode.lamp());
                boolean parity = on && count % 2 == 1;
                var result = evaluator.evaluate(nodes);
                assertEquals(kind == XNOR ? !parity : parity, result.gateOutputs().get(p(0, 0)));
                assertEquals(result.gateOutputs().get(p(0, 0)), result.lamps().get(lamp));
            }
        }
    }

    @Test void inputRemovalAndReplacementChangesParityWithoutAnotherLeverClick() {
        for (var kind : new CircuitNode.Kind[]{XOR, XNOR}) {
            var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(kind, EAST),
                    p(-1, 0), CircuitNode.lever(true), p(0, -1), CircuitNode.lever(true),
                    p(1, 0), CircuitNode.lamp()));
            assertEquals(kind == XNOR, evaluator.evaluate(nodes).lamps().get(p(1, 0)));
            nodes.remove(p(0, -1));
            assertEquals(kind == XOR, evaluator.evaluate(nodes).lamps().get(p(1, 0)));
            nodes.put(p(0, -1), CircuitNode.lever(false));
            assertEquals(kind == XOR, evaluator.evaluate(nodes).lamps().get(p(1, 0)));
            nodes.remove(p(-1, 0));
            assertEquals(kind == XNOR, evaluator.evaluate(nodes).lamps().get(p(1, 0)));
            nodes.remove(p(0, -1));
            assertFalse(evaluator.evaluate(nodes).lamps().get(p(1, 0)));
        }
    }

    private Map<BlockPosition, CircuitNode> loop(CircuitNode.Kind kind) {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(p(0, 0), CircuitNode.gate(kind, EAST));
        // Only the rear input touches this loop; the side faces remain independent.
        for (var wire : new BlockPosition[]{p(1, 0), p(2, 0), p(2, 1), p(2, 2), p(1, 2),
                p(0, 2), p(-1, 2), p(-2, 2), p(-2, 1), p(-2, 0), p(-1, 0)}) {
            nodes.put(wire, CircuitNode.cable());
        }
        return nodes;
    }

    @Test void xorAndXnorCyclesStartUnknownAndNeverOscillateOrRememberPower() {
        for (var kind : new CircuitNode.Kind[]{XOR, XNOR}) for (boolean sideOn : new boolean[]{false, true}) {
            var nodes = loop(kind);
            nodes.put(p(0, -1), CircuitNode.lever(sideOn));
            var unknown = evaluator.evaluate(nodes);
            assertFalse(unknown.gateOutputs().get(p(0, 0)));
            assertEquals(Set.of(p(0, 0)), unknown.feedbackGates());
            assertTrue(unknown.poweredCables().isEmpty());
            nodes.put(p(3, 0), CircuitNode.lever(true));
            var forced = evaluator.evaluate(nodes);
            assertEquals(kind == XNOR ? sideOn : !sideOn, forced.gateOutputs().get(p(0, 0)));
            assertTrue(forced.feedbackGates().isEmpty());
            assertEquals(11, forced.poweredCables().size());
            for (int seed = 0; seed < 12; seed++) {
                var entries = new ArrayList<>(nodes.entrySet());
                Collections.shuffle(entries, new Random(seed));
                var shuffled = new LinkedHashMap<BlockPosition, CircuitNode>();
                entries.forEach(e -> shuffled.put(e.getKey(), e.getValue()));
                assertEquals(forced, evaluator.evaluate(shuffled));
            }
            nodes.remove(p(3, 0));
            assertEquals(unknown, evaluator.evaluate(nodes));
        }
    }

    @Test void mixedChainsIncludeAllSevenGateKindsAndReconstructFromBothSourceStates() {
        var kinds = new CircuitNode.Kind[]{OR, XOR, NOT, XNOR, AND, NAND, NOR};
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        for (int i = 0; i < kinds.length; i++) nodes.put(p(i, 0), CircuitNode.gate(kinds[i], EAST));
        nodes.put(p(kinds.length, 0), CircuitNode.lamp());
        for (boolean on : new boolean[]{false, true, false}) {
            nodes.put(p(-1, 0), CircuitNode.lever(on));
            var result = evaluator.evaluate(nodes);
            assertEquals(on, result.lamps().get(p(kinds.length, 0)));
            assertEquals(!on, result.gateOutputs().get(p(2, 0)));
            assertEquals(on, result.gateOutputs().get(p(3, 0)));
        }
    }

    @Test void xnorMatchesXorFollowedByNotExceptTheSpecifiedDisconnectedCase() {
        for (int code = 1; code < 27; code++) {
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            int config = code;
            for (var input : new HorizontalDirection[]{NORTH, SOUTH, WEST}) {
                int state = config % 3;
                config /= 3;
                if (state != 0) nodes.put(p(0, 0).neighbor(input), CircuitNode.lever(state == 2));
            }
            nodes.put(p(0, 0), CircuitNode.gate(XNOR, EAST));
            boolean expected = evaluator.evaluate(nodes).gateOutputs().get(p(0, 0));
            nodes.put(p(0, 0), CircuitNode.gate(XOR, EAST));
            nodes.put(p(1, 0), CircuitNode.gate(NOT, EAST));
            assertEquals(expected, evaluator.evaluate(nodes).gateOutputs().get(p(1, 0)));
        }
    }

    @Test void unavailableFacesSuspendParityEvenWhenKnownInputsWouldOtherwiseLightIt() {
        for (var kind : new CircuitNode.Kind[]{XOR, XNOR}) for (var output : HorizontalDirection.values()) {
            for (var missing : HorizontalDirection.values()) {
                if (missing == output) continue;
                var nodes = new HashMap<BlockPosition, CircuitNode>();
                nodes.put(p(0, 0), CircuitNode.gate(kind, output, missing.bit()));
                for (var input : HorizontalDirection.values()) if (input != output && input != missing) {
                    nodes.put(p(0, 0).neighbor(input), CircuitNode.lever(kind == XOR && input == output.opposite()));
                }
                var result = evaluator.evaluate(nodes);
                assertEquals(Set.of(p(0, 0)), result.pendingGates());
                assertFalse(result.gateOutputs().get(p(0, 0)));
                assertTrue(result.feedbackGates().isEmpty());
                nodes.put(p(0, 0), CircuitNode.gate(kind, output));
                assertTrue(evaluator.evaluate(nodes).pendingGates().isEmpty());
            }
        }
    }

    @Test void outputPowerDoesNotChangeParityOrFeedTheInputNetwork() {
        for (var kind : new CircuitNode.Kind[]{XOR, XNOR}) {
            var nodes = Map.of(p(0, 0), CircuitNode.gate(kind, EAST),
                    p(-1, 0), CircuitNode.cable(), p(-2, 0), CircuitNode.lever(kind == XNOR),
                    p(1, 0), CircuitNode.cable(), p(2, 0), CircuitNode.lever(true), p(1, 1), CircuitNode.lamp());
            var result = evaluator.evaluate(nodes);
            assertFalse(result.gateOutputs().get(p(0, 0)));
            assertTrue(result.lamps().get(p(1, 1)));
            assertEquals(kind == XNOR, result.poweredCables().contains(p(-1, 0)));
        }
    }

    @Test void incompatibleFacesAndNonHorizontalNodesNeverCountAsParityInputs() {
        for (var kind : new CircuitNode.Kind[]{XOR, XNOR}) {
            var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(kind, EAST),
                    p(-1, 0), CircuitNode.gate(AND, WEST), p(1, 0), CircuitNode.gate(OR, WEST),
                    p(0, -1), CircuitNode.lamp(), p(0, 1), CircuitNode.gate(NOT, SOUTH),
                    p(-1, -1), CircuitNode.lever(true), new BlockPosition(0, 11, 0), CircuitNode.lever(true)));
            var result = evaluator.evaluate(nodes);
            assertFalse(result.gateOutputs().get(p(0, 0)));
            assertEquals(0, result.gateMasks().get(p(0, 0)));
            nodes.put(p(-1, 0), CircuitNode.lever(false));
            nodes.put(p(1, 0), CircuitNode.lamp());
            result = evaluator.evaluate(nodes);
            assertEquals(kind == XNOR, result.gateOutputs().get(p(0, 0)));
            assertEquals(kind == XNOR, result.lamps().get(p(1, 0)));
            assertEquals(10, result.gateMasks().get(p(0, 0)));
        }
    }

    @Test void sharedOutputNetworksAggregateParityGateProducersByOr() {
        var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(XOR, EAST), p(-1, 0), CircuitNode.lever(false),
                p(0, 2), CircuitNode.gate(XNOR, EAST), p(-1, 2), CircuitNode.lever(false),
                p(1, 0), CircuitNode.cable(), p(1, 1), CircuitNode.cable(), p(1, 2), CircuitNode.cable(),
                p(2, 1), CircuitNode.lamp()));
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(2, 1)));
        nodes.put(p(-1, 2), CircuitNode.lever(true));
        assertFalse(evaluator.evaluate(nodes).lamps().get(p(2, 1)));
        nodes.put(p(-1, 0), CircuitNode.lever(true));
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(2, 1)));
        nodes.put(p(-1, 2), CircuitNode.lever(false));
        nodes.remove(p(0, 0));
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(2, 1)));
        nodes.put(p(-1, 2), CircuitNode.lever(true));
        assertFalse(evaluator.evaluate(nodes).lamps().get(p(2, 1)));
    }

    @Test void longParityChainsUseNoRecursiveStackAndKeepNoDerivedState() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        for (int x = 0; x < 1024; x++) nodes.put(p(x, 0), CircuitNode.gate(x % 2 == 0 ? XOR : XNOR, EAST));
        nodes.put(p(1024, 0), CircuitNode.lamp());
        for (boolean on : new boolean[]{false, true, false}) {
            nodes.put(p(-1, 0), CircuitNode.lever(on));
            assertEquals(on, evaluator.evaluate(nodes).lamps().get(p(1024, 0)));
        }
    }
}
