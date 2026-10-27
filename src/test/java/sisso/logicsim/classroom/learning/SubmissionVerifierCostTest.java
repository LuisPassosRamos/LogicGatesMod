package sisso.logicsim.classroom.learning;

import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.*;
import sisso.logicsim.util.BuildInfo;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Records bounded offline work, without asserting a machine-dependent latency or player capacity. */
class SubmissionVerifierCostTest {
    @Test void measureSmallAndMaximumLogicalSubmissions() throws Exception{
        var nodes=new HashMap<BlockPosition,CircuitNode>();
        for(int i=0;i<6;i++){
            nodes.put(new BlockPosition(0,0,i*4),CircuitNode.lever(false));nodes.put(new BlockPosition(1,0,i*4),CircuitNode.cable());
            if(i<4)nodes.put(new BlockPosition(2,0,i*4),CircuitNode.lamp());
        }
        outer:for(int y=1;y<4;y++)for(int z=0;z<32;z++)for(int x=0;x<32;x++){
            if(nodes.size()==VerificationRules.MAX_COMPONENTS)break outer;nodes.put(new BlockPosition(x,y,z),CircuitNode.cable());
        }
        var table=new StringBuilder();for(int i=0;i<64;i++){
            var bits=String.format("%6s",Integer.toBinaryString(i)).replace(' ','0');table.append(bits).append(" -> ").append(bits,0,4).append('\n');
        }
        var rules=new VerificationRules(Map.of(),table.toString());var verifier=new SubmissionVerifier();
        verifier.verify(new VerificationRules(Map.of(),SubmissionVerifierTest.AND),SubmissionVerifierTest.and());
        var rounds=new ArrayList<Map<String,Object>>();
        for(int round=1;round<=3;round++){
            long start=System.nanoTime();var small=verifier.verify(new VerificationRules(Map.of(),SubmissionVerifierTest.AND),SubmissionVerifierTest.and());double smallMs=(System.nanoTime()-start)/1_000_000.0;
            var bean=java.lang.management.ManagementFactory.getThreadMXBean();
            var allocations=bean instanceof com.sun.management.ThreadMXBean b && b.isThreadAllocatedMemorySupported()?b:null;
            long before=allocations==null?-1:allocations.getThreadAllocatedBytes(Thread.currentThread().threadId());
            start=System.nanoTime();var maximum=verifier.verify(rules,nodes);double maxMs=(System.nanoTime()-start)/1_000_000.0;
            long allocated=before<0?-1:allocations.getThreadAllocatedBytes(Thread.currentThread().threadId())-before;
            assertEquals(4,small.cases().size());assertEquals(64,maximum.cases().size());assertEquals(2048,nodes.size());
            assertTrue(maximum.cases().stream().allMatch(c->c.outcome()==SubmissionVerifier.Outcome.MATCH));
            rounds.add(Map.of("round",round,"smallMs",smallMs,"maximumMs",maxMs,"maximumAllocatedBytes",allocated));
        }
        Path report=Path.of("build/reports/submission-verification.json");Files.createDirectories(report.getParent());
        Files.writeString(report,new com.google.gson.GsonBuilder().setPrettyPrinting().create().toJson(Map.of("buildId",BuildInfo.BUILD_ID,"smallNodes",7,"maxNodes",2048,"maxCases",64,"rounds",rounds)));
    }
}
