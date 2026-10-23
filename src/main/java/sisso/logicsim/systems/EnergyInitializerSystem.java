package sisso.logicsim.systems;

import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import org.joml.Vector3i;

/** Observes committed ECS changes, including physics removal and section reloads. */
public final class EnergyInitializerSystem extends RefSystem<ChunkStore> {
    private final Query<ChunkStore> query = Query.and(BlockModule.BlockStateInfo.getComponentType(),
            Query.or(LogicSimPlugin.getLeverComponentType(), LogicSimPlugin.getLampComponentType(),
                    LogicSimPlugin.getCableComponentType(), LogicSimPlugin.getGateComponentType()));

    @Override
    public Query<ChunkStore> getQuery() { return query; }

    @Override
    public void onEntityAdded(Ref<ChunkStore> ref, AddReason reason, Store<ChunkStore> store,
                              CommandBuffer<ChunkStore> buffer) {
        var info = buffer.getComponent(ref, BlockModule.BlockStateInfo.getComponentType());
        var position = new Vector3i();
        var state = store.getResource(LogicSimPlugin.getCircuitStateType());
        if (info != null && info.fillWorldPos(buffer, position)) {
            state.liveNodes.put(ref, new BlockPosition(position.x, position.y, position.z));
        }
        state.invalidate();
    }

    @Override
    public void onEntityRemove(Ref<ChunkStore> ref, RemoveReason reason, Store<ChunkStore> store,
                               CommandBuffer<ChunkStore> buffer) {
        var state = store.getResource(LogicSimPlugin.getCircuitStateType());
        state.liveNodes.remove(ref);
        state.invalidate();
    }

    /** Parked holders also belong to loaded sections and must participate in circuits. */
    public static final class LoadedSections extends RefSystem<ChunkStore> {
        private final Query<ChunkStore> query = Query.and(ChunkSection.getComponentType(),
                BlockComponentSection.getComponentType());

        @Override
        public Query<ChunkStore> getQuery() { return query; }

        @Override
        public void onEntityAdded(Ref<ChunkStore> ref, AddReason reason, Store<ChunkStore> store,
                                  CommandBuffer<ChunkStore> buffer) {
            var state = store.getResource(LogicSimPlugin.getCircuitStateType());
            state.sections.add(ref);
            state.invalidate();
        }

        @Override
        public void onEntityRemove(Ref<ChunkStore> ref, RemoveReason reason, Store<ChunkStore> store,
                                   CommandBuffer<ChunkStore> buffer) {
            var state = store.getResource(LogicSimPlugin.getCircuitStateType());
            state.sections.remove(ref);
            state.invalidate();
        }
    }
}
