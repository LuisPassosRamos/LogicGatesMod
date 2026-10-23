package sisso.logicsim.circuit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.circuit.CircuitNode.Kind.*;
import static sisso.logicsim.circuit.HorizontalDirection.*;

class GateEvaluatorTest {
    private final CircuitEvaluator evaluator = new CircuitEvaluator();
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 10, z); }

    static Stream<Arguments> truthCases() {
        var cases = new ArrayList<Arguments>();
        for (var kind : new CircuitNode.Kind[]{AND, OR, NAND, NOR, XOR, XNOR}) for (var facing : HorizontalDirection.values()) {
            for (int configuration = 0; configuration < 27; configuration++) {
                cases.add(Arguments.of(kind, facing, configuration));
            }
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "{0}, output {1}, absent/off/on configuration {2}")
    @MethodSource("truthCases")
    void allOptionalInputCombinationsInEveryOrientationWithDirectAndCableConnections(
            CircuitNode.Kind kind, HorizontalDirection facing, int configuration) {
        for (boolean useCables : new boolean[]{false, true}) {
            var center = p(0, 0);
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            nodes.put(center, CircuitNode.gate(kind, facing));
            int code = configuration, connected = 0, energized = 0, mask = facing.bit();
            for (var input : HorizontalDirection.values()) {
                if (input == facing) continue;
                int state = code % 3;
                code /= 3;
                if (state == 0) continue;
                connected++;
                if (state == 2) energized++;
                mask |= input.bit();
                var neighbor = center.neighbor(input);
                if (useCables) {
                    nodes.put(neighbor, CircuitNode.cable());
                    neighbor = neighbor.neighbor(input);
                }
                nodes.put(neighbor, CircuitNode.lever(state == 2));
            }
            var lamp = center.neighbor(facing);
            if (useCables) {
                nodes.put(lamp, CircuitNode.cable());
                lamp = lamp.neighbor(facing);
            }
            nodes.put(lamp, CircuitNode.lamp());
            boolean base = switch (kind) {
                case AND, NAND -> energized == connected;
                case XOR, XNOR -> energized % 2 == 1;
                default -> energized > 0;
            };
            boolean expected = connected > 0 && (kind.isInverting() ? !base : base);
            var result = evaluator.evaluate(nodes);
            assertEquals(expected, result.gateOutputs().get(center));
            assertEquals(expected, result.lamps().get(lamp));
            assertEquals(mask, result.gateMasks().get(center));
            assertTrue(result.pendingGates().isEmpty());
            if (useCables) assertEquals(expected, result.poweredCables().contains(center.neighbor(facing)));
        }
    }

    @Test void removingAnOffInputCanTurnAndOnButDisconnectedGatesStayOff() {
        var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(AND, EAST),
                p(-1, 0), CircuitNode.lever(true), p(0, -1), CircuitNode.cable()));
        assertFalse(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
        nodes.remove(p(0, -1));
        assertTrue(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
        nodes.remove(p(-1, 0));
        assertFalse(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
        nodes.put(p(-1, -1), CircuitNode.lever(true));
        nodes.put(new BlockPosition(0, 11, 0), CircuitNode.lever(true));
        assertFalse(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
    }

    @Test void incompatibleFacesDoNotConnectOrCountAsInputs() {
        var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(AND, EAST),
                p(-1, 0), CircuitNode.lever(true), p(1, 0), CircuitNode.gate(OR, WEST),
                p(0, -1), CircuitNode.gate(OR, EAST), p(0, 1), CircuitNode.lamp()));
        var result = evaluator.evaluate(nodes);
        assertTrue(result.gateOutputs().get(p(0, 0)));
        assertEquals(8, result.gateMasks().get(p(0, 0)));
        assertEquals(0, result.terminalMasks().get(p(0, 1)));
        assertFalse(result.lamps().get(p(0, 1)));
        nodes.put(p(1, 0), CircuitNode.gate(OR, EAST));
        result = evaluator.evaluate(nodes);
        assertTrue(result.gateOutputs().get(p(1, 0)));
        assertEquals(10, result.gateMasks().get(p(0, 0)));
    }

    @Test void anEnergizedOutputCableDoesNotBackfeedTheGateOrItsInputs() {
        var nodes = Map.of(p(0, 0), CircuitNode.gate(AND, EAST), p(0, -1), CircuitNode.cable(),
                p(1, 0), CircuitNode.cable(), p(2, 0), CircuitNode.lever(true), p(1, 1), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertFalse(result.gateOutputs().get(p(0, 0)));
        assertTrue(result.lamps().get(p(1, 1)));
        assertTrue(result.poweredCables().contains(p(1, 0)));
        assertFalse(result.poweredCables().contains(p(0, -1)));
    }

    @Test void multipleGateProducersUseOrOnTheSharedCableNetwork() {
        var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(AND, EAST),
                p(-1, 0), CircuitNode.lever(true), p(0, 2), CircuitNode.gate(OR, EAST),
                p(-1, 2), CircuitNode.lever(false), p(1, 0), CircuitNode.cable(),
                p(1, 1), CircuitNode.cable(), p(1, 2), CircuitNode.cable(), p(2, 1), CircuitNode.lamp()));
        var result = evaluator.evaluate(nodes);
        assertTrue(result.lamps().get(p(2, 1)));
        assertFalse(result.gateOutputs().get(p(0, 2)));
        nodes.put(p(-1, 0), CircuitNode.lever(false));
        assertFalse(evaluator.evaluate(nodes).lamps().get(p(2, 1)));
    }

    @Test void longChainsOfDirectAndOrGatesSettleWithoutRecursionAndResetAfterRemoval() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(p(-1, 0), CircuitNode.lever(true));
        for (int x = 0; x < 512; x++) nodes.put(p(x, 0), CircuitNode.gate(x % 2 == 0 ? AND : OR, EAST));
        nodes.put(p(512, 0), CircuitNode.lamp());
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(512, 0)));
        nodes.remove(p(-1, 0));
        assertFalse(evaluator.evaluate(nodes).lamps().get(p(512, 0)));
    }

    @Test void selfFeedbackSettlesFromOffOnEverySnapshotWithoutLatching() {
        for (var kind : new CircuitNode.Kind[]{AND, OR}) {
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            nodes.put(p(0, 0), CircuitNode.gate(kind, EAST));
            for (var position : new BlockPosition[]{p(1, 0), p(1, 1), p(0, 1), p(-1, 1), p(-1, 0)}) {
                nodes.put(position, CircuitNode.cable());
            }
            assertFalse(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
            nodes.put(p(2, 0), CircuitNode.lever(true));
            assertTrue(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
            nodes.remove(p(2, 0));
            assertFalse(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
            assertTrue(evaluator.evaluate(nodes).poweredCables().isEmpty());
        }
    }

    @Test void cyclesAcrossMultipleGatesAreIndependentOfEnumerationAndPreviousPower() {
        var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(OR, EAST),
                p(2, 0), CircuitNode.gate(AND, SOUTH), p(2, 2), CircuitNode.gate(OR, WEST),
                p(0, 2), CircuitNode.gate(AND, NORTH), p(1, 0), CircuitNode.cable(),
                p(2, 1), CircuitNode.cable(), p(1, 2), CircuitNode.cable(), p(0, 1), CircuitNode.cable()));
        assertTrue(evaluator.evaluate(nodes).gateOutputs().values().stream().noneMatch(Boolean::booleanValue));
        nodes.put(p(-1, 0), CircuitNode.lever(true));
        var expected = evaluator.evaluate(nodes);
        assertTrue(expected.gateOutputs().values().stream().allMatch(Boolean::booleanValue));
        for (int seed = 0; seed < 20; seed++) {
            var entries = new ArrayList<>(nodes.entrySet());
            Collections.shuffle(entries, new Random(seed));
            var shuffled = new LinkedHashMap<BlockPosition, CircuitNode>();
            entries.forEach(e -> shuffled.put(e.getKey(), e.getValue()));
            assertEquals(expected, evaluator.evaluate(shuffled));
        }
        nodes.remove(p(-1, 0));
        assertTrue(evaluator.evaluate(nodes).gateOutputs().values().stream().noneMatch(Boolean::booleanValue));
    }

    @Test void unavailableInputFacesSuspendBothKindsUntilAnEmptySectionReturns() {
        for (var kind : new CircuitNode.Kind[]{AND, OR}) for (var output : HorizontalDirection.values()) {
            var unavailable = output.opposite();
            var source = HorizontalDirection.values()[(output.ordinal() + 1) & 3];
            var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(kind, output, unavailable.bit()),
                    p(0, 0).neighbor(source), CircuitNode.lever(true), p(0, 0).neighbor(output), CircuitNode.lamp()));
            var pending = evaluator.evaluate(nodes);
            assertEquals(Set.of(p(0, 0)), pending.pendingGates());
            assertFalse(pending.gateOutputs().get(p(0, 0)));
            assertFalse(pending.lamps().get(p(0, 0).neighbor(output)));
            nodes.put(p(0, 0), CircuitNode.gate(kind, output));
            var loaded = evaluator.evaluate(nodes);
            assertTrue(loaded.pendingGates().isEmpty());
            assertTrue(loaded.gateOutputs().get(p(0, 0)));
        }
    }

    @Test void pendingUpstreamGateIsStillAConnectedOffInput() {
        var result = evaluator.evaluate(Map.of(p(0, 0), CircuitNode.gate(OR, EAST, 1),
                p(1, 0), CircuitNode.gate(AND, EAST), p(1, -1), CircuitNode.lever(true)));
        assertFalse(result.gateOutputs().get(p(1, 0)));
        assertEquals(9, result.gateMasks().get(p(1, 0)));
    }

    @Test void gateResultsAreImmutable() {
        var result = evaluator.evaluate(Map.of(p(0, 0), CircuitNode.gate(AND, EAST, 1)));
        assertThrows(UnsupportedOperationException.class, () -> result.gateOutputs().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.gateMasks().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.pendingGates().clear());
    }
}
