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

class InvertingGateTest {
    private final CircuitEvaluator evaluator = new CircuitEvaluator();
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 10, z); }

    static Stream<Arguments> notCases() {
        var cases = new ArrayList<Arguments>();
        for (var direction : HorizontalDirection.values()) for (int input = 0; input < 3; input++) {
            cases.add(Arguments.of(direction, input));
        }
        return cases.stream();
    }

    @ParameterizedTest(name = "NOT output {0}, absent/off/on input {1}")
    @MethodSource("notCases")
    void notInvertsOnlyItsRearInputAndClosesBothSideFaces(HorizontalDirection output, int inputState) {
        for (boolean cables : new boolean[]{false, true}) {
            var center = p(0, 0);
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            nodes.put(center, CircuitNode.gate(NOT, output));
            for (var direction : HorizontalDirection.values()) {
                if (direction == output || direction == output.opposite()) continue;
                nodes.put(center.neighbor(direction), CircuitNode.cable());
                nodes.put(center.neighbor(direction).neighbor(direction), CircuitNode.lever(true));
            }
            if (inputState != 0) {
                var source = center.neighbor(output.opposite());
                if (cables) {
                    nodes.put(source, CircuitNode.cable());
                    source = source.neighbor(output.opposite());
                }
                nodes.put(source, CircuitNode.lever(inputState == 2));
            }
            var lamp = center.neighbor(output);
            if (cables) {
                nodes.put(lamp, CircuitNode.cable());
                lamp = lamp.neighbor(output);
            }
            nodes.put(lamp, CircuitNode.lamp());
            var result = evaluator.evaluate(nodes);
            assertEquals(inputState == 1, result.gateOutputs().get(center));
            assertEquals(inputState == 1, result.lamps().get(lamp));
            assertEquals(output.bit() | (inputState == 0 ? 0 : output.opposite().bit()), result.gateMasks().get(center));
            for (var direction : HorizontalDirection.values()) {
                if (direction == output || direction == output.opposite()) continue;
                assertEquals(0, result.cableMasks().get(center.neighbor(direction)) & direction.opposite().bit());
            }
            assertTrue(result.feedbackGates().isEmpty());
        }
    }

    @Test void mixedInverterChainsEvaluateInDependencyOrderAndForgetPreviousPower() {
        var nodes = new HashMap<>(Map.of(p(-1, 0), CircuitNode.lever(false), p(0, 0), CircuitNode.gate(NOT, EAST),
                p(1, 0), CircuitNode.cable(), p(2, 0), CircuitNode.gate(NAND, EAST),
                p(3, 0), CircuitNode.gate(NOR, EAST), p(4, 0), CircuitNode.gate(NOT, EAST),
                p(5, 0), CircuitNode.lamp()));
        for (boolean on : new boolean[]{false, true, false}) {
            nodes.put(p(-1, 0), CircuitNode.lever(on));
            var result = evaluator.evaluate(nodes);
            assertEquals(!on, result.gateOutputs().get(p(0, 0)));
            assertEquals(on, result.gateOutputs().get(p(2, 0)));
            assertEquals(!on, result.gateOutputs().get(p(3, 0)));
            assertEquals(on, result.lamps().get(p(5, 0)));
        }
    }

    @Test void nandAndNorMatchAndOrFollowedByNotForEveryNonemptyInputConfiguration() {
        for (var kind : new CircuitNode.Kind[]{NAND, NOR}) for (int code = 1; code < 27; code++) {
            var inputs = new HashMap<BlockPosition, CircuitNode>();
            int value = code;
            for (var direction : new HorizontalDirection[]{NORTH, SOUTH, WEST}) {
                int input = value % 3;
                value /= 3;
                if (input != 0) inputs.put(p(0, 0).neighbor(direction), CircuitNode.lever(input == 2));
            }
            inputs.put(p(0, 0), CircuitNode.gate(kind, EAST));
            boolean expected = evaluator.evaluate(inputs).gateOutputs().get(p(0, 0));
            inputs.put(p(0, 0), CircuitNode.gate(kind == NAND ? AND : OR, EAST));
            inputs.put(p(1, 0), CircuitNode.gate(NOT, EAST));
            assertEquals(expected, evaluator.evaluate(inputs).gateOutputs().get(p(1, 0)));
        }
    }

    private Map<BlockPosition, CircuitNode> loop(CircuitNode.Kind kind) {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(p(0, 0), CircuitNode.gate(kind, EAST));
        for (var position : new BlockPosition[]{p(1, 0), p(1, 1), p(0, 1), p(-1, 1), p(-1, 0)}) {
            nodes.put(position, CircuitNode.cable());
        }
        return nodes;
    }

    @Test void anUnresolvedNotLoopIsDisabledWithoutOscillationOrPowerRetention() {
        var nodes = loop(NOT);
        var unresolved = evaluator.evaluate(nodes);
        assertEquals(Set.of(p(0, 0)), unresolved.feedbackGates());
        assertFalse(unresolved.gateOutputs().get(p(0, 0)));
        assertTrue(unresolved.poweredCables().isEmpty());
        nodes.put(p(2, 0), CircuitNode.lever(true));
        var forced = evaluator.evaluate(nodes);
        assertTrue(forced.feedbackGates().isEmpty());
        assertFalse(forced.gateOutputs().get(p(0, 0)));
        assertEquals(5, forced.poweredCables().size());
        nodes.remove(p(2, 0));
        assertEquals(unresolved, evaluator.evaluate(nodes));
    }

    @Test void externalInputsCanResolveNandAndNorFeedbackWithoutDisablingThem() {
        for (var kind : new CircuitNode.Kind[]{NAND, NOR}) {
            var nodes = loop(kind);
            nodes.put(p(0, -1), CircuitNode.lever(kind == NOR));
            var resolved = evaluator.evaluate(nodes);
            assertTrue(resolved.feedbackGates().isEmpty());
            assertEquals(kind == NAND, resolved.gateOutputs().get(p(0, 0)));
            nodes.put(p(0, -1), CircuitNode.lever(kind != NOR));
            var unresolved = evaluator.evaluate(nodes);
            assertEquals(Set.of(p(0, 0)), unresolved.feedbackGates());
            assertFalse(unresolved.gateOutputs().get(p(0, 0)));
        }
    }

    @Test void bistableFeedbackRequiresExternalInformationAndNeverRestoresALatchState() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(p(0, 0), CircuitNode.gate(NOT, EAST));
        nodes.put(p(2, 2), CircuitNode.gate(NOT, EAST));
        for (var wire : new BlockPosition[]{p(1, 0), p(1, 1), p(1, 2), p(3, 2), p(4, 2), p(4, 1),
                p(4, 0), p(4, -1), p(4, -2), p(3, -2), p(2, -2), p(1, -2), p(0, -2),
                p(-1, -2), p(-1, -1), p(-1, 0)}) nodes.put(wire, CircuitNode.cable());
        var unseeded = evaluator.evaluate(nodes);
        assertEquals(Set.of(p(0, 0), p(2, 2)), unseeded.feedbackGates());
        nodes.put(p(0, 1), CircuitNode.lever(true));
        var seeded = evaluator.evaluate(nodes);
        assertTrue(seeded.feedbackGates().isEmpty());
        assertTrue(seeded.gateOutputs().get(p(0, 0)));
        assertFalse(seeded.gateOutputs().get(p(2, 2)));
        for (int seed = 0; seed < 20; seed++) {
            var entries = new ArrayList<>(nodes.entrySet());
            Collections.shuffle(entries, new Random(seed));
            var shuffled = new LinkedHashMap<BlockPosition, CircuitNode>();
            entries.forEach(e -> shuffled.put(e.getKey(), e.getValue()));
            assertEquals(seeded, evaluator.evaluate(shuffled));
        }
        nodes.remove(p(0, 1));
        assertEquals(unseeded, evaluator.evaluate(nodes));
    }

    @Test void positiveCyclesAndUnrelatedOutputsContinueWorkingBesideAnUnresolvedInverter() {
        var nodes = loop(NOT);
        nodes.put(p(10, 0), CircuitNode.gate(OR, EAST));
        for (var wire : new BlockPosition[]{p(11, 0), p(11, 1), p(10, 1), p(9, 1), p(9, 0)}) {
            nodes.put(wire, CircuitNode.cable());
        }
        nodes.put(p(12, 0), CircuitNode.lever(true));
        nodes.put(p(11, 2), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertEquals(Set.of(p(0, 0)), result.feedbackGates());
        assertTrue(result.gateOutputs().get(p(10, 0)));
        assertTrue(result.lamps().get(p(11, 2)));
    }

    @Test void longInverterChainsSettleWithoutRecursiveStackOrRetainedValues() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        for (int x = 0; x < 1024; x++) nodes.put(p(x, 0), CircuitNode.gate(NOT, EAST));
        nodes.put(p(1024, 0), CircuitNode.lamp());
        for (boolean on : new boolean[]{false, true, false}) {
            nodes.put(p(-1, 0), CircuitNode.lever(on));
            assertEquals(on, evaluator.evaluate(nodes).lamps().get(p(1024, 0)));
        }
    }

    @Test void unavailableRearSuspendsNotAndOtherClosedSidesDoNotCountAsInputs() {
        var nodes = new HashMap<>(Map.of(p(0, 0), CircuitNode.gate(NOT, EAST, 8),
                p(1, 0), CircuitNode.lamp(), p(0, -1), CircuitNode.lever(true)));
        var blocked = evaluator.evaluate(nodes);
        assertFalse(blocked.gateOutputs().get(p(0, 0)));
        assertEquals(Set.of(p(0, 0)), blocked.pendingGates());
        assertTrue(blocked.feedbackGates().isEmpty());
        nodes.put(p(0, 0), CircuitNode.gate(NOT, EAST));
        nodes.put(p(-1, 0), CircuitNode.cable());
        assertTrue(evaluator.evaluate(nodes).gateOutputs().get(p(0, 0)));
        assertThrows(IllegalArgumentException.class, () -> CircuitNode.gate(NOT, EAST, NORTH.bit()));
    }

    @Test void outputPowerNeverBackfeedsAnInverterAndFeedbackResultsAreImmutable() {
        var nodes = Map.of(p(0, 0), CircuitNode.gate(NOT, EAST), p(-1, 0), CircuitNode.lever(true),
                p(1, 0), CircuitNode.cable(), p(2, 0), CircuitNode.lever(true), p(1, 1), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertFalse(result.gateOutputs().get(p(0, 0)));
        assertTrue(result.lamps().get(p(1, 1)));
        assertThrows(UnsupportedOperationException.class, () -> result.feedbackGates().clear());
    }
}
