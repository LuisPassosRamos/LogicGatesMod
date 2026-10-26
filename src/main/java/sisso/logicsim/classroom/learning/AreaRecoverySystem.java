package sisso.logicsim.classroom.learning;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

/** Retry durable cleanup when sections naturally become available; never poll or generate chunks. */
public final class AreaRecoverySystem extends RefSystem<ChunkStore> {
    private final AreaRuntime areas;
    public AreaRecoverySystem(AreaRuntime areas){this.areas=areas;}
    @Override public Query<ChunkStore> getQuery(){return ChunkSection.getComponentType();}
    @Override public void onEntityAdded(Ref<ChunkStore> ref,AddReason reason,Store<ChunkStore> store,CommandBuffer<ChunkStore> buffer){areas.recoverLoaded(store.getExternalData().getWorld());}
    @Override public void onEntityRemove(Ref<ChunkStore> ref,RemoveReason reason,Store<ChunkStore> store,CommandBuffer<ChunkStore> buffer){ }
}
