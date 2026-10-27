package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import sisso.logicsim.classroom.learning.*;
import sisso.logicsim.classroom.learning.LearningViews.Submission;
import sisso.logicsim.classroom.learning.LearningViews.Publication;
import sisso.logicsim.circuit.*;
import sisso.logicsim.circuit.CircuitNode.Kind;
import java.util.*;
import static sisso.logicsim.util.LogicMessages.text;

/** Localized presentation only. Reports never become scores or overwrite grade-editor fields. */
final class LearningVerification {
    private LearningVerification() { }
    static void render(UICommandBuilder c,VerificationRules rules){
        for(var kind:Kind.values())c.set("#Required"+kind+".Value",rules.quantities().containsKey(kind)?Integer.toString(rules.quantities().get(kind)):"");
        c.set("#TruthTableField.Value",rules.table());
    }
    static VerificationRules rules(String[] fields,String table){
        var counts=new EnumMap<Kind,Integer>(Kind.class);var kinds=Kind.values();
        for(int i=0;i<kinds.length;i++){var count=VerificationRules.quantity(fields[i]);if(count!=null)counts.put(kinds[i],count);}
        return new VerificationRules(counts,table);
    }
    static Message module(Kind kind){return switch(kind){
        case LEVER->text("learning.moduleLever");case CABLE->text("learning.moduleCable");case LAMP->text("learning.moduleLamp");
        default->Message.raw(kind.name());};}
    static Message summary(VerificationRules rules){
        var result=text("learning.requirements").insert("\n");
        if(rules.quantities().isEmpty())result.insert(text("learning.noRestrictions"));
        else rules.quantities().forEach((kind,count)->result.insert(module(kind)).insert(": "+count+"\n"));
        return result;
    }
    static Message table(VerificationRules rules){return text("learning.expectedTable").insert("\n").insert(rules.table());}
    static Message deliveredCriteria(Publication version){
        var result=text("learning.deliveredCriteria").param("version",version.number()).insert("\n").insert(summary(version.rules()));
        if(!version.rules().table().isEmpty())result.insert("\n").insert(table(version.rules()));
        return result;
    }
    static Message report(SubmissionVerifier.Report report,Submission submission){
        var result=text("learning.verificationReport").param("revision",submission.revision()).param("version",submission.publication()).insert("\n");
        for(var component:report.components())result.insert(text("learning.componentResult")
                .param("module",module(component.kind())).param("actual",component.actual())
                .param("required",component.required()==null?text("learning.unrestricted"):Message.raw(component.required().toString()))
                .param("result",text(component.matches()?"learning.checkMatch":"learning.checkMismatch"))).insert("\n");
        result.insert(text("learning.portOrder")).insert("\n");
        ports(result,report.inputs(),"I",VerificationRules.MAX_INPUTS);ports(result,report.outputs(),"O",VerificationRules.MAX_OUTPUTS);
        if(report.status()==SubmissionVerifier.Status.COUNTS_ONLY)return result.insert(text("learning.countsOnly"));
        if(report.status()==SubmissionVerifier.Status.IO_MISMATCH)return result.insert(text("learning.ioMismatch"));
        long passed=report.cases().stream().filter(r->r.outcome()==SubmissionVerifier.Outcome.MATCH).count();
        result.insert(text("learning.caseSummary").param("passed",passed).param("total",report.cases().size())).insert("\n");
        for(var row:report.cases())result.insert(text("learning.caseResult").param("inputs",row.inputs()).param("expected",row.expected()).param("actual",row.actual())
                .param("result",outcome(row.outcome()))).insert("\n");
        return result.insert(text("learning.manualGradeNotice"));
    }
    private static void ports(Message result,List<BlockPosition> ports,String prefix,int limit){
        for(int i=0;i<Math.min(limit,ports.size());i++){var p=ports.get(i);result.insert(prefix+(i+1)+" @ ("+p.x()+", "+p.y()+", "+p.z()+")\n");}
    }
    private static Message outcome(SubmissionVerifier.Outcome outcome){return text(switch(outcome){
        case MATCH->"learning.checkMatch";case MISMATCH->"learning.checkMismatch";
        case UNSTABLE->"learning.checkUnstable";case DISCONNECTED->"learning.checkDisconnected";});}
}
