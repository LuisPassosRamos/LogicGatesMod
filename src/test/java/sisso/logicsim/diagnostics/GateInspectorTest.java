package sisso.logicsim.diagnostics;

import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.circuit.CircuitNode.Kind.*;
import static sisso.logicsim.circuit.HorizontalDirection.*;
import static sisso.logicsim.diagnostics.GateInspection.*;

class GateInspectorTest {
    private static final BlockPosition CENTER = new BlockPosition(0, 64, 0);
    private GateInspection inspect(Map<BlockPosition, CircuitNode> nodes) {
        return GateInspector.inspect(new CircuitSnapshot(nodes, new CircuitEvaluator().evaluate(nodes)), CENTER).orElseThrow();
    }

    @Test void labelsFollowAllFourRotationsAndSeparateEmptyFromZero() {
        var expectedA = Map.of(EAST, NORTH, SOUTH, EAST, WEST, SOUTH, NORTH, WEST);
        for (var output : HorizontalDirection.values()) {
            var nodes = new HashMap<BlockPosition, CircuitNode>();
            nodes.put(CENTER, CircuitNode.gate(AND, output));
            nodes.put(CENTER.neighbor(expectedA.get(output)), CircuitNode.lever(true));
            nodes.put(CENTER.neighbor(expectedA.get(output).opposite()), CircuitNode.cable());
            var gate = inspect(nodes);
            assertEquals(output, gate.outputDirection());
            assertEquals(List.of("A", "B", "C"), gate.inputs().stream().map(Input::label).toList());
            assertEquals(List.of(InputState.ON, InputState.OFF, InputState.ABSENT), gate.inputs().stream().map(Input::state).toList());
            assertEquals(Reason.LOGIC, gate.reason());
            assertFalse(gate.outputOn());
            nodes.remove(CENTER.neighbor(expectedA.get(output).opposite()));
            assertTrue(inspect(nodes).outputOn(), "removing an input 0 legitimately turns AND on");
        }
    }

    @Test void distinguishesNoInputIncompatibleFaceAndUnavailableSection() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(CENTER, CircuitNode.gate(OR, EAST));
        assertEquals(Reason.NO_INPUTS, inspect(nodes).reason());
        nodes.put(CENTER.neighbor(WEST), CircuitNode.gate(AND, WEST));
        assertEquals(InputState.INCOMPATIBLE, inspect(nodes).inputs().get(2).state());
        nodes.put(CENTER, CircuitNode.gate(OR, EAST, WEST.bit()));
        assertEquals(InputState.UNAVAILABLE, inspect(nodes).inputs().get(2).state());
        assertEquals(Reason.SECTION_UNAVAILABLE, inspect(nodes).reason());
    }

    @Test void notOnlyReportsRearAndReadsDirectGateOutputs() {
        var nodes = Map.of(CENTER, CircuitNode.gate(NOT, EAST), CENTER.neighbor(WEST), CircuitNode.gate(NOR, EAST),
                CENTER.neighbor(WEST).neighbor(WEST), CircuitNode.lever(false));
        var gate = inspect(nodes);
        assertEquals(1, gate.inputs().size());
        assertEquals("C", gate.inputs().getFirst().label());
        assertEquals(InputState.ON, gate.inputs().getFirst().state());
        assertFalse(gate.outputOn());
    }

    @Test void repeatedNetworkIsReportedOncePerPhysicalInputAndExternalOutputPowerIsIsolated() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(CENTER, CircuitNode.gate(XOR, EAST));
        for (var position : List.of(new BlockPosition(0,64,-1),new BlockPosition(-1,64,-1),
                new BlockPosition(-1,64,0),new BlockPosition(-1,64,1),new BlockPosition(0,64,1))) nodes.put(position,CircuitNode.cable());
        nodes.put(new BlockPosition(-2,64,0),CircuitNode.lever(true));
        var gate = inspect(nodes);
        assertEquals(3, gate.inputs().stream().filter(i -> i.state()==InputState.ON).count());
        assertTrue(gate.outputOn());
        nodes.remove(CENTER.neighbor(SOUTH));
        assertFalse(inspect(nodes).outputOn());
        nodes.put(CENTER.neighbor(EAST), CircuitNode.cable());
        nodes.put(new BlockPosition(2,64,0),CircuitNode.lever(true));
        assertFalse(inspect(nodes).outputOn());
    }

    @Test void unresolvedInvertingFeedbackHasItsOwnReason() {
        var nodes = new HashMap<BlockPosition, CircuitNode>();
        nodes.put(CENTER,CircuitNode.gate(NOT,EAST));
        for (var position : List.of(new BlockPosition(1,64,0),new BlockPosition(1,64,1),new BlockPosition(0,64,1),
                new BlockPosition(-1,64,1),new BlockPosition(-1,64,0))) nodes.put(position,CircuitNode.cable());
        assertEquals(Reason.INDETERMINATE_FEEDBACK,inspect(nodes).reason());
        assertFalse(inspect(nodes).outputOn());
    }

    @Test void snapshotIsImmutableAndNonGateTargetsAreRejected() {
        var nodes = new HashMap<BlockPosition,CircuitNode>();
        nodes.put(CENTER,CircuitNode.gate(AND,EAST));
        var snapshot = new CircuitSnapshot(nodes,new CircuitEvaluator().evaluate(nodes));
        nodes.clear();
        assertTrue(GateInspector.inspect(snapshot,CENTER).isPresent());
        assertThrows(UnsupportedOperationException.class,()->snapshot.nodes().clear());
        assertTrue(GateInspector.inspect(CircuitSnapshot.empty(),CENTER).isEmpty());
    }

    @Test void helpTablesMatchIndependentTwoInputExpectedResults() {
        var expected=Map.of(AND,"0001",OR,"0111",NAND,"1110",NOR,"1000",XOR,"0110",XNOR,"1001");
        expected.forEach((kind,pattern)->assertEquals(pattern,GateTruthTable.rows(kind,2).stream()
                .map(row->row.output()?"1":"0").collect(java.util.stream.Collectors.joining())));
        assertEquals(List.of(true,false),GateTruthTable.rows(NOT,1).stream().map(GateTruthTable.Row::output).toList());
        assertEquals(List.of(false,true,true,false,true,false,false,true),GateTruthTable.rows(XOR,3).stream().map(GateTruthTable.Row::output).toList());
        assertThrows(IllegalArgumentException.class,()->GateTruthTable.rows(NOT,2));
        assertThrows(IllegalArgumentException.class,()->GateTruthTable.rows(AND,0));
    }
}
