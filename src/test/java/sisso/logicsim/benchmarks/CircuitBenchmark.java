package sisso.logicsim.benchmarks;

import com.google.gson.GsonBuilder;
import sisso.logicsim.circuit.*;
import sisso.logicsim.util.BuildInfo;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/** Repeatable evaluator baseline; intentionally has no timing assertions or Hytale world access. */
public final class CircuitBenchmark {
    private static final CircuitEvaluator EVALUATOR = new CircuitEvaluator();
    private static volatile CircuitEvaluator.Result sink;
    private static final int WARMUP = 10, SAMPLES = 25;

    private static Map<BlockPosition,CircuitNode> cableLine(int size) {
        var nodes=new HashMap<BlockPosition,CircuitNode>();
        nodes.put(new BlockPosition(0,64,0),CircuitNode.lever(true));
        for(int x=1;x<=size;x++) nodes.put(new BlockPosition(x,64,0),CircuitNode.cable());
        nodes.put(new BlockPosition(size+1,64,0),CircuitNode.lamp());
        return nodes;
    }

    private static Map<BlockPosition,CircuitNode> grid(int size) {
        var nodes=new HashMap<BlockPosition,CircuitNode>();
        int width=(int)Math.sqrt(size);
        for(int i=0;i<size;i++) nodes.put(new BlockPosition(i%width,64,i/width),CircuitNode.cable());
        nodes.put(new BlockPosition(-1,64,0),CircuitNode.lever(true));
        return nodes;
    }

    private static Map<BlockPosition,CircuitNode> gates(int size) {
        var nodes=new HashMap<BlockPosition,CircuitNode>();
        var kinds=new CircuitNode.Kind[]{CircuitNode.Kind.AND,CircuitNode.Kind.OR,CircuitNode.Kind.NOT,
                CircuitNode.Kind.NAND,CircuitNode.Kind.NOR,CircuitNode.Kind.XOR,CircuitNode.Kind.XNOR};
        nodes.put(new BlockPosition(-1,64,0),CircuitNode.lever(true));
        for(int x=0;x<size;x++) nodes.put(new BlockPosition(x,64,0),CircuitNode.gate(kinds[x%kinds.length],HorizontalDirection.EAST));
        return nodes;
    }

    private static Map<BlockPosition,CircuitNode> independentNetworks(int size) {
        var nodes=new HashMap<BlockPosition,CircuitNode>();
        for(int i=0;i<size;i++) nodes.put(new BlockPosition(i*3,64,0),CircuitNode.cable());
        nodes.put(new BlockPosition(-1,64,0),CircuitNode.lever(true));
        return nodes;
    }

    private static Map<String,Object> measure(String scenario,int size,Map<BlockPosition,CircuitNode> nodes) {
        for(int i=0;i<WARMUP;i++) sink=EVALUATOR.evaluate(nodes);
        var bean=ManagementFactory.getThreadMXBean();
        var allocations=bean instanceof com.sun.management.ThreadMXBean b && b.isThreadAllocatedMemorySupported()?b:null;
        if(allocations!=null && !allocations.isThreadAllocatedMemoryEnabled()) allocations.setThreadAllocatedMemoryEnabled(true);
        long thread=Thread.currentThread().threadId();
        long before=allocations==null?0:allocations.getThreadAllocatedBytes(thread);
        long[] nanos=new long[SAMPLES];
        for(int i=0;i<SAMPLES;i++) {
            long start=System.nanoTime();
            sink=EVALUATOR.evaluate(nodes);
            nanos[i]=System.nanoTime()-start;
        }
        long allocated=allocations==null?-1:(allocations.getThreadAllocatedBytes(thread)-before)/SAMPLES;
        Arrays.sort(nanos);
        var result=new LinkedHashMap<String,Object>();
        result.put("scenario",scenario);result.put("size",size);result.put("nodes",nodes.size());
        result.put("samples",SAMPLES);result.put("medianMs",nanos[SAMPLES/2]/1_000_000d);
        result.put("p95Ms",nanos[(int)Math.ceil(SAMPLES*.95)-1]/1_000_000d);
        result.put("meanMs",Arrays.stream(nanos).average().orElse(0)/1_000_000);
        result.put("allocatedBytesPerEvaluation",allocated);
        System.out.printf(Locale.ROOT,"%-21s %,6d nodes: median %.3f ms; p95 %.3f ms%n",scenario,nodes.size(),result.get("medianMs"),result.get("p95Ms"));
        return result;
    }

    public static void main(String[] args) throws Exception {
        var cases=new ArrayList<Map<String,Object>>();
        for(int size:new int[]{100,1_000,10_000}) {
            cases.add(measure("cable-line",size,cableLine(size)));
            cases.add(measure("cable-grid",size,grid(size)));
            cases.add(measure("mixed-gate-chain",size,gates(size)));
            cases.add(measure("independent-networks",size,independentNetworks(size)));
        }
        var report=new LinkedHashMap<String,Object>();
        report.put("build",BuildInfo.BUILD_ID);report.put("recordedAt",Instant.now().toString());
        report.put("java",System.getProperty("java.runtime.version"));report.put("os",System.getProperty("os.name"));
        report.put("processors",Runtime.getRuntime().availableProcessors());report.put("maxHeapBytes",Runtime.getRuntime().maxMemory());
        report.put("warmupIterations",WARMUP);report.put("samplesPerCase",SAMPLES);report.put("cases",cases);
        report.put("scope","Evaluator only; excludes Hytale tick/section discovery, publication, client rendering and multiple players.");
        var output=Path.of(args.length==0?"build/reports/benchmarks/circuit.json":args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        Files.writeString(output,new GsonBuilder().setPrettyPrinting().create().toJson(report)+"\n");
    }
}
