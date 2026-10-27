package sisso.logicsim.classroom.learning;

import sisso.logicsim.circuit.*;
import sisso.logicsim.circuit.CircuitNode.Kind;
import java.util.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.VERIFICATION_LIMIT;

/** Bounded, stateless assistance. Uses only the immutable submitted topology, never a live world. */
public final class SubmissionVerifier {
    public enum Status { COUNTS_ONLY, IO_MISMATCH, COMPLETE }
    public enum Outcome { MATCH, MISMATCH, UNSTABLE, DISCONNECTED }
    public record Component(Kind kind,Integer required,int actual){public boolean matches(){return required==null || required==actual;}}
    public record CaseResult(String inputs,String expected,String actual,Outcome outcome) { }
    public record Report(List<Component> components,List<BlockPosition> inputs,List<BlockPosition> outputs,List<CaseResult> cases,Status status){
        public Report{components=List.copyOf(components);inputs=List.copyOf(inputs);outputs=List.copyOf(outputs);cases=List.copyOf(cases);}
    }
    public Report verify(VerificationRules rules,Map<BlockPosition,CircuitNode> source){
        var nodes=new HashMap<>(source);var counts=new EnumMap<Kind,Integer>(Kind.class);
        nodes.values().forEach(n->counts.merge(n.kind(),1,Integer::sum));
        var components=Arrays.stream(Kind.values()).map(k->new Component(k,rules.quantities().get(k),counts.getOrDefault(k,0))).toList();
        var order=Comparator.comparingInt(BlockPosition::x).thenComparingInt(BlockPosition::y).thenComparingInt(BlockPosition::z);
        var inputs=nodes.entrySet().stream().filter(e->e.getValue().kind()==Kind.LEVER).map(Map.Entry::getKey).sorted(order).toList();
        var outputs=nodes.entrySet().stream().filter(e->e.getValue().kind()==Kind.LAMP).map(Map.Entry::getKey).sorted(order).toList();
        var table=rules.cases();
        if(table.isEmpty())return new Report(components,inputs,outputs,List.of(),Status.COUNTS_ONLY);
        if(inputs.size()!=table.getFirst().inputs().length() || outputs.size()!=table.getFirst().outputs().length())
            return new Report(components,inputs,outputs,List.of(),Status.IO_MISMATCH);
        if(nodes.size()>VerificationRules.MAX_COMPONENTS)throw new LearningException(VERIFICATION_LIMIT);
        var cases=new ArrayList<CaseResult>();var evaluator=new CircuitEvaluator();
        for(var row:table){
            if(Thread.currentThread().isInterrupted())throw new LearningException(VERIFICATION_LIMIT);
            for(int i=0;i<inputs.size();i++)nodes.put(inputs.get(i),CircuitNode.lever(row.inputs().charAt(i)=='1'));
            var result=evaluator.evaluate(nodes);var actual=new StringBuilder();
            outputs.forEach(p->actual.append(Boolean.TRUE.equals(result.lamps().get(p))?'1':'0'));
            boolean disconnected=inputs.stream().anyMatch(p->result.terminalMasks().getOrDefault(p,0)==0)
                    || outputs.stream().anyMatch(p->result.terminalMasks().getOrDefault(p,0)==0);
            Outcome outcome=!result.feedbackGates().isEmpty() || !result.pendingGates().isEmpty()?Outcome.UNSTABLE:
                    disconnected?Outcome.DISCONNECTED:row.outputs().contentEquals(actual)?Outcome.MATCH:Outcome.MISMATCH;
            cases.add(new CaseResult(row.inputs(),row.outputs(),actual.toString(),outcome));
        }
        return new Report(components,inputs,outputs,cases,Status.COMPLETE);
    }
}
