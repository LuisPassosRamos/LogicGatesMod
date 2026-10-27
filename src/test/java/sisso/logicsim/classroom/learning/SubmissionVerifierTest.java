package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.*;
import sisso.logicsim.circuit.CircuitNode.Kind;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.circuit.HorizontalDirection.*;
import static sisso.logicsim.classroom.learning.SubmissionVerifier.*;

class SubmissionVerifierTest {
    static final String AND="00 -> 0\n01 -> 0\n10 -> 0\n11 -> 1";
    static BlockPosition p(int x,int z){return new BlockPosition(x,0,z);}
    static Map<BlockPosition,CircuitNode> and(){
        return new HashMap<>(Map.of(p(0,-2),CircuitNode.lever(true),p(0,-1),CircuitNode.cable(),
                p(0,0),CircuitNode.gate(Kind.AND,EAST),p(0,1),CircuitNode.cable(),p(0,2),CircuitNode.lever(false),
                p(1,0),CircuitNode.cable(),p(2,0),CircuitNode.lamp()));
    }
    @Test void completeTableReusesDirectionalSimulationWithoutChangingSavedLeverIntent(){
        var nodes=and();var before=Map.copyOf(nodes);
        var report=new SubmissionVerifier().verify(new VerificationRules(Map.of(Kind.AND,1,Kind.LEVER,2),AND),nodes);
        assertEquals(Status.COMPLETE,report.status());assertEquals(4,report.cases().size());
        assertTrue(report.cases().stream().allMatch(c->c.outcome()==Outcome.MATCH));assertEquals(before,nodes);
        assertEquals(List.of(p(0,-2),p(0,2)),report.inputs());assertEquals(List.of(p(2,0)),report.outputs());
    }
    @Test void equivalentCircuitPassesButForbiddenOrExtraComponentsRemainVisible(){
        var nodes=and();nodes.put(p(0,0),CircuitNode.gate(Kind.NAND,EAST));
        nodes.put(p(2,0),CircuitNode.gate(Kind.NOT,EAST));nodes.put(p(3,0),CircuitNode.cable());nodes.put(p(4,0),CircuitNode.lamp());
        var report=new SubmissionVerifier().verify(new VerificationRules(Map.of(Kind.AND,0,Kind.NAND,1,Kind.NOT,1),AND),nodes);
        assertTrue(report.cases().stream().allMatch(c->c.outcome()==Outcome.MATCH));assertTrue(report.components().stream().allMatch(Component::matches));
        nodes.put(p(9,9),CircuitNode.gate(Kind.AND,EAST));
        report=new SubmissionVerifier().verify(new VerificationRules(Map.of(Kind.AND,0),AND),nodes);
        assertFalse(report.components().stream().filter(c->c.kind()==Kind.AND).findFirst().orElseThrow().matches());
    }
    @Test void countDivergencesAndBehaviorDivergencesAreIndependent(){
        var report=new SubmissionVerifier().verify(new VerificationRules(Map.of(Kind.AND,2,Kind.OR,0),AND.replace("11 -> 1","11 -> 0")),and());
        assertEquals(1,report.components().stream().filter(c->!c.matches()).count());
        assertEquals(1,report.cases().stream().filter(c->c.outcome()==Outcome.MISMATCH).count());
    }
    @Test void missingPortsAreNotInventedAndDisconnectedPortsAreInconclusive(){
        var nodes=and();nodes.remove(p(2,0));
        assertEquals(Status.IO_MISMATCH,new SubmissionVerifier().verify(new VerificationRules(Map.of(),AND),nodes).status());
        nodes.put(p(8,8),CircuitNode.lamp());
        assertTrue(new SubmissionVerifier().verify(new VerificationRules(Map.of(),AND),nodes).cases().stream().allMatch(c->c.outcome()==Outcome.DISCONNECTED));
    }
    @Test void unresolvedFeedbackIsNotAnIncorrectLogicalAnswer(){
        var nodes=and();nodes.put(p(4,4),CircuitNode.gate(Kind.NOT,EAST));
        for(var pos:List.of(p(5,4),p(5,5),p(4,5),p(3,5),p(3,4)))nodes.put(pos,CircuitNode.cable());
        assertTrue(new SubmissionVerifier().verify(new VerificationRules(Map.of(),AND),nodes).cases().stream().allMatch(c->c.outcome()==Outcome.UNSTABLE));
    }
    @Test void multipleOutputsAndLocalOrderingAreDeterministic(){
        var nodes=new HashMap<BlockPosition,CircuitNode>();
        nodes.put(p(0,0),CircuitNode.lever(false));nodes.put(p(1,0),CircuitNode.cable());nodes.put(p(2,0),CircuitNode.lamp());
        nodes.put(p(0,4),CircuitNode.lever(true));nodes.put(p(1,4),CircuitNode.cable());nodes.put(p(2,4),CircuitNode.lamp());
        var report=new SubmissionVerifier().verify(new VerificationRules(Map.of(),"00 -> 00\n01 -> 01\n10 -> 10\n11 -> 11"),nodes);
        assertTrue(report.cases().stream().allMatch(c->c.outcome()==Outcome.MATCH));
        assertEquals(List.of(p(0,0),p(0,4)),report.inputs());
    }
    @Test void countsOnlyDoesNotSimulateAndComplexityLimitDoesNotDeleteOrGradeAnything(){
        var nodes=and();for(int i=0;i<2050;i++)nodes.put(p(i+20,10),CircuitNode.cable());
        assertEquals(Status.COUNTS_ONLY,new SubmissionVerifier().verify(new VerificationRules(Map.of(Kind.LEVER,2),""),nodes).status());
        assertEquals(LearningException.Code.VERIFICATION_LIMIT,assertThrows(LearningException.class,()->new SubmissionVerifier().verify(new VerificationRules(Map.of(),AND),nodes)).code());
        assertTrue(nodes.get(p(0,-2)).sourceOn());
    }
}
