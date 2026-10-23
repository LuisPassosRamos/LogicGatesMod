package sisso.logicsim.integration;

import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.VariantRotation;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.joml.Vector3i;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.HorizontalDirection;
import sisso.logicsim.interactions.ToggleLogicLeverInteraction;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import static sisso.logicsim.circuit.HorizontalDirection.*;

/** Real 0.7.4 API checks, sharing only the dedicated integration universe. */
final class GateServerChecks {
    private static final BlockPosition SOURCE = p(11, 16), AND = p(12, 16), OR = p(13, 16), LAMP = p(15, 16);
    private static final BlockPosition BOUNDARY = p(16, 31), EMPTY_NEIGHBOR = p(16, 32);
    private final List<String> passed;

    GateServerChecks(List<String> passed) { this.passed = passed; }
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 64, z); }

    void create(World world) {
        for (int x = 8; x <= 20; x++) for (int z = 8; z <= 31; z++) {
            place(world, new BlockPosition(x, 63, z), "Rock_Stone");
        }
        assets();
        truthTables(world);
        chainsAndFeedback(world);
        check(BlockComponentAccess.sectionIfLoaded(world, EMPTY_NEIGHBOR) == null, "empty neighbor initially unloaded");
        place(world, BOUNDARY, LogicBlockTypes.AND_ID, rotationFor(NORTH).index());
        place(world, p(15, 31), LogicBlockTypes.LEVER_ID);
        toggle(world, p(15, 31));
        place(world, p(16, 30), LogicBlockTypes.LAMP_ID);
        evaluate(world);
        gate(world, BOUNDARY, false);
        check(state(world).pendingGateCount == 1, "unknown input is pending, not a removed input");
    }

    private void assets() {
        for (String id : new String[]{LogicBlockTypes.AND_ID, LogicBlockTypes.OR_ID,
                LogicBlockTypes.NOT_ID, LogicBlockTypes.NAND_ID, LogicBlockTypes.NOR_ID,
                LogicBlockTypes.XOR_ID, LogicBlockTypes.XNOR_ID}) {
            var base = BlockType.getAssetMap().getAsset(id);
            check(base != null && LogicBlockTypes.isLogicGate(base), "gate asset registered: " + id);
            check(base.getVariantRotation() == VariantRotation.NESW, "gate placement has four horizontal rotations");
            check(base.getItem() != null && base.getItem().getModel() != null, "inventory/held model provided");
            check(base.getPlacementSettings().toPacket().rotationMode
                    == com.hypixel.hytale.protocol.BlockPlacementRotationMode.FacingPlayer, "player-relative placement packet");
            check(base.getRotationYawPlacementOffset() == Rotation.Ninety, "correct art yaw offset");
            for (var towardPlayer : Rotation.NORMAL) {
                var look = new Vector3i(0, 0, 1);
                RotationTuple.of(towardPlayer, Rotation.None, Rotation.None).applyRotationTo(look);
                look.negate();
                var output = new Vector3i(1, 0, 0);
                RotationTuple.of(towardPlayer.add(base.getRotationYawPlacementOffset()), Rotation.None, Rotation.None)
                        .applyRotationTo(output);
                check(look.equals(output), "placement output aligns with look in all four directions");
            }
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                if (id.equals(LogicBlockTypes.NOT_ID) && (mask & ~10) != 0) continue;
                var type = base.getBlockForState(LogicBlockTypes.terminalState(on, mask));
                check(type != null && type.getHitboxTypeIndex() > 0, "gate state and custom hitbox loaded");
                check(type.getInteractionHitboxTypeIndex() == type.getHitboxTypeIndex(), "matching selection and collision");
                check((type.getLight() != null) == on, "gate light matches output");
                var boxes = BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
                check(boxes != null && !boxes.protrudesUnitBox(), "gate fits one block");
                for (var rotation : Rotation.NORMAL) {
                    var rotated = boxes.get(rotation, Rotation.None, Rotation.None);
                    check(rotated.getBoundingBox().max.y <= .1875, "gate collision matches low model");
                    int pieces = switch (LogicBlockTypes.kind(base)) {
                        case AND -> 4;
                        case NOT -> 10;
                        case NAND -> 5;
                        case XOR, XNOR -> 21;
                        default -> 14;
                    };
                    check(rotated.getDetailBoxes().length == pieces
                            + (id.equals(LogicBlockTypes.NOT_ID) ? 8 : 23) + Integer.bitCount(mask), "gate hitbox matches actual pieces");
                }
            }
        }
        passed.add("seven gate types: all 200 visual states, placement packets, inventory models, lights and hitboxes load");
    }

    private void truthTables(World world) {
        var center = p(12, 12);
        for (String id : new String[]{LogicBlockTypes.AND_ID, LogicBlockTypes.OR_ID,
                LogicBlockTypes.NAND_ID, LogicBlockTypes.NOR_ID, LogicBlockTypes.XOR_ID, LogicBlockTypes.XNOR_ID}) {
            for (var yaw : Rotation.NORMAL) {
                var rotation = RotationTuple.of(yaw, Rotation.None, Rotation.None);
                var output = outputOf(rotation);
                for (int configuration = 0; configuration < 27; configuration++) {
                    clearCross(world, center);
                    place(world, center, id, rotation.index());
                    int code = configuration, connected = 0, energized = 0, mask = output.bit();
                    boolean cables = (configuration & 1) != 0;
                    for (var direction : HorizontalDirection.values()) {
                        if (direction == output) continue;
                        int value = code % 3;
                        code /= 3;
                        if (value == 0) continue;
                        connected++;
                        if (value == 2) energized++;
                        mask |= direction.bit();
                        var source = center.neighbor(direction);
                        if (cables) {
                            place(world, source, LogicBlockTypes.CABLE_ID);
                            source = source.neighbor(direction);
                        }
                        place(world, source, LogicBlockTypes.LEVER_ID);
                        if (value == 2) toggle(world, source);
                    }
                    var lamp = center.neighbor(output);
                    if (cables) {
                        place(world, lamp, LogicBlockTypes.CABLE_ID);
                        lamp = lamp.neighbor(output);
                    }
                    place(world, lamp, LogicBlockTypes.LAMP_ID);
                    evaluate(world);
                    var kind = LogicBlockTypes.kind(BlockType.getAssetMap().getAsset(id));
                    boolean base = switch (kind) {
                        case AND, NAND -> connected == energized;
                        case XOR, XNOR -> energized % 2 == 1;
                        default -> energized > 0;
                    };
                    boolean expected = connected > 0 && (kind.isInverting() ? !base : base);
                    gate(world, center, expected);
                    visual(world, lamp, expected);
                    String expectedState = LogicBlockTypes.terminalState(expected,
                            BlockComponentAccess.localConnectionMask(world, center, mask));
                    check(expectedState.equals(LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world, center))),
                            "gate ports match real placement rotation");
                    check(BlockComponentAccess.rotationIfLoaded(world, center).index() == rotation.index(), "rotation preserved");
                }
            }
        }
        clearCross(world, center);
        passed.add("AND/OR/NAND/NOR/XOR/XNOR: 648 truth-table/rotation combinations with absent, off and on inputs");
    }

    private void chainsAndFeedback(World world) {
        place(world, SOURCE, LogicBlockTypes.LEVER_ID);
        toggle(world, SOURCE);
        place(world, AND, LogicBlockTypes.AND_ID);
        place(world, OR, LogicBlockTypes.OR_ID);
        place(world, p(13, 15), LogicBlockTypes.LEVER_ID);
        place(world, p(14, 16), LogicBlockTypes.CABLE_ID);
        place(world, LAMP, LogicBlockTypes.LAMP_ID);
        place(world, p(14, 17), LogicBlockTypes.CABLE_ID);
        place(world, p(14, 18), LogicBlockTypes.LAMP_ID);
        evaluate(world);
        gate(world, AND, true);
        gate(world, OR, true);
        visual(world, LAMP, true);
        visual(world, p(14, 18), true);
        passed.add("lever directly drives AND -> OR -> branched cable network and lamps");

        toggle(world, SOURCE);
        place(world, p(14, 15), LogicBlockTypes.LEVER_ID);
        toggle(world, p(14, 15));
        evaluate(world);
        gate(world, AND, false);
        gate(world, OR, false);
        visual(world, LAMP, true);
        place(world, p(14, 15), "Empty");
        evaluate(world);
        visual(world, LAMP, false);
        toggle(world, SOURCE);
        evaluate(world);
        passed.add("external output power does not light/backfeed gates; networks aggregate multiple producers");

        var section = BlockComponentAccess.sectionIfLoaded(world, AND);
        var store = world.getChunkStore().getStore();
        var parked = ChunkStore.REGISTRY.getNonTickingComponentType();
        store.ensureComponent(section, parked);
        evaluate(world);
        gate(world, AND, true);
        store.tryRemoveComponent(section, parked);
        evaluate(world);
        gate(world, OR, true);
        passed.add("gate markers survive parked/live entity transitions and visual changes");

        var center = p(18, 12);
        var loop = new BlockPosition[]{p(19, 12), p(19, 13), p(18, 13), p(17, 13), p(17, 12)};
        for (String id : new String[]{LogicBlockTypes.AND_ID, LogicBlockTypes.OR_ID}) {
            place(world, center, id);
            for (var position : loop) place(world, position, LogicBlockTypes.CABLE_ID);
            evaluate(world);
            gate(world, center, false);
            place(world, p(20, 12), LogicBlockTypes.LEVER_ID);
            toggle(world, p(20, 12));
            evaluate(world);
            gate(world, center, true);
            toggle(world, p(20, 12));
            evaluate(world);
            gate(world, center, false);
            place(world, p(20, 12), "Empty");
            place(world, center, "Empty");
            for (var position : loop) place(world, position, "Empty");
        }
        passed.add("AND/OR feedback loops settle without retained power after source turns off");
    }

    CompletableFuture<Void> automaticUpdates(World world) {
        toggle(world, SOURCE);
        return afterTicks(world, () -> {
            gate(world, AND, false);
            gate(world, OR, false);
            visual(world, LAMP, false);
            toggle(world, SOURCE);
            place(world, p(12, 15), LogicBlockTypes.CABLE_ID);
        }).thenCompose(ignored -> afterTicks(world, () -> {
            gate(world, AND, false);
            place(world, p(12, 15), "Empty");
        })).thenCompose(ignored -> afterTicks(world, () -> {
            gate(world, AND, true);
            gate(world, OR, true);
            visual(world, LAMP, true);
            gate(world, BOUNDARY, false);
            check(BlockComponentAccess.sectionIfLoaded(world, EMPTY_NEIGHBOR) == null, "gate never forces neighbor loading");
            passed.add("world tick updates gate toggles and removal of an off input without manual evaluation");
        })).thenCompose(ignored -> loadEmptyNeighbor(world))
                .thenCompose(ignored -> afterTicks(world, () -> {
                    gate(world, BOUNDARY, true);
                    check(state(world).pendingGateCount == 0, "empty section return clears pending gate");
                    passed.add("empty neighbor section load automatically enables a waiting gate");
                    rotateWithoutEntityChange(world, WEST);
                })).thenCompose(ignored -> afterTicks(world, () -> {
                    gate(world, BOUNDARY, false);
                    check(state(world).pendingGateCount == 0, "rotation changed ports, not section availability");
                    rotateWithoutEntityChange(world, NORTH);
                })).thenCompose(ignored -> afterTicks(world, () -> {
                    gate(world, BOUNDARY, true);
                    passed.add("in-place gate rotation is detected without replacing its entity or invalidating manually");
                    unloadEmptyNeighbor(world);
                })).thenCompose(ignored -> afterTicks(world, () -> {
                    gate(world, BOUNDARY, false);
                    check(state(world).pendingGateCount == 1, "unloaded input section suspends gate");
                    check(BlockComponentAccess.sectionIfLoaded(world, EMPTY_NEIGHBOR) == null, "neighbor remains unloaded");
                    passed.add("input section unload suspends gate output and indicator without forced loading");
                })).thenCompose(ignored -> loadEmptyNeighbor(world))
                .thenCompose(ignored -> afterTicks(world, () -> {
                    gate(world, BOUNDARY, true);
                    check(state(world).pendingGateCount == 0, "gate resumes after empty section reload");
                    passed.add("empty section reload automatically restores output and indicator");
                }));
    }

    void reload(World world) {
        evaluate(world);
        gate(world, AND, true);
        gate(world, OR, true);
        visual(world, LAMP, true);
        passed.add("real restart restores gate markers, directions and direct chained circuit");
        toggle(world, SOURCE);
        evaluate(world);
        gate(world, AND, false);
        gate(world, OR, false);
        visual(world, LAMP, false);
        toggle(world, SOURCE);
        if (BlockComponentAccess.sectionIfLoaded(world, EMPTY_NEIGHBOR) != null) unloadEmptyNeighbor(world);
        evaluate(world);
        gate(world, AND, true);
        gate(world, BOUNDARY, false);
        check(state(world).pendingGateCount == 1, "stored On visual is not authoritative when an input is unavailable");
        passed.add("restarted gates toggle correctly and recompute power instead of retaining saved On visuals");
    }

    private void rotateWithoutEntityChange(World world, HorizontalDirection output) {
        var marker = BlockComponentAccess.getIfLoaded(world, BOUNDARY, LogicSimPlugin.getGateComponentType());
        var section = BlockComponentAccess.sectionIfLoaded(world, BOUNDARY);
        var blocks = world.getChunkStore().getStore().getComponent(section, BlockSection.getComponentType());
        blocks.set(BOUNDARY.x(), BOUNDARY.y(), BOUNDARY.z(),
                blocks.get(BOUNDARY.x(), BOUNDARY.y(), BOUNDARY.z()), rotationFor(output).index(), 0);
        check(marker == BlockComponentAccess.getIfLoaded(world, BOUNDARY, LogicSimPlugin.getGateComponentType()),
                "rotation keeps the same marker binding");
    }

    private CompletableFuture<?> loadEmptyNeighbor(World world) {
        return world.getChunkStore().getChunkSectionReferenceAtBlockAsync(
                EMPTY_NEIGHBOR.x(), EMPTY_NEIGHBOR.y(), EMPTY_NEIGHBOR.z());
    }

    private void unloadEmptyNeighbor(World world) {
        var section = BlockComponentAccess.sectionIfLoaded(world, EMPTY_NEIGHBOR);
        check(section != null, "empty neighbor loaded before unload");
        var chunk = world.getChunkStore().getStore().getComponent(section, ChunkSection.getComponentType());
        world.getChunkStore().remove(chunk.getChunkColumnReference(), RemoveReason.UNLOAD);
    }

    RotationTuple rotationFor(HorizontalDirection output) {
        for (var yaw : Rotation.NORMAL) {
            var rotation = RotationTuple.of(yaw, Rotation.None, Rotation.None);
            if (outputOf(rotation) == output) return rotation;
        }
        throw new AssertionError("No upright rotation for " + output);
    }

    HorizontalDirection outputOf(RotationTuple rotation) {
        var vector = new Vector3i(1, 0, 0);
        rotation.applyRotationTo(vector);
        for (var direction : HorizontalDirection.values()) {
            if (vector.equals(new Vector3i(direction.dx(), 0, direction.dz()))) return direction;
        }
        throw new AssertionError("Unexpected non-horizontal rotation");
    }

    void clearCross(World world, BlockPosition center) {
        place(world, center, "Empty");
        for (var direction : HorizontalDirection.values()) {
            var neighbor = center.neighbor(direction);
            place(world, neighbor, "Empty");
            place(world, neighbor.neighbor(direction), "Empty");
        }
    }

    void gate(World world, BlockPosition position, boolean on) {
        var type = BlockComponentAccess.getBlockType(world, position);
        check(LogicBlockTypes.isLogicGate(type), "gate exists at " + position);
        check(BlockComponentAccess.getIfLoaded(world, position, LogicSimPlugin.getGateComponentType()) != null,
                "gate marker preserved: " + position);
        visual(world, position, on);
        check((type.getLight() != null) == on, "gate illumination matches output");
    }

    void visual(World world, BlockPosition position, boolean on) {
        String state = LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world, position));
        check(state != null && state.contains("_Connections_") && LogicBlockTypes.isOnState(state) == on,
                "visual " + position + " expected " + on + ", got " + state);
    }

    sisso.logicsim.systems.CircuitWorldState state(World world) {
        return world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
    }

    void evaluate(World world) { EnergyPropagationSystem.recalculate(world); }
    void toggle(World world, BlockPosition position) {
        check(ToggleLogicLeverInteraction.toggle(world, position), "source toggles: " + position);
    }
    void place(World world, BlockPosition position, String id) {
        place(world, position, id, RotationTuple.NONE_INDEX);
    }
    void place(World world, BlockPosition position, String id, int rotation) {
        var section = BlockComponentAccess.sectionIfLoaded(world, position);
        var type = BlockType.getAssetMap().getAsset(id);
        check(section != null && type != null, "loaded section and asset for " + id + " at " + position);
        BlockOperations.setBlock(world.getChunkStore(), section, position.x(), position.y(), position.z(),
                BlockType.getAssetMap().getIndex(id), type, rotation, 0, 0);
    }
    CompletableFuture<Void> afterTicks(World world, Runnable action) {
        var future = new CompletableFuture<Void>();
        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() ->
                inWorld(world, () -> { action.run(); return null; }).whenComplete((value, error) -> {
                    if (error == null) future.complete(null);
                    else future.completeExceptionally(error);
                }));
        return future;
    }
    <T> CompletableFuture<T> inWorld(World world, Supplier<T> action) {
        var future = new CompletableFuture<T>();
        world.execute(() -> {
            try { future.complete(action.get()); }
            catch (Throwable error) { future.completeExceptionally(error); }
        });
        return future;
    }
    void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
