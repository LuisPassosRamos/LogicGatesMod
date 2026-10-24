package sisso.logicsim.integration;

import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BlockComponentAccess;
import java.util.List;

/** Parked-holder mutations do not emit live-entity callbacks; preserve detection by identity. */
final class ParkedIndexServerChecks {
    static void run(World world, List<String> passed) {
        var position = new BlockPosition(4, 62, 4);
        set(world, new BlockPosition(4, 61, 4), "Rock_Stone");
        set(world, position, "Logic_Cable");
        var store = world.getChunkStore().getStore();
        var section = BlockComponentAccess.sectionIfLoaded(world, position);
        var nonTicking = ChunkStore.REGISTRY.getNonTickingComponentType();
        boolean wasNonTicking = store.getComponent(section, nonTicking) != null;
        store.ensureComponent(section, nonTicking);
        var entities = store.getComponent(section, BlockComponentSection.getComponentType());
        short key = (short) ChunkUtil.indexBlock(position.x(), position.y(), position.z());
        var original = entities.getBlockHolder(key);
        if (original == null) throw new AssertionError("Parked fixture holder missing");
        var state = store.getResource(LogicSimPlugin.getCircuitStateType());
        var system = new EnergyPropagationSystem();
        system.tick(1 / 30f, 0, store);
        EnergyPropagationSystem.recalculate(world);
        var snapshot = state.parkedNodes.get(section);
        system.tick(1 / 30f, 0, store);
        if (state.dirty || state.parkedNodes.get(section) != snapshot)
            throw new AssertionError("Idle polling must reuse the parked snapshot");
        var replacement = original.clone();
        entities.loadBlockHolder(key, replacement);
        system.tick(1 / 30f, 0, store);
        if (!state.dirty || state.parkedNodes.get(section).get(key) != replacement)
            throw new AssertionError("Same-position/count holder replacement was missed");
        EnergyPropagationSystem.recalculate(world);
        entities.loadBlockHolder((short) (key + 1), ChunkStore.REGISTRY.newHolder());
        system.tick(1 / 30f, 0, store);
        if (state.dirty) throw new AssertionError("An unrelated holder invalidated electrical state");
        entities.removeBlockHolder((short) (key + 1));
        entities.removeBlockHolder(key);
        system.tick(1 / 30f, 0, store);
        if (!state.dirty || state.parkedNodes.get(section) != null)
            throw new AssertionError("Removal of the final parked logical holder was missed");
        entities.loadBlockHolder(key, replacement);
        system.tick(1 / 30f, 0, store);
        if (state.parkedNodes.get(section).get(key) != replacement)
            throw new AssertionError("Return of a holder to an empty section was missed");
        set(world, position, "Empty");
        if (!wasNonTicking) store.tryRemoveComponent(section, nonTicking);
        EnergyPropagationSystem.recalculate(world);
        passed.add("parked snapshot reuse, binding replacement, last removal/return and unrelated holders");
    }

    private static void set(World world, BlockPosition p, String id) {
        BlockOperations.setBlock(world.getChunkStore(), BlockComponentAccess.sectionIfLoaded(world, p),
                p.x(), p.y(), p.z(), BlockType.getAssetMap().getIndex(id), BlockType.getAssetMap().getAsset(id), 0, 0, 0);
    }
}
