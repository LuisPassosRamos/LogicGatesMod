package sisso.logicsim.integration;

import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.HorizontalDirection;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import static sisso.logicsim.circuit.HorizontalDirection.*;

/** New inverted gates use the same real-server harness and isolated test world. */
final class InvertingServerChecks {
    private static final BlockPosition SOURCE = p(8, 24), PROBE = p(24, 31), REAR = p(24, 32);
    private final GateServerChecks h;
    private final List<String> passed;
    InvertingServerChecks(GateServerChecks harness, List<String> passed) { this.h = harness; this.passed = passed; }
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 64, z); }

    void create(World world) {
        var center = p(12, 12);
        for (var yaw : Rotation.NORMAL) for (int input = 0; input < 3; input++) for (boolean wire : new boolean[]{false, true}) {
            var rotation = RotationTuple.of(yaw, Rotation.None, Rotation.None);
            var output = h.outputOf(rotation);
            h.clearCross(world, center);
            h.place(world, center, LogicBlockTypes.NOT_ID, rotation.index());
            for (var direction : HorizontalDirection.values()) {
                if (direction == output || direction == output.opposite()) continue;
                h.place(world, center.neighbor(direction), LogicBlockTypes.CABLE_ID);
                var source = center.neighbor(direction).neighbor(direction);
                h.place(world, source, LogicBlockTypes.LEVER_ID);
                h.toggle(world, source);
            }
            if (input != 0) {
                var source = center.neighbor(output.opposite());
                if (wire) {
                    h.place(world, source, LogicBlockTypes.CABLE_ID);
                    source = source.neighbor(output.opposite());
                }
                h.place(world, source, LogicBlockTypes.LEVER_ID);
                if (input == 2) h.toggle(world, source);
            }
            var lamp = center.neighbor(output);
            if (wire) {
                h.place(world, lamp, LogicBlockTypes.CABLE_ID);
                lamp = lamp.neighbor(output);
            }
            h.place(world, lamp, LogicBlockTypes.LAMP_ID);
            h.evaluate(world);
            h.gate(world, center, input == 1);
            h.visual(world, lamp, input == 1);
            int mask = output.bit() | (input == 0 ? 0 : output.opposite().bit());
            h.check(LogicBlockTypes.connectionsInState(LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world, center)))
                    == BlockComponentAccess.localConnectionMask(world, center, mask), "NOT ignores powered side wires visually and electrically");
        }
        h.clearCross(world, center);
        passed.add("NOT: 24 input/rotation/direct-or-cable combinations ignore powered side connections");
        feedback(world);

        h.place(world, SOURCE, LogicBlockTypes.LEVER_ID);
        h.place(world, p(9, 24), LogicBlockTypes.NOT_ID);
        h.place(world, p(10, 24), LogicBlockTypes.NAND_ID);
        h.place(world, p(11, 24), LogicBlockTypes.NOR_ID);
        h.place(world, p(12, 24), LogicBlockTypes.NOT_ID);
        h.place(world, p(13, 24), LogicBlockTypes.CABLE_ID);
        h.place(world, p(14, 24), LogicBlockTypes.LAMP_ID);
        h.place(world, p(19, 31), LogicBlockTypes.NOT_ID);
        h.place(world, p(18, 31), LogicBlockTypes.CABLE_ID);
        h.place(world, p(20, 31), LogicBlockTypes.LAMP_ID);
        h.evaluate(world);
        chain(world, false);
        h.gate(world, p(19, 31), true);
        h.check(BlockComponentAccess.gateTopologyIfLoaded(world, p(19, 31)).unavailableInputMask() == 0,
                "NOT ignores its unloaded closed side");
        passed.add("NOT does not wait for unloaded side sections; mixed inverter chain starts from a false source");
    }

    private void feedback(World world) {
        var center = p(18, 12);
        var loop = new BlockPosition[]{p(19, 12), p(19, 13), p(18, 13), p(17, 13), p(17, 12)};
        for (String id : new String[]{LogicBlockTypes.NOT_ID, LogicBlockTypes.NAND_ID, LogicBlockTypes.NOR_ID}) {
            h.place(world, center, id);
            for (var position : loop) h.place(world, position, LogicBlockTypes.CABLE_ID);
            if (!id.equals(LogicBlockTypes.NOT_ID)) {
                h.place(world, p(18, 11), LogicBlockTypes.LEVER_ID);
                if (id.equals(LogicBlockTypes.NOR_ID)) h.toggle(world, p(18, 11));
            }
            h.evaluate(world);
            if (id.equals(LogicBlockTypes.NOT_ID)) {
                h.check(h.state(world).feedbackGateCount == 1, "self-NOT is diagnosed instead of oscillating");
                h.place(world, p(20, 12), LogicBlockTypes.LEVER_ID);
                h.toggle(world, p(20, 12));
                h.evaluate(world);
                h.gate(world, center, false);
                h.check(h.state(world).feedbackGateCount == 0, "external power resolves NOT loop");
                h.place(world, p(20, 12), "Empty");
            } else {
                h.gate(world, center, id.equals(LogicBlockTypes.NAND_ID));
                h.check(h.state(world).feedbackGateCount == 0, "short-circuit inputs resolve inverse feedback");
                h.toggle(world, p(18, 11));
            }
            h.evaluate(world);
            h.gate(world, center, false);
            h.check(h.state(world).feedbackGateCount == 1, "unresolved feedback is disabled and counted");
            h.place(world, center, "Empty");
            h.place(world, p(18, 11), "Empty");
            for (var position : loop) h.place(world, position, "Empty");
        }
        h.evaluate(world);
        h.check(h.state(world).feedbackGateCount == 0, "removing loop clears diagnostics");
        passed.add("NOT/NAND/NOR feedback resolves forced values or disables indeterminate outputs without oscillation");
    }

    CompletableFuture<Void> automaticUpdates(World world) {
        h.toggle(world, SOURCE);
        return h.afterTicks(world, () -> {
            chain(world, true);
            h.toggle(world, SOURCE);
        }).thenCompose(ignored -> h.afterTicks(world, () -> {
            chain(world, false);
            h.toggle(world, SOURCE);
        })).thenCompose(ignored -> h.afterTicks(world, () -> {
            chain(world, true);
            passed.add("world ticks update mixed NOT/NAND/NOR chains in both directions without retained power");
        })).thenCompose(ignored -> world.getChunkStore().getChunkSectionReferenceAtBlockAsync(24, 63, 32))
                .thenCompose(ignored -> h.afterTicks(world, () -> {
                    for (var position : new BlockPosition[]{PROBE, p(24, 30), REAR}) {
                        h.place(world, new BlockPosition(position.x(), 63, position.z()), "Rock_Stone");
                    }
                    h.place(world, PROBE, LogicBlockTypes.NOT_ID, h.rotationFor(NORTH).index());
                    h.place(world, p(24, 30), LogicBlockTypes.LAMP_ID);
                    h.place(world, REAR, LogicBlockTypes.CABLE_ID);
                })).thenCompose(ignored -> h.afterTicks(world, () -> h.gate(world, PROBE, true)))
                .thenCompose(ignored -> h.inWorld(world, () -> ChunkSavingSystems.saveChunksInWorld(
                        world.getChunkStore().getStore(), world))).thenCompose(future -> future)
                .thenCompose(ignored -> h.inWorld(world, () -> {
                    var section = BlockComponentAccess.sectionIfLoaded(world, REAR);
                    var chunk = world.getChunkStore().getStore().getComponent(section, ChunkSection.getComponentType());
                    world.getChunkStore().remove(chunk.getChunkColumnReference(), RemoveReason.UNLOAD);
                    return null;
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, false);
                    h.check(BlockComponentAccess.sectionIfLoaded(world, REAR) == null, "NOT never forces rear loading");
                    h.check(BlockComponentAccess.gateTopologyIfLoaded(world, PROBE).unavailableInputMask() == SOUTH.bit(),
                            "only NOT's rear is unavailable");
                    passed.add("NOT rear section unload suspends its indicator and output without loading neighbors");
                })).thenCompose(ignored -> world.getChunkStore().getChunkSectionReferenceAtBlockAsync(24, 64, 32))
                .thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, true);
                    h.place(world, REAR, "Empty");
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, false);
                    h.place(world, REAR, LogicBlockTypes.CABLE_ID);
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, true);
                    h.place(world, PROBE, "Empty");
                    h.place(world, REAR, "Empty");
                    h.place(world, p(24, 30), "Empty");
                    passed.add("NOT rear reload, removal and replacement update inversion automatically");
                }));
    }

    void reload(World world) {
        h.evaluate(world);
        chain(world, true);
        h.gate(world, p(19, 31), true);
        h.visual(world, p(20, 31), true);
        passed.add("restart reconstructs NOT/NAND/NOR markers, chained inversion and closed-side availability");
        h.toggle(world, SOURCE);
        h.evaluate(world);
        chain(world, false);
        h.toggle(world, SOURCE);
        h.evaluate(world);
        chain(world, true);
        passed.add("restarted inverter chain toggles off and on correctly");
    }

    private void chain(World world, boolean source) {
        h.gate(world, p(9, 24), !source);
        h.gate(world, p(10, 24), source);
        h.gate(world, p(11, 24), !source);
        h.gate(world, p(12, 24), source);
        h.visual(world, p(14, 24), source);
    }
}
