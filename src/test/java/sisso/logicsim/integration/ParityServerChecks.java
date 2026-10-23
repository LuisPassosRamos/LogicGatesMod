package sisso.logicsim.integration;

import com.hypixel.hytale.component.RemoveReason;
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

/** Parity checks run in a separate area of the dedicated integration universe. */
final class ParityServerChecks {
    private static final BlockPosition SOURCE = p(22, 25), EXTRA = p(23, 24);
    private static final BlockPosition ROTATED = p(25, 28);
    private static final BlockPosition PROBE = p(28, 31), REAR = p(28, 32), REAR_SOURCE = p(28, 33);
    private final GateServerChecks h;
    private final List<String> passed;
    ParityServerChecks(GateServerChecks harness, List<String> passed) { h = harness; this.passed = passed; }
    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 64, z); }

    void create(World world) {
        for (int x = 21; x <= 30; x++) for (int z = 8; z <= 31; z++) {
            h.place(world, new BlockPosition(x, 63, z), "Rock_Stone");
        }
        sharedInputs(world);
        feedback(world);
        h.place(world, SOURCE, LogicBlockTypes.LEVER_ID);
        String[] ids = {LogicBlockTypes.XOR_ID, LogicBlockTypes.XNOR_ID, LogicBlockTypes.AND_ID,
                LogicBlockTypes.OR_ID, LogicBlockTypes.NAND_ID, LogicBlockTypes.NOR_ID, LogicBlockTypes.NOT_ID};
        for (int i = 0; i < ids.length; i++) h.place(world, p(23 + i, 25), ids[i]);
        h.place(world, p(30, 25), LogicBlockTypes.LAMP_ID);
        h.place(world, ROTATED, LogicBlockTypes.XNOR_ID, h.rotationFor(SOUTH).index());
        h.place(world, p(25, 27), LogicBlockTypes.CABLE_ID);
        h.place(world, p(25, 29), LogicBlockTypes.LAMP_ID);
        h.evaluate(world);
        chain(world, false);
        rotated(world);
        passed.add("mixed XOR/XNOR/AND/OR/NAND/NOR/NOT chain computes from an off source");
    }

    private BlockPosition offset(BlockPosition center, int x, int z, HorizontalDirection output) {
        return switch (output) {
            case EAST -> p(center.x() + x, center.z() + z);
            case SOUTH -> p(center.x() - z, center.z() + x);
            case WEST -> p(center.x() - x, center.z() - z);
            case NORTH -> p(center.x() + z, center.z() - x);
        };
    }

    private void sharedInputs(World world) {
        var center = p(25, 12);
        for (String id : new String[]{LogicBlockTypes.XOR_ID, LogicBlockTypes.XNOR_ID}) {
            for (var output : HorizontalDirection.values()) for (int count : new int[]{2, 3}) {
                h.place(world, center, id, h.rotationFor(output).index());
                int[][] wires = count == 2 ? new int[][]{{-1, 0}, {-1, -1}, {0, -1}}
                        : new int[][]{{-1, 0}, {-1, -1}, {0, -1}, {-1, 1}, {0, 1}};
                for (var wire : wires) h.place(world, offset(center, wire[0], wire[1], output), LogicBlockTypes.CABLE_ID);
                var lever = offset(center, -2, -1, output);
                h.place(world, lever, LogicBlockTypes.LEVER_ID);
                h.place(world, center.neighbor(output), LogicBlockTypes.LAMP_ID);
                for (boolean on : new boolean[]{false, true, false}) {
                    if (on != BlockComponentAccess.getIfLoaded(world, lever, sisso.logicsim.LogicSimPlugin.getLeverComponentType()).isOn()) h.toggle(world, lever);
                    h.evaluate(world);
                    boolean parity = on && count == 3;
                    boolean expected = id.equals(LogicBlockTypes.XNOR_ID) ? !parity : parity;
                    h.gate(world, center, expected);
                    h.visual(world, center.neighbor(output), expected);
                }
                h.clearCross(world, center);
                for (var wire : wires) h.place(world, offset(center, wire[0], wire[1], output), "Empty");
                h.place(world, lever, "Empty");
            }
        }
        passed.add("XOR/XNOR count two or three physical inputs sharing one cable network in all orientations");
    }

    private void feedback(World world) {
        var center = p(25, 18);
        int[][] wires = {{1, 0}, {2, 0}, {2, 1}, {2, 2}, {1, 2}, {0, 2}, {-1, 2}, {-2, 2}, {-2, 1}, {-2, 0}, {-1, 0}};
        for (String id : new String[]{LogicBlockTypes.XOR_ID, LogicBlockTypes.XNOR_ID}) {
            h.place(world, center, id);
            for (var wire : wires) h.place(world, offset(center, wire[0], wire[1], EAST), LogicBlockTypes.CABLE_ID);
            h.place(world, p(25, 17), LogicBlockTypes.LEVER_ID);
            h.toggle(world, p(25, 17));
            h.evaluate(world);
            h.gate(world, center, false);
            h.check(h.state(world).feedbackGateCount == 1, "parity feedback is diagnosed without oscillation");
            h.place(world, p(28, 18), LogicBlockTypes.LEVER_ID);
            h.toggle(world, p(28, 18));
            h.evaluate(world);
            h.gate(world, center, id.equals(LogicBlockTypes.XNOR_ID));
            h.check(h.state(world).feedbackGateCount == 0, "external cable producer resolves parity feedback");
            h.place(world, p(28, 18), "Empty");
            h.evaluate(world);
            h.gate(world, center, false);
            h.check(h.state(world).feedbackGateCount == 1, "removing external producer retains no gate power");
            h.place(world, center, "Empty");
            h.place(world, p(25, 17), "Empty");
            for (var wire : wires) h.place(world, offset(center, wire[0], wire[1], EAST), "Empty");
        }
        h.evaluate(world);
        h.check(h.state(world).feedbackGateCount == 0, "parity loop cleanup clears diagnostics");
        passed.add("XOR/XNOR feedback is suspended, resolved by known external power and reset after removal");
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
            h.place(world, EXTRA, LogicBlockTypes.LEVER_ID);
            h.toggle(world, EXTRA);
        })).thenCompose(ignored -> h.afterTicks(world, () -> {
            chain(world, false);
            h.place(world, EXTRA, "Empty");
        })).thenCompose(ignored -> h.afterTicks(world, () -> {
            chain(world, true);
            passed.add("world ticks update parity chains and input removal without manual evaluation");
        })).thenCompose(ignored -> sections(world, LogicBlockTypes.XOR_ID))
                .thenCompose(ignored -> sections(world, LogicBlockTypes.XNOR_ID));
    }

    private CompletableFuture<Void> sections(World world, String id) {
        return world.getChunkStore().getChunkSectionReferenceAtBlockAsync(28, 63, 32)
                .thenCompose(ignored -> h.afterTicks(world, () -> {
                    for (var position : new BlockPosition[]{PROBE, REAR, REAR_SOURCE, p(27, 31), p(28, 30)}) {
                        h.place(world, new BlockPosition(position.x(), 63, position.z()), "Rock_Stone");
                    }
                    h.place(world, PROBE, id, h.rotationFor(NORTH).index());
                    h.place(world, p(27, 31), LogicBlockTypes.CABLE_ID);
                    h.place(world, p(28, 30), LogicBlockTypes.LAMP_ID);
                    h.place(world, REAR, LogicBlockTypes.CABLE_ID);
                    h.place(world, REAR_SOURCE, LogicBlockTypes.LEVER_ID);
                    if (id.equals(LogicBlockTypes.XOR_ID)) h.toggle(world, REAR_SOURCE);
                })).thenCompose(ignored -> h.afterTicks(world, () -> h.gate(world, PROBE, true)))
                .thenCompose(ignored -> h.inWorld(world, () -> ChunkSavingSystems.saveChunksInWorld(
                        world.getChunkStore().getStore(), world)))
                .thenCompose(save -> save)
                .thenCompose(ignored -> h.inWorld(world, () -> {
                    var section = BlockComponentAccess.sectionIfLoaded(world, REAR);
                    var chunk = world.getChunkStore().getStore().getComponent(section, ChunkSection.getComponentType());
                    world.getChunkStore().remove(chunk.getChunkColumnReference(), RemoveReason.UNLOAD);
                    return null;
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, false);
                    h.visual(world, p(28, 30), false);
                    h.check(BlockComponentAccess.sectionIfLoaded(world, REAR) == null, "parity never forces section loading");
                    h.check(BlockComponentAccess.gateTopologyIfLoaded(world, PROBE).unavailableInputMask() == SOUTH.bit(), "only rear parity input unavailable");
                })).thenCompose(ignored -> world.getChunkStore().getChunkSectionReferenceAtBlockAsync(28, 64, 32))
                .thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, true);
                    h.place(world, REAR, "Empty");
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, id.equals(LogicBlockTypes.XNOR_ID));
                    h.place(world, p(27, 31), "Empty");
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, false);
                    h.place(world, REAR, LogicBlockTypes.CABLE_ID);
                })).thenCompose(ignored -> h.afterTicks(world, () -> {
                    h.gate(world, PROBE, true);
                    for (var position : new BlockPosition[]{PROBE, REAR, REAR_SOURCE, p(27, 31), p(28, 30)}) h.place(world, position, "Empty");
                    passed.add(id + ": actual input-section unload/reload, removal and reconnection update automatically");
                }));
    }

    void reload(World world) {
        h.evaluate(world);
        chain(world, true);
        rotated(world);
        passed.add("real restart reconstructs XOR/XNOR markers, rotations and mixed gate chain");
        h.toggle(world, SOURCE);
        h.evaluate(world);
        chain(world, false);
        h.toggle(world, SOURCE);
        h.evaluate(world);
        chain(world, true);
        passed.add("restarted parity chain toggles both ways and derives fresh outputs");
    }

    private void chain(World world, boolean source) {
        boolean[] signals = {source, !source, !source, !source, source, !source, source};
        for (int i = 0; i < signals.length; i++) h.gate(world, p(23 + i, 25), signals[i]);
        h.visual(world, p(30, 25), source);
    }

    private void rotated(World world) {
        h.gate(world, ROTATED, true);
        h.visual(world, p(25, 29), true);
        h.check(BlockComponentAccess.gateTopologyIfLoaded(world, ROTATED).outputDirection() == SOUTH,
                "nondefault XNOR rotation is preserved after visual updates and restart");
    }
}
