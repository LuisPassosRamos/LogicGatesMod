package sisso.logicsim.util;

import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockComponentSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.HorizontalDirection;
import sisso.logicsim.circuit.CircuitNode;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import org.joml.Vector3i;

/** Hytale 0.7.4 section access. Never loads sections; call only on the world thread. */
public final class BlockComponentAccess {
    private BlockComponentAccess() { }

    /** Also observes empty neighbor sections, which need not have block entities or lifecycle callbacks. */
    public record GateTopology(CircuitNode.Kind kind, int rotationIndex,
                               HorizontalDirection outputDirection, int unavailableInputMask) { }

    public static RotationTuple rotationIfLoaded(World world, BlockPosition position) {
        var section = sectionIfLoaded(world, position);
        if (section == null) return null;
        var blocks = world.getChunkStore().getStore().getComponent(section, BlockSection.getComponentType());
        return blocks == null ? null : blocks.getRotation(position.x(), position.y(), position.z());
    }

    public static GateTopology gateTopologyIfLoaded(World world, BlockPosition position) {
        var kind = LogicBlockTypes.kind(getBlockType(world, position));
        if (kind == null || !kind.isGate()) return null;
        var rotation = rotationIfLoaded(world, position);
        if (rotation == null) return null;
        var output = GateOrientation.horizontalOutput(rotation);
        // Assets allow NESW only; imported unsupported rotations remain safely unpowered.
        if (output == null) return new GateTopology(kind, rotation.index(), HorizontalDirection.EAST,
                CircuitNode.inputMask(kind, HorizontalDirection.EAST));
        int unavailable = 0;
        for (var direction : HorizontalDirection.values()) {
            if ((CircuitNode.inputMask(kind, output) & direction.bit()) != 0
                    && rotationIfLoaded(world, position.neighbor(direction)) == null) {
                unavailable |= direction.bit();
            }
        }
        return new GateTopology(kind, rotation.index(), output, unavailable);
    }

    public static Ref<ChunkStore> sectionIfLoaded(World world, BlockPosition position) {
        var section = world.getChunkStore().getChunkSectionReferenceAtBlock(
                position.x(), position.y(), position.z());
        return section != null && section.isValid() ? section : null;
    }

    public static BlockType getBlockType(World world, BlockPosition position) {
        var ref = sectionIfLoaded(world, position);
        if (ref == null) return null;
        var blocks = world.getChunkStore().getStore().getComponent(ref, BlockSection.getComponentType());
        return blocks == null ? null : BlockType.getAssetMap().getAsset(
                blocks.get(position.x(), position.y(), position.z()));
    }

    public static <T extends Component<ChunkStore>> T getIfLoaded(
            World world, BlockPosition position, ComponentType<ChunkStore, T> type) {
        var section = sectionIfLoaded(world, position);
        if (section == null) return null;
        var store = world.getChunkStore().getStore();
        var entities = store.getComponent(section, BlockComponentSection.getComponentType());
        if (entities == null) return null;
        int index = ChunkUtil.indexBlock(position.x(), position.y(), position.z());
        var ref = entities.getBlockReference(index);
        if (ref != null && ref.isValid()) return store.getComponent(ref, type);
        var holder = entities.getBlockHolder(index);
        return holder == null ? null : holder.getComponent(type);
    }

    public static void markNeedsSavingIfLoaded(World world, BlockPosition position) {
        var section = sectionIfLoaded(world, position);
        if (section == null) return;
        var store = world.getChunkStore().getStore();
        var entities = store.getComponent(section, BlockComponentSection.getComponentType());
        if (entities != null) {
            entities.markBlockNeedsSaving(ChunkUtil.indexBlock(position.x(), position.y(), position.z()));
        }
        var chunk = store.getComponent(section, ChunkSection.getComponentType());
        if (chunk != null) chunk.markNeedsSaving();
    }

    public static void applyVisualState(World world, BlockPosition position, String state) {
        var section = sectionIfLoaded(world, position);
        if (section == null) return;
        var type = getBlockType(world, position);
        if (type == null || state.equals(LogicBlockTypes.stateName(type))) return;
        // Official state operations preserve the existing block entity and rotation.
        BlockOperations.setBlockInteractionState(world.getChunkStore(), section,
                position.x(), position.y(), position.z(), type, state, false);
    }

    public static int localConnectionMask(World world, BlockPosition position, int worldMask) {
        var rotation = rotationIfLoaded(world, position);
        return rotation == null ? 0 : localConnectionMask(rotation, worldMask);
    }

    public static int localConnectionMask(
            com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple rotation, int worldMask) {
        int localMask = 0;
        for (var direction : HorizontalDirection.values()) {
            if ((worldMask & direction.bit()) == 0) continue;
            var vector = new Vector3i(direction.dx(), 0, direction.dz());
            rotation.undoRotationTo(vector);
            for (var local : HorizontalDirection.values()) {
                if (vector.x == local.dx() && vector.y == 0 && vector.z == local.dz()) {
                    localMask |= local.bit();
                }
            }
        }
        return localMask;
    }
}
