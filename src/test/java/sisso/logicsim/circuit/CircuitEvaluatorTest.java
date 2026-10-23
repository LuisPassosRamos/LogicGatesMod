package sisso.logicsim.circuit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import static org.junit.jupiter.api.Assertions.*;

class CircuitEvaluatorTest {
    private final CircuitEvaluator evaluator = new CircuitEvaluator();
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 10, z); }
    private static Map<BlockPosition, CircuitNode> line(boolean on) {
        return new HashMap<>(Map.of(p(0, 0), CircuitNode.lever(on),
                p(1, 0), CircuitNode.cable(), p(2, 0), CircuitNode.cable(),
                p(3, 0), CircuitNode.lamp()));
    }

    @Test void followsTheSourceInBothDirectionsWithoutRememberingOldPower() {
        var nodes = line(true);
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(3, 0)));
        nodes.put(p(0, 0), CircuitNode.lever(false));
        var off = evaluator.evaluate(nodes);
        assertFalse(off.lamps().get(p(3, 0)));
        assertTrue(off.poweredCables().isEmpty());
        nodes.put(p(0, 0), CircuitNode.lever(true));
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(3, 0)));
    }

    @Test void aLampCanBeDirectlyAdjacentToALever() {
        var result = evaluator.evaluate(Map.of(p(0, 0), CircuitNode.lever(true), p(0, -1), CircuitNode.lamp()));
        assertTrue(result.lamps().get(p(0, -1)));
    }

    @Test void bendsAndBranchesReachEveryLamp() {
        var nodes = line(true);
        nodes.put(p(2, 1), CircuitNode.cable());
        nodes.put(p(2, 2), CircuitNode.cable());
        nodes.put(p(1, 2), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertEquals(Map.of(p(3, 0), true, p(1, 2), true), result.lamps());
        assertEquals(14, result.cableMasks().get(p(2, 0)));
        assertEquals(9, result.cableMasks().get(p(2, 2)));
    }

    @Test void removingAndReplacingTheMiddleCableSplitsAndReconnectsTheNetwork() {
        var nodes = line(true);
        nodes.remove(p(2, 0));
        var split = evaluator.evaluate(nodes);
        assertFalse(split.lamps().get(p(3, 0)));
        assertEquals(8, split.cableMasks().get(p(1, 0)));
        nodes.put(p(2, 0), CircuitNode.cable());
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(3, 0)));
    }

    @Test void removingTheLastSourceTurnsEverythingOff() {
        var nodes = line(true);
        nodes.remove(p(0, 0));
        var result = evaluator.evaluate(nodes);
        assertFalse(result.lamps().get(p(3, 0)));
        assertTrue(result.poweredCables().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void multipleSourcesUseOr(int inputs) {
        var nodes = line((inputs & 1) != 0);
        nodes.put(p(1, -1), CircuitNode.lever((inputs & 2) != 0));
        assertEquals(inputs != 0, evaluator.evaluate(nodes).lamps().get(p(3, 0)));
        if ((inputs & 2) != 0) {
            nodes.remove(p(0, 0));
            assertTrue(evaluator.evaluate(nodes).lamps().get(p(3, 0)));
        }
    }

    @Test void cableRingsTerminateAndDoNotLatchPower() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        for (int x = 0; x < 3; x++) {
            nodes.put(p(x, 0), CircuitNode.cable());
            nodes.put(p(x, 2), CircuitNode.cable());
        }
        nodes.put(p(0, 1), CircuitNode.cable());
        nodes.put(p(2, 1), CircuitNode.cable());
        nodes.put(p(-1, 0), CircuitNode.lever(true));
        nodes.put(p(3, 2), CircuitNode.lamp());
        assertEquals(8, evaluator.evaluate(nodes).poweredCables().size());
        nodes.remove(p(-1, 0));
        assertFalse(evaluator.evaluate(nodes).lamps().get(p(3, 2)));
    }

    @Test void lampsDoNotBridgeSeparateCableNetworks() {
        var nodes = line(true);
        nodes.put(p(2, 0), CircuitNode.lamp());
        nodes.put(p(3, 0), CircuitNode.cable());
        nodes.put(p(4, 0), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertTrue(result.lamps().get(p(2, 0)));
        assertFalse(result.lamps().get(p(4, 0)));
    }

    @Test void disabledLeversDoNotBridgeSeparateCableNetworks() {
        var nodes = line(true);
        nodes.put(p(2, 0), CircuitNode.lever(false));
        nodes.put(p(3, 0), CircuitNode.cable());
        nodes.put(p(4, 0), CircuitNode.lamp());
        assertFalse(evaluator.evaluate(nodes).lamps().get(p(4, 0)));
        nodes.put(p(2, 0), CircuitNode.lever(true));
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(4, 0)));
    }

    @Test void diagonalsAndVerticalNeighborsDoNotConnect() {
        var nodes = Map.of(p(0, 0), CircuitNode.lever(true),
                p(1, 1), CircuitNode.cable(), p(2, 1), CircuitNode.lamp(),
                new BlockPosition(0, 11, 0), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertTrue(result.poweredCables().isEmpty());
        assertTrue(result.lamps().values().stream().noneMatch(Boolean::booleanValue));
    }

    @Test void allSixteenVisualMasksIncludeDisabledTerminals() {
        for (int mask = 0; mask < 16; mask++) {
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            nodes.put(p(0, 0), CircuitNode.cable());
            for (var direction : HorizontalDirection.values()) {
                if ((mask & direction.bit()) != 0) {
                    nodes.put(p(0, 0).neighbor(direction), CircuitNode.lever(false));
                }
            }
            var result = evaluator.evaluate(nodes);
            assertEquals(mask, result.cableMasks().get(p(0, 0)));
            assertTrue(result.poweredCables().isEmpty());
        }
    }

    @Test void terminalsPointToAllSixteenNeighborCombinationsEvenWhenUnpowered() {
        for (var terminal : new CircuitNode[]{CircuitNode.lever(false), CircuitNode.lamp()}) {
            for (int mask = 0; mask < 16; mask++) {
                var nodes = new HashMap<BlockPosition, CircuitNode>();
                var center = p(0, 0);
                nodes.put(center, terminal);
                for (var direction : HorizontalDirection.values()) {
                    if ((mask & direction.bit()) != 0) nodes.put(center.neighbor(direction), CircuitNode.cable());
                }
                var result = evaluator.evaluate(nodes);
                assertEquals(mask, result.terminalMasks().get(center));
                nodes.remove(center.neighbor(HorizontalDirection.NORTH));
                assertEquals(mask & ~1, evaluator.evaluate(nodes).terminalMasks().get(center));
            }
        }
    }

    @Test void directTerminalConnectionsAreReciprocalWithoutJoiningEqualKinds() {
        var center = p(0, 0);
        var result = evaluator.evaluate(Map.of(center, CircuitNode.lever(true),
                p(1, 0), CircuitNode.lamp(), p(-1, 0), CircuitNode.lever(false),
                p(2, 0), CircuitNode.lamp()));
        assertEquals(2, result.terminalMasks().get(center));
        assertEquals(8, result.terminalMasks().get(p(1, 0)));
        assertEquals(0, result.terminalMasks().get(p(2, 0)));
        assertEquals(0, result.terminalMasks().get(p(-1, 0)));
        assertTrue(result.lamps().get(p(1, 0)));
        assertFalse(result.lamps().get(p(2, 0)));
    }

    @Test void crossingsJoinEveryBranch() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(p(0, 0), CircuitNode.cable());
        nodes.put(p(0, -1), CircuitNode.lever(true));
        nodes.put(p(1, 0), CircuitNode.lamp());
        nodes.put(p(0, 1), CircuitNode.lamp());
        nodes.put(p(-1, 0), CircuitNode.lamp());
        var result = evaluator.evaluate(nodes);
        assertEquals(15, result.cableMasks().get(p(0, 0)));
        assertTrue(result.lamps().values().stream().allMatch(Boolean::booleanValue));
    }

    @Test void addingALampToAnAlreadyPoweredNetworkUpdatesItsOutput() {
        var nodes = line(true);
        evaluator.evaluate(nodes);
        nodes.put(p(1, -1), CircuitNode.lamp());
        assertTrue(evaluator.evaluate(nodes).lamps().get(p(1, -1)));
    }

    @Test void negativeCoordinatesAndSectionBoundariesDoNotAffectConnectivity() {
        for (int baseX : new int[]{-65, -33, -1, 31, 63}) {
            var translated = new HashMap<BlockPosition, CircuitNode>();
            line(true).forEach((pos, node) -> translated.put(
                    new BlockPosition(pos.x() + baseX, -32, pos.z() - 33), node));
            assertTrue(evaluator.evaluate(translated).lamps().get(new BlockPosition(baseX + 3, -32, -33)));
        }
    }

    @Test void resultsAreIndependentOfEnumerationAndPreviousWorlds() {
        var nodes = line(true);
        var expected = evaluator.evaluate(nodes);
        for (int seed = 0; seed < 20; seed++) {
            var entries = new ArrayList<>(nodes.entrySet());
            Collections.shuffle(entries, new Random(seed));
            var shuffled = new LinkedHashMap<BlockPosition, CircuitNode>();
            entries.forEach(entry -> shuffled.put(entry.getKey(), entry.getValue()));
            assertEquals(expected, evaluator.evaluate(shuffled));
        }
        assertFalse(evaluator.evaluate(line(false)).lamps().get(p(3, 0)));
        assertEquals(expected, evaluator.evaluate(nodes));
    }

    @Test void snapshotsAreNotMutatedAndResultsAreImmutable() {
        var nodes = Map.copyOf(line(true));
        var result = evaluator.evaluate(nodes);
        assertEquals(4, nodes.size());
        assertThrows(UnsupportedOperationException.class, () -> result.lamps().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.cableMasks().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.terminalMasks().clear());
        assertThrows(UnsupportedOperationException.class, () -> result.poweredCables().clear());
    }
}
