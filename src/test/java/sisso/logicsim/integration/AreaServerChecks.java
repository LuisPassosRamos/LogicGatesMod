package sisso.logicsim.integration;

import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.prefab.config.SelectionPrefabSerializer;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.classroom.learning.*;
import sisso.logicsim.classroom.learning.AreaSessions.Bounds;
import sisso.logicsim.interactions.ToggleLogicLeverInteraction;
import sisso.logicsim.util.*;
import java.lang.foreign.Arena;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Native proof only in the runner's disposable universe; client rendering is a separate acceptance. */
final class AreaServerChecks {
    private final List<String> passed;
    private final GateServerChecks gates;
    private final SubmissionFiles files;
    private final Map<String,Object> measurements=new LinkedHashMap<>();
    private AreaSnapshot maximumSnapshot;
    private static final LearningLimits LIMITS=LearningLimits.DEFAULT;
    private static final Bounds SOURCE=new Bounds(29,96,-16,9,2,12);
    private static final Bounds TARGET=new Bounds(1,128,-16,9,2,12);
    AreaServerChecks(List<String> passed,Path data) {
        this.passed=passed;gates=new GateServerChecks(passed);files=new SubmissionFiles(data.resolve("area-proof"),LIMITS);
    }
    Map<String,Object> measurements(){return Map.copyOf(measurements);}
    CompletableFuture<Void> run(World world) {
        fixture(world);
        return gates.afterTicks(world,()->{}).thenCompose(ignored->gates.inWorld(world,()->{
            long start=System.nanoTime();var snapshot=NativeAreaAdapter.capture(world,SOURCE,LIMITS);
            measurements.put("smallCaptureMs",millis(start));
            check(snapshot.blocks().stream().anyMatch(b->Boolean.TRUE.equals(b.leverOn())),"on lever captured");
            var nativeSelection=NativeAreaAdapter.selection(snapshot);
            var nativeDocument=SelectionPrefabSerializer.serialize(nativeSelection);
            var nativeRestored=SelectionPrefabSerializer.deserialize(nativeDocument);
            check(nativeRestored.getBlockCount()==snapshot.blocks().size(),"native selection codec preserves blocks");
            var preview=NativeAreaAdapter.preview(snapshot);
            check(preview.bounds!=null && preview.blocksChange.length==snapshot.blocks().size(),"bounded native preview packet");
            try(var arena=Arena.ofConfined()) {
                var memory=arena.allocate(preview.computeSize());int bytes=preview.serialize(memory,0);
                check(preview.equals(com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview.toObject(memory)),"native preview wire round trip");
                measurements.put("smallPreviewBytes",bytes);
            }
            expect(LearningException.Code.AREA_UNLOADED,()->NativeAreaAdapter.capture(world,new Bounds(100000,96,100000,1,1,1),LIMITS));
            passed.add("area proof: section boundary, native selection codec, preview wire and unloaded-region rejection");
            return snapshot;
        })).thenCompose(snapshot->CompletableFuture.supplyAsync(()->{
            long start=System.nanoTime();var saved=files.write(snapshot);var restored=files.read(saved.id(),saved.checksum());
            check(snapshot.equals(restored),"private content reopened exactly");measurements.put("smallFileBytes",saved.bytes());measurements.put("smallFileRoundTripMs",millis(start));return restored;
        })).thenCompose(snapshot->gates.inWorld(world,()->{
            long start=System.nanoTime();NativeAreaAdapter.paste(world,snapshot,TARGET);measurements.put("smallPasteMs",millis(start));
            expect(LearningException.Code.AREA_OCCUPIED,()->NativeAreaAdapter.paste(world,snapshot,TARGET));
            return snapshot;
        })).thenCompose(snapshot->gates.afterTicks(world,()->{
            var copy=NativeAreaAdapter.capture(world,TARGET,LIMITS);
            for(var original:snapshot.blocks()) {
                var block=copy.blocks().stream().filter(b->b.x()==original.x() && b.y()==original.y() && b.z()==original.z()).findFirst().orElseThrow();
                check(block.rotation()==original.rotation(),"orientation copied");
                check(LogicBlockTypes.kind(com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap().getAsset(block.asset()))
                        ==LogicBlockTypes.kind(com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap().getAsset(original.asset())),"circuit type copied");
                check(Objects.equals(block.leverOn(),original.leverOn()),"lever intent copied");
            }
            check(LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world,p(TARGET,4,1,0))).startsWith("On"),"pasted live lamp recalculated");
            ToggleLogicLeverInteraction.toggle(world,p(TARGET,0,1,0));
            passed.add("area proof: fixed file, seven gate types, all horizontal orientations and on/off lever intent survive paste");
        })).thenCompose(ignored->gates.afterTicks(world,()->{
            check(!LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world,p(TARGET,4,1,0))).startsWith("On"),"copy reacts independently");
            check(LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world,p(SOURCE,4,1,0))).startsWith("On"),"source unchanged by copy");
            gates.place(world,p(TARGET,8,1,11),"Rock_Stone");
            NativeAreaAdapter.clear(world,TARGET);NativeAreaAdapter.requireEmpty(world,TARGET);
            passed.add("area proof: independent circuit simulation and discard removes edits throughout leased bounds");
            NativeAreaAdapter.clear(world,SOURCE);
            maximum(world);
        })).thenCompose(ignored->CompletableFuture.runAsync(()->{
            long start=System.nanoTime();var saved=files.write(maximumSnapshot);files.read(saved.id(),saved.checksum());
            measurements.put("maxFileBytes",saved.bytes());measurements.put("maxFileRoundTripMs",millis(start));
            measurements.put("maxRawBytes",new com.google.gson.Gson().toJson(maximumSnapshot).getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
        })).thenCompose(ignored->simulation(world));
    }
    private void fixture(World world) {
        for(int x=0;x<SOURCE.width();x++)for(int z=0;z<SOURCE.depth();z++)gates.place(world,p(SOURCE,x,0,z),"Rock_Stone");
        gates.place(world,p(SOURCE,0,1,0),LogicBlockTypes.LEVER_ID);
        for(int x=1;x<4;x++)gates.place(world,p(SOURCE,x,1,0),LogicBlockTypes.CABLE_ID);
        gates.place(world,p(SOURCE,4,1,0),LogicBlockTypes.LAMP_ID);
        ToggleLogicLeverInteraction.toggle(world,p(SOURCE,0,1,0));
        int z=2;
        for(String id:List.of(LogicBlockTypes.AND_ID,LogicBlockTypes.OR_ID,LogicBlockTypes.NOT_ID,LogicBlockTypes.NAND_ID,LogicBlockTypes.NOR_ID,LogicBlockTypes.XOR_ID,LogicBlockTypes.XNOR_ID)) {
            int x=0;for(var rotation:Rotation.NORMAL)gates.place(world,p(SOURCE,x++*2,1,z),id,RotationTuple.of(rotation,Rotation.None,Rotation.None).index());z++;
        }
        gates.place(world,p(SOURCE,8,1,10),LogicBlockTypes.LEVER_ID);
    }
    private void maximum(World world) {
        var source=new Bounds(0,96,-32,32,16,32);var target=new Bounds(32,96,-32,32,16,32);
        for(int y=0;y<source.height();y++)for(int z=0;z<source.depth();z++)for(int x=0;x<source.width();x++)gates.place(world,p(source,x,y,z),"Rock_Stone");
        var allocationBean=(com.sun.management.ThreadMXBean)java.lang.management.ManagementFactory.getThreadMXBean();
        long allocatedBefore=allocationBean.isThreadAllocatedMemorySupported()?allocationBean.getThreadAllocatedBytes(Thread.currentThread().threadId()):-1;
        long memoryBefore=usedMemory(),start=System.nanoTime();var snapshot=NativeAreaAdapter.capture(world,source,LIMITS);maximumSnapshot=snapshot;measurements.put("maxCaptureMs",millis(start));
        var preview=NativeAreaAdapter.preview(snapshot);measurements.put("maxPreviewBytes",preview.computeSize());
        start=System.nanoTime();NativeAreaAdapter.paste(world,snapshot,target);measurements.put("maxPasteMs",millis(start));
        measurements.put("maxTransientHeapDeltaBytes",Math.max(0,usedMemory()-memoryBefore));
        if(allocatedBefore>=0)measurements.put("maxWorldThreadAllocatedBytes",allocationBean.getThreadAllocatedBytes(Thread.currentThread().threadId())-allocatedBefore);
        start=System.nanoTime();NativeAreaAdapter.clear(world,target);measurements.put("maxDiscardMs",millis(start));
        NativeAreaAdapter.clear(world,source);
        passed.add("area proof: maximum 16384-position capture, preview, empty-space paste and complete discard measured");
    }
    private CompletableFuture<Void> simulation(World world) {
        var source=new Bounds(0,96,-32,32,16,32);var target=new Bounds(32,96,-32,32,16,32);
        return gates.inWorld(world,()->{
            for(int layer=0;layer<16;layer+=2){
                for(int z=0;z<32;z++)for(int x=0;x<32;x++){
                    gates.place(world,p(source,x,layer,z),"Rock_Stone");gates.place(world,p(source,x,layer+1,z),LogicBlockTypes.CABLE_ID);
                }
                gates.place(world,p(source,0,layer+1,0),LogicBlockTypes.LEVER_ID);gates.place(world,p(source,31,layer+1,31),LogicBlockTypes.LAMP_ID);
                ToggleLogicLeverInteraction.toggle(world,p(source,0,layer+1,0));
            }
            return null;
        }).thenCompose(ignored->gates.afterTicks(world,()->{
            long start=System.nanoTime();var snapshot=NativeAreaAdapter.capture(world,source,LIMITS);measurements.put("maxCircuitCaptureMs",millis(start));
            start=System.nanoTime();NativeAreaAdapter.paste(world,snapshot,target);measurements.put("maxCircuitPasteMs",millis(start));NativeAreaAdapter.clear(world,source);
        })).thenCompose(ignored->gates.afterTicks(world,()->{
            check(LogicBlockTypes.isOnState(LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world,p(target,31,1,31)))),"copied 8192-node stack participates in native simulation");
            var state=world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
            var timing=state.metrics.summary();measurements.put("withCopyNodes",state.nodeCount);measurements.put("withCopyLastEvaluationMs",timing.lastMs());
            for(int layer=0;layer<16;layer+=2)ToggleLogicLeverInteraction.toggle(world,p(target,0,layer+1,0));
        })).thenCompose(ignored->gates.afterTicks(world,()->{
            check(!LogicBlockTypes.isOnState(LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world,p(target,31,1,31)))),"copied grid responds to teacher edit");
            NativeAreaAdapter.clear(world,target);
            passed.add("area proof: 8192-node maximum-volume correction copy runs in actual ticks, responds independently and is fully discarded");
        }));
    }
    private static BlockPosition p(Bounds b,int x,int y,int z){return new BlockPosition(b.x()+x,b.y()+y,b.z()+z);}
    private static double millis(long start){return (System.nanoTime()-start)/1_000_000.0;}
    private static long usedMemory(){return Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory();}
    private static void check(boolean b,String message){if(!b)throw new AssertionError(message);}
    private static void expect(LearningException.Code code,Runnable action){try{action.run();throw new AssertionError("Expected "+code);}catch(LearningException e){check(e.code()==code,"expected "+code+", got "+e.code());}}
}
