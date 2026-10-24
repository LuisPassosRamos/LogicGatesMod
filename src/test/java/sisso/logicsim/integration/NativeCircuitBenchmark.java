package sisso.logicsim.integration;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.interactions.ToggleLogicLeverInteraction;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BlockComponentAccess;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/** Opt-in world-thread workload; no players, rendering or artificial timing thresholds. */
final class NativeCircuitBenchmark {
    private static final int WARMUP = 10, SAMPLES = 25;
    private final List<BlockPosition> placed = new ArrayList<>();
    private final List<BlockPosition> sources = new ArrayList<>();
    private final List<BlockPosition> lamps = new ArrayList<>();
    private final List<Map<String, Object>> cases = new ArrayList<>();

    CompletableFuture<Map<String, Object>> run(World world) {
        var chunks = new ArrayList<CompletableFuture<?>>();
        for (int x = 0; x < 128; x += 32) for (int z = 64; z < 192; z += 32) {
            chunks.add(world.getChunkStore().getChunkSectionReferenceAtBlockAsync(x, 64, z));
            chunks.add(world.getChunkStore().getChunkSectionReferenceAtBlockAsync(x, 63, z));
        }
        return CompletableFuture.allOf(chunks.toArray(CompletableFuture[]::new)).thenCompose(ignored -> {
            var result = new CompletableFuture<Map<String, Object>>();
            world.execute(() -> {
                try { result.complete(measureWorld(world)); }
                catch (Throwable error) { result.completeExceptionally(error); }
            });
            return result;
        });
    }

    private Map<String, Object> measureWorld(World world) {
        measure(world, "loaded-empty-sections", () -> {}, 0);
        for (int count : new int[]{16, 128}) {
            clear(world);
            for (int i = 0; i < count; i++) {
                int x = 2 + (i % 16) * 6, z = 66 + (i / 16) * 4;
                var source = new BlockPosition(x, 64, z);
                sources.add(source);
                var lamp = new BlockPosition(x + 3, 64, z);
                lamps.add(lamp);
                place(world, source, "Logic_Lever");
                place(world, new BlockPosition(x + 1, 64, z), "Logic_Cable");
                place(world, new BlockPosition(x + 2, 64, z), "Logic_AND_Gate");
                place(world, lamp, "Logic_Lamp");
            }
            measure(world, "independent-networks-" + count, () -> sources.forEach(p ->
                    ToggleLogicLeverInteraction.toggle(world, p)), count * 4);
            // Only one of 128 independent networks changes; the current evaluator still
            // rebuilds the complete loaded world. Report this explicitly for comparison.
            if (count == 128) measure(world, "one-source-in-128-networks", () ->
                    ToggleLogicLeverInteraction.toggle(world, sources.getFirst()), count * 4);
        }
        clear(world);
        var source = new BlockPosition(1, 64, 66);
        sources.add(source);
        place(world, source, "Logic_Lever");
        for (int i = 0; i < 1024; i++) place(world,
                new BlockPosition(2 + i % 64, 64, 66 + i / 64), "Logic_Cable");
        measure(world, "connected-grid-1024", () -> ToggleLogicLeverInteraction.toggle(world, source), 1025);
        for (int i = 0; i < 128; i++) place(world,
                new BlockPosition(2 + i % 16 * 6, 64, 130 + i / 16 * 4), "Logic_Workbench");
        measure(world, "grid-with-128-unrelated-benches", () ->
                ToggleLogicLeverInteraction.toggle(world, source), 1025);
        var result = new LinkedHashMap<String, Object>();
        result.put("recordedAt", Instant.now().toString());
        result.put("java", System.getProperty("java.runtime.version"));
        result.put("os", System.getProperty("os.name"));
        result.put("processors", Runtime.getRuntime().availableProcessors());
        result.put("maxHeapBytes", Runtime.getRuntime().maxMemory());
        result.put("warmupIterations", WARMUP);
        result.put("samplesPerCase", SAMPLES);
        result.put("cases", cases);
        result.put("scope", "Real loaded Hytale sections and block entities on the world thread; synchronous "
                + "polling and source-toggle/collection/solver/publication timings. No players, network, "
                + "client rendering, tick scheduling latency or capacity assertion. Timed workloads run in one world task.");
        return result;
    }

    private void measure(World world, String scenario, Runnable change, int expectedNodes) {
        var store = world.getChunkStore().getStore();
        var state = store.getResource(LogicSimPlugin.getCircuitStateType());
        var system = new EnergyPropagationSystem();
        for (int i = 0; i < 3; i++) {
            system.tick(1 / 30f, 0, store);
            EnergyPropagationSystem.recalculate(world);
        }
        if (state.nodeCount != expectedNodes) throw new AssertionError("Native benchmark node count: " + scenario);
        var row = new LinkedHashMap<String, Object>();
        row.put("scenario", scenario);
        row.put("nodes", state.nodeCount);
        row.put("gates", state.gateCount);
        row.put("loadedSections", state.sections.size());
        int parked = state.parkedNodes.values().stream().mapToInt(Map::size).sum();
        int allHolders = state.sections.stream().filter(ref -> ref.isValid())
                .map(ref -> store.getComponent(ref, BlockComponentSection.getComponentType()))
                .filter(Objects::nonNull).map(BlockComponentSection::getBlockHolders)
                .filter(Objects::nonNull).mapToInt(Map::size).sum();
        row.put("parkedLogicHolders", parked);
        row.put("nonLogicParkedHolders", allHolders - parked);
        row.put("liveLogicalNodes", state.liveNodes.size());
        row.put("monitoring", timed(() -> system.tick(1 / 30f, 0, store)));
        if (state.dirty) throw new AssertionError("Idle polling invalidated an unchanged native fixture: " + scenario);
        row.put("toggleAndEvaluate", timed(() -> { change.run(); EnergyPropagationSystem.recalculate(world); }));
        if (!lamps.isEmpty()) {
            boolean on = BlockComponentAccess.getIfLoaded(world, sources.getFirst(),
                    LogicSimPlugin.getLeverComponentType()).isOn();
            if (sisso.logicsim.util.LogicBlockTypes.isOnState(sisso.logicsim.util.LogicBlockTypes.stateName(
                    BlockComponentAccess.getBlockType(world, lamps.getFirst()))) != on)
                throw new AssertionError("Native benchmark did not publish the changed lamp");
        }
        cases.add(row);
    }

    private Map<String, Object> timed(Runnable action) {
        for (int i = 0; i < WARMUP; i++) action.run();
        var bean = ManagementFactory.getThreadMXBean();
        var allocations = bean instanceof com.sun.management.ThreadMXBean b
                && b.isThreadAllocatedMemorySupported() ? b : null;
        if (allocations != null && !allocations.isThreadAllocatedMemoryEnabled())
            allocations.setThreadAllocatedMemoryEnabled(true);
        long thread = Thread.currentThread().threadId();
        long before = allocations == null ? 0 : allocations.getThreadAllocatedBytes(thread);
        long[] times = new long[SAMPLES];
        for (int i = 0; i < SAMPLES; i++) {
            long start = System.nanoTime();
            action.run();
            times[i] = System.nanoTime() - start;
        }
        long bytes = allocations == null ? -1 : (allocations.getThreadAllocatedBytes(thread) - before) / SAMPLES;
        Arrays.sort(times);
        return Map.of("medianMs", times[SAMPLES / 2] / 1_000_000d,
                "p95Ms", times[(int) Math.ceil(SAMPLES * .95) - 1] / 1_000_000d,
                "allocatedBytesPerOperation", bytes);
    }

    private void clear(World world) {
        for (var position : List.copyOf(placed)) set(world, position, "Empty");
        placed.clear(); sources.clear(); lamps.clear();
    }

    private void place(World world, BlockPosition p, String id) {
        set(world, new BlockPosition(p.x(), 63, p.z()), "Rock_Stone");
        set(world, p, id);
        placed.add(p);
    }

    private void set(World world, BlockPosition p, String id) {
        BlockOperations.setBlock(world.getChunkStore(), BlockComponentAccess.sectionIfLoaded(world, p),
                p.x(), p.y(), p.z(), BlockType.getAssetMap().getIndex(id), BlockType.getAssetMap().getAsset(id), 0, 0, 0);
    }
}
