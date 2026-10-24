package sisso.logicsim.systems;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.system.tick.TickingSystem;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMaps;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.CircuitEvaluator;
import sisso.logicsim.circuit.CircuitNode;
import sisso.logicsim.circuit.CircuitSnapshot;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;

/** Coalesces changes and evaluates after ECS processing, on the same world thread. */
public final class EnergyPropagationSystem extends TickingSystem<ChunkStore> {
    private static final CircuitEvaluator EVALUATOR = new CircuitEvaluator();

    @Override
    public void tick(float dt, int index, Store<ChunkStore> store) {
        var state = store.getResource(LogicSimPlugin.getCircuitStateType());
        World world = store.getExternalData().getWorld();
        long monitoringStarted = System.nanoTime();
        detectParkedChanges(store, state);
        for (var entry : state.gateTopologies.entrySet()) {
            if (!Objects.equals(entry.getValue(), BlockComponentAccess.gateTopologyIfLoaded(world, entry.getKey()))) {
                state.invalidate();
                break;
            }
        }
        state.metrics.recordMonitoring(System.nanoTime() - monitoringStarted);
        if (!state.dirty || state.scheduled) return;
        state.scheduled = true;
        world.execute(() -> {
            state.scheduled = false;
            if (!store.isShutdown()) recalculate(world);
        });
    }

    private static void detectParkedChanges(Store<ChunkStore> store, CircuitWorldState state) {
        // Parked holders are not live ECS entities: their addition/removal emits no RefSystem callback.
        // Compare only logic holder bindings in already-loaded sections; never scan block palettes.
        int seen = 0;
        for (var sectionRef : state.sections) {
            if (!sectionRef.isValid()) continue;
            var entities = store.getComponent(sectionRef, BlockComponentSection.getComponentType());
            var previous = state.parkedNodes.get(sectionRef);
            int count = 0;
            if (entities != null && entities.getBlockHolders() != null) {
                var iterator = Short2ObjectMaps.fastIterator(entities.getBlockHolders());
                while (iterator.hasNext()) {
                    var entry = iterator.next();
                    if (!isLogic(entry.getValue())) continue;
                    count++;
                    // Identity matters: replacing a holder at the same position/count
                    // must invalidate, including an electrically identical replacement.
                    if (previous == null || previous.get(entry.getShortKey()) != entry.getValue()) {
                        rebuildParkedIndex(store, state);
                        return;
                    }
                }
            }
            if (previous != null) {
                seen++;
                if (count != previous.size()) { rebuildParkedIndex(store, state); return; }
            }
        }
        if (seen != state.parkedNodes.size()) rebuildParkedIndex(store, state);
    }

    private static void rebuildParkedIndex(Store<ChunkStore> store, CircuitWorldState state) {
        // Allocate snapshots only after an actual binding change; idle ticks reuse them.
        state.parkedNodes.clear();
        for (var sectionRef : state.sections) {
            if (!sectionRef.isValid()) continue;
            var entities = store.getComponent(sectionRef, BlockComponentSection.getComponentType());
            if (entities == null || entities.getBlockHolders() == null) continue;
            Short2ObjectOpenHashMap<Holder<ChunkStore>> nodes = null;
            var iterator = Short2ObjectMaps.fastIterator(entities.getBlockHolders());
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (!isLogic(entry.getValue())) continue;
                if (nodes == null) nodes = new Short2ObjectOpenHashMap<>();
                nodes.put(entry.getShortKey(), entry.getValue());
            }
            if (nodes != null) state.parkedNodes.put(sectionRef, nodes);
        }
        state.invalidate();
    }

    private static boolean isLogic(Holder<ChunkStore> holder) {
        return holder.getComponent(LogicSimPlugin.getLeverComponentType()) != null
                || holder.getComponent(LogicSimPlugin.getLampComponentType()) != null
                || holder.getComponent(LogicSimPlugin.getCableComponentType()) != null
                || holder.getComponent(LogicSimPlugin.getGateComponentType()) != null;
    }

    public static void recalculate(World world) {
        long started = System.nanoTime();
        var store = world.getChunkStore().getStore();
        var state = store.getResource(LogicSimPlugin.getCircuitStateType());
        state.dirty = false;
        state.liveNodes.keySet().removeIf(ref -> !ref.isValid());
        var positions = new HashSet<>(state.liveNodes.values());
        state.sections.removeIf(ref -> !ref.isValid());
        for (var sectionRef : state.sections) {
            var section = store.getComponent(sectionRef, ChunkSection.getComponentType());
            var entities = store.getComponent(sectionRef, BlockComponentSection.getComponentType());
            if (section == null || entities == null || entities.getBlockHolders() == null) continue;
            for (var entry : entities.getBlockHolders().short2ObjectEntrySet()) {
                var holder = entry.getValue();
                if (!isLogic(holder)) continue;
                int blockIndex = entry.getShortKey() & 0xffff;
                positions.add(new BlockPosition(
                        (section.getX() << 5) + ChunkUtil.xFromIndex(blockIndex),
                        (section.getY() << 5) + ChunkUtil.yFromIndex(blockIndex),
                        (section.getZ() << 5) + ChunkUtil.zFromIndex(blockIndex)));
            }
        }

        var nodes = new HashMap<BlockPosition, CircuitNode>();
        state.gateTopologies.clear();
        for (var position : positions) {
            var type = BlockComponentAccess.getBlockType(world, position);
            var kind = LogicBlockTypes.kind(type);
            if (kind == null) continue;
            if (kind == CircuitNode.Kind.LEVER) {
                var lever = BlockComponentAccess.getIfLoaded(world, position,
                        LogicSimPlugin.getLeverComponentType());
                if (lever == null) continue;
                lever.restoreFromStoredVisualStateIfNeeded(type);
                nodes.put(position, CircuitNode.lever(lever.isOn()));
            } else if (kind.isGate()) {
                var topology = BlockComponentAccess.gateTopologyIfLoaded(world, position);
                if (topology == null || BlockComponentAccess.getIfLoaded(world, position,
                        LogicSimPlugin.getGateComponentType()) == null) continue;
                state.gateTopologies.put(position, topology);
                nodes.put(position, CircuitNode.gate(kind, topology.outputDirection(), topology.unavailableInputMask()));
            } else {
                nodes.put(position, new CircuitNode(kind, false));
            }
        }
        long solverStarted = System.nanoTime();
        var result = EVALUATOR.evaluate(nodes);
        long solverNanos = System.nanoTime() - solverStarted;
        result.terminalMasks().forEach((position, mask) -> {
            var node = nodes.get(position);
            boolean on = node.kind() == CircuitNode.Kind.LEVER ? node.sourceOn() : result.lamps().get(position);
            BlockComponentAccess.applyVisualState(world, position, LogicBlockTypes.terminalState(on,
                    BlockComponentAccess.localConnectionMask(world, position, mask)));
        });
        result.cableMasks().forEach((position, mask) -> BlockComponentAccess.applyVisualState(world, position,
                LogicBlockTypes.cableState(BlockComponentAccess.localConnectionMask(world, position, mask))));
        result.gateMasks().forEach((position, mask) -> BlockComponentAccess.applyVisualState(world, position,
                LogicBlockTypes.terminalState(result.gateOutputs().get(position),
                        BlockComponentAccess.localConnectionMask(world, position, mask))));
        state.nodeCount = nodes.size();
        state.poweredCableCount = result.poweredCables().size();
        state.litLampCount = (int) result.lamps().values().stream().filter(Boolean::booleanValue).count();
        state.gateCount = result.gateOutputs().size();
        state.activeGateCount = (int) result.gateOutputs().values().stream().filter(Boolean::booleanValue).count();
        state.pendingGateCount = result.pendingGates().size();
        state.feedbackGateCount = result.feedbackGates().size();
        state.lastSnapshot = new CircuitSnapshot(nodes, result);
        state.evaluations++;
        state.metrics.recordEvaluation(System.nanoTime() - started, solverNanos);
    }
}
