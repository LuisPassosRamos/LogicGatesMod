package sisso.logicsim.integration;

import com.google.gson.GsonBuilder;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.server.core.HytaleServer;
import com.hypixel.hytale.server.core.ShutdownReason;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.Universe;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.WorldConfig;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.ChunkSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.component.ChunkSavingSystems;
import com.hypixel.hytale.server.core.universe.world.worldgen.provider.VoidWorldGenProvider;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.circuit.HorizontalDirection;
import sisso.logicsim.interactions.ToggleLogicLeverInteraction;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/** Test-only plugin packaged separately; all writes target its dedicated void world. */
public final class ServerIntegrationPlugin extends JavaPlugin {
    private static final String WORLD = "logicgates-integration";
    private static final BlockPosition LEVER = p(30, 0);
    private static final BlockPosition LAMP = p(33, 0);
    private final List<String> passed = new ArrayList<>();
    private Map<String,Object> measurements = Map.of();

    public ServerIntegrationPlugin(JavaPluginInit init) { super(init); }

    @Override
    protected void setup() { getCommandRegistry().registerCommand(new SelfTestCommand()); }

    private final class SelfTestCommand extends CommandBase {
        private final RequiredArg<String> phase =
                withRequiredArg("phase", "create or reload", ArgTypes.STRING);
        SelfTestCommand() { super("logic-selftest", "Runs isolated LogicGates server tests."); }
        @Override
        protected void executeSync(CommandContext context) {
            if (context.isPlayer()) throw new IllegalStateException("Console-only test command");
            String action = phase.get(context);
            if (!action.equals("create") && !action.equals("reload")) {
                throw new IllegalArgumentException("Expected create or reload");
            }
            run(action);
        }
    }

    private static BlockPosition p(int x, int z) { return new BlockPosition(x, 64, z); }

    private void run(String phase) {
        var universe = Universe.get();
        CompletableFuture<World> future;
        var loaded = universe.getWorld(WORLD);
        if (loaded != null) future = CompletableFuture.completedFuture(loaded);
        else if (Files.exists(universe.getWorldsPath().resolve(WORLD).resolve("config.json"))) {
            future = universe.addWorld(WORLD);
        } else {
            var config = new WorldConfig();
            config.setWorldGenProvider(new VoidWorldGenProvider());
            config.setCanUnloadChunks(false);
            config.setSpawningNPC(false);
            config.setTicking(true);
            future = universe.makeWorld(WORLD, universe.getWorldsPath().resolve(WORLD), config);
        }
        future.thenCompose(world -> {
            var tasks = new ArrayList<CompletableFuture<?>>();
            for (int x : new int[]{-1, 0, 32}) {
                for (int z : new int[]{-1, 0}) {
                    tasks.add(world.getChunkStore().getChunkSectionReferenceAtBlockAsync(x, 64, z));
                    tasks.add(world.getChunkStore().getChunkSectionReferenceAtBlockAsync(x, 63, z));
                }
            }
            return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).thenApply(ignored -> world);
        }).orTimeout(60, TimeUnit.SECONDS).whenComplete((world, failure) -> {
            if (failure != null) { finish(phase, failure); return; }
            world.execute(() -> {
                try {
                    if (phase.equals("create")) {
                        create(world);
                        var gates = new GateServerChecks(passed);
                        gates.create(world);
                        var inverters = new InvertingServerChecks(gates, passed);
                        inverters.create(world);
                        var parity = new ParityServerChecks(gates, passed);
                        parity.create(world);
                        gates.automaticUpdates(world).thenCompose(ignored -> inverters.automaticUpdates(world))
                                .thenCompose(ignored -> parity.automaticUpdates(world))
                                .thenCompose(ignored -> automaticUpdates(world))
                                .thenCompose(ignored -> teachingChecks(world, true))
                                .whenComplete((ignored, error) -> finish(phase, error));
                    } else {
                        reload(world);
                        new GateServerChecks(passed).reload(world);
                        new InvertingServerChecks(new GateServerChecks(passed), passed).reload(world);
                        new ParityServerChecks(new GateServerChecks(passed), passed).reload(world);
                        teachingChecks(world, false).whenComplete((ignored,error)->finish(phase,error));
                    }
                } catch (Throwable error) { finish(phase, error); }
            });
        });
    }

    private CompletableFuture<Void> teachingChecks(World world, boolean create) {
        var checks = new TeachingServerChecks(passed);
        return checks.run(world,create).thenRun(()->measurements=checks.measurements());
    }

    private void create(World world) {
        for (int x = -1; x <= 35; x++) for (int z = -5; z <= 3; z++) {
            place(world, new BlockPosition(x, 63, z), "Rock_Stone", RotationTuple.NONE_INDEX);
        }
        terminalAssetsAndRotations(world);
        place(world, LEVER, LogicBlockTypes.LEVER_ID);
        place(world, p(31, 0), LogicBlockTypes.CABLE_ID);
        place(world, p(32, 0), LogicBlockTypes.CABLE_ID);
        place(world, LAMP, LogicBlockTypes.LAMP_ID);
        evaluate(world);
        lamp(world, LAMP, false);
        var leverType = BlockComponentAccess.getBlockType(world, LEVER);
        check(LogicBlockTypes.isLogicLever(leverType), "recognized lever: id=" + leverType.getId()
                + ", default=" + leverType.getDefaultStateKey()
                + ", item=" + (leverType.getItem() == null ? null : leverType.getItem().getId()));
        check(BlockComponentAccess.getIfLoaded(world, LEVER, LogicSimPlugin.getLeverComponentType()) != null,
                "lever component present after state change");
        check(ToggleLogicLeverInteraction.toggle(world, LEVER), "lever component attached");
        evaluate(world);
        lamp(world, LAMP, true);
        terminal(world, LEVER, true, 2);
        terminal(world, LAMP, true, 8);
        passed.add("lever -> cables -> lamp across section boundary");

        for (var rotation : Rotation.NORMAL) {
            int index = RotationTuple.of(rotation, Rotation.None, Rotation.None).index();
            place(world, p(31, 0), LogicBlockTypes.CABLE_ID, index);
            evaluate(world);
            lamp(world, LAMP, true);
            var type = BlockComponentAccess.getBlockType(world, p(31, 0));
            int local = BlockComponentAccess.localConnectionMask(world, p(31, 0), 10);
            check(LogicBlockTypes.cableState(local).equals(LogicBlockTypes.stateName(type)), "rotated cable mask");
            var section = BlockComponentAccess.sectionIfLoaded(world, p(31, 0));
            check(world.getChunkStore().getStore().getComponent(section, BlockSection.getComponentType())
                    .getRotationIndex(31, 64, 0) == index, "rotation preserved");
        }
        passed.add("all horizontal cable rotations preserve connectivity and orientation");

        place(world, p(32, 1), LogicBlockTypes.CABLE_ID);
        place(world, p(32, 2), LogicBlockTypes.LAMP_ID);
        evaluate(world);
        lamp(world, p(32, 2), true);
        passed.add("branch and corner");

        place(world, p(31, -1), LogicBlockTypes.LEVER_ID);
        ToggleLogicLeverInteraction.toggle(world, p(31, -1));
        ToggleLogicLeverInteraction.toggle(world, LEVER);
        evaluate(world);
        lamp(world, LAMP, true);
        place(world, p(31, -1), "Empty");
        evaluate(world);
        lamp(world, LAMP, false);
        passed.add("multiple sources, last source removal and no phantom power");

        ToggleLogicLeverInteraction.toggle(world, LEVER);
        place(world, p(32, 0), "Empty");
        evaluate(world);
        lamp(world, LAMP, false);
        lamp(world, p(32, 2), false);
        place(world, p(32, 0), LogicBlockTypes.CABLE_ID);
        evaluate(world);
        lamp(world, LAMP, true);
        place(world, p(31, 1), LogicBlockTypes.LAMP_ID);
        evaluate(world);
        lamp(world, p(31, 1), true);
        passed.add("cable break/replacement and lamp added to live network");

        place(world, p(0, -4), LogicBlockTypes.LEVER_ID);
        place(world, p(1, -4), LogicBlockTypes.CABLE_ID);
        place(world, p(2, -4), LogicBlockTypes.LAMP_ID);
        place(world, p(3, -4), LogicBlockTypes.CABLE_ID);
        place(world, p(4, -4), LogicBlockTypes.LAMP_ID);
        ToggleLogicLeverInteraction.toggle(world, p(0, -4));
        evaluate(world);
        lamp(world, p(2, -4), true);
        lamp(world, p(4, -4), false);
        passed.add("lamps do not bridge networks");

        var store = world.getChunkStore().getStore();
        var section = BlockComponentAccess.sectionIfLoaded(world, LEVER);
        var nonTicking = ChunkStore.REGISTRY.getNonTickingComponentType();
        store.ensureComponent(section, nonTicking);
        evaluate(world);
        lamp(world, LAMP, true);
        store.tryRemoveComponent(section, nonTicking);
        evaluate(world);
        lamp(world, LAMP, true);
        passed.add("parked and live block entity lifecycle");
        check(BlockComponentAccess.getIfLoaded(world, LEVER, LogicSimPlugin.getLeverComponentType()).isOn(),
                "persistent lever prepared");
    }

    private void terminalAssetsAndRotations(World world) {
        var center = p(8, -2);
        for (String id : new String[]{LogicBlockTypes.LEVER_ID, LogicBlockTypes.LAMP_ID}) {
            var base = BlockType.getAssetMap().getAsset(id);
            check(base != null, "terminal asset loaded: " + id);
            for (boolean on : new boolean[]{false, true}) for (int mask = 0; mask < 16; mask++) {
                var type = base.getBlockForState(LogicBlockTypes.terminalState(on, mask));
                check(type != null, "connected terminal state loaded");
                check(type.getHitboxTypeIndex() > 0, "custom physical hitbox resolved");
                check(type.getHitboxTypeIndex() == type.getInteractionHitboxTypeIndex(), "selection matches collision");
                check((type.getLight() != null) == (id.equals(LogicBlockTypes.LAMP_ID) && on), "terminal light state");
                var boxes = BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
                check(boxes != null && !boxes.protrudesUnitBox(), "hitbox stays inside block");
                for (var yaw : Rotation.NORMAL) {
                    var rotated = boxes.get(yaw, Rotation.None, Rotation.None);
                    check(rotated.getBoundingBox().max.y < .65, "no invisible full-height terminal collision");
                    check(rotated.getDetailBoxes().length == (id.equals(LogicBlockTypes.LEVER_ID) ? 3 : 4)
                            + Integer.bitCount(mask), "compound hitbox covers only body and actual connectors");
                    check(!rotated.containsPosition(.02, .5, .02), "empty space beside terminal remains passable");
                }
            }
            for (var yaw : Rotation.NORMAL) {
                int rotation = RotationTuple.of(yaw, Rotation.None, Rotation.None).index();
                place(world, center, id, rotation);
                boolean on = id.equals(LogicBlockTypes.LEVER_ID);
                if (on) check(ToggleLogicLeverInteraction.toggle(world, center), "test lever toggles");
                for (int mask = 0; mask < 16; mask++) {
                    for (var direction : HorizontalDirection.values()) {
                        place(world, center.neighbor(direction),
                                (mask & direction.bit()) == 0 ? "Empty" : LogicBlockTypes.CABLE_ID);
                    }
                    evaluate(world);
                    terminal(world, center, on, mask);
                    var section = BlockComponentAccess.sectionIfLoaded(world, center);
                    check(world.getChunkStore().getStore().getComponent(section, BlockSection.getComponentType())
                            .getRotationIndex(center.x(), center.y(), center.z()) == rotation, "terminal rotation preserved");
                }
            }
            for (var direction : HorizontalDirection.values()) place(world, center.neighbor(direction), "Empty");
            place(world, center, "Empty");
        }
        passed.add("all 64 terminal power/connection states load with compact physical and selection hitboxes");
        passed.add("lever and lamp: all 16 connection masks in four horizontal rotations match their neighbors");
        var leverBase = BlockType.getAssetMap().getAsset(LogicBlockTypes.LEVER_ID);
        for (String storedState : new String[]{"On", "Off", "On_Connections_15", "Off_Connections_15"}) {
            var storedType = leverBase.getBlockForState(storedState);
            check(storedType != null, "saved state remains resolvable: " + storedState);
            place(world, center, storedType.getId());
            evaluate(world);
            var lever = BlockComponentAccess.getIfLoaded(world, center, LogicSimPlugin.getLeverComponentType());
            check(lever != null && lever.isOn() == LogicBlockTypes.isOnState(storedState), "restores saved lever intention");
            terminal(world, center, lever.isOn(), 0);
            place(world, center, "Empty");
        }
        evaluate(world);
        passed.add("legacy and connected saved lever states retain their power intention");
    }

    private void terminal(World world, BlockPosition position, boolean on, int worldMask) {
        String expected = LogicBlockTypes.terminalState(on,
                BlockComponentAccess.localConnectionMask(world, position, worldMask));
        String actual = LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world, position));
        check(expected.equals(actual), "terminal " + position + " expected " + expected + ", got " + actual);
    }

    private void reload(World world) {
        check(LogicBlockTypes.isLogicLever(BlockComponentAccess.getBlockType(world, LEVER)), "saved lever exists");
        var lever = BlockComponentAccess.getIfLoaded(world, LEVER, LogicSimPlugin.getLeverComponentType());
        check(lever != null && lever.isOn(), "lever On survives actual server restart");
        evaluate(world);
        lamp(world, LAMP, true);
        lamp(world, p(32, 2), true);
        lamp(world, p(4, -4), false);
        passed.add("real server restart rebuilds independent networks and restores source");
        ToggleLogicLeverInteraction.toggle(world, LEVER);
        evaluate(world);
        lamp(world, LAMP, false);
        lamp(world, p(32, 2), false);
        passed.add("restarted circuit toggles off correctly");
        ToggleLogicLeverInteraction.toggle(world, LEVER);
        evaluate(world);
    }

    private CompletableFuture<Void> automaticUpdates(World world) {
        ToggleLogicLeverInteraction.toggle(world, LEVER);
        terminal(world, LEVER, false, 2);
        return afterTicks(world, () -> {
            lamp(world, LAMP, false);
            terminal(world, LAMP, false, 8);
            ToggleLogicLeverInteraction.toggle(world, LEVER);
            place(world, p(32, 0), "Empty");
        }).thenCompose(ignored -> afterTicks(world, () -> {
            lamp(world, LAMP, false);
            terminal(world, LAMP, false, 0);
            place(world, p(32, 0), LogicBlockTypes.CABLE_ID);
        })).thenCompose(ignored -> afterTicks(world, () -> {
            lamp(world, LAMP, true);
            terminal(world, LAMP, true, 8);
            passed.add("world tick automatically updates toggles, removal and reconnection");
        })).thenCompose(ignored -> inWorld(world, () -> ChunkSavingSystems.saveChunksInWorld(
                world.getChunkStore().getStore(), world))).thenCompose(future -> future)
                .thenCompose(ignored -> inWorld(world, () -> {
                    var sectionRef = BlockComponentAccess.sectionIfLoaded(world, LEVER);
                    var section = world.getChunkStore().getStore().getComponent(
                            sectionRef, ChunkSection.getComponentType());
                    // Column storage must unload the entire column, not drop an individual section.
                    world.getChunkStore().remove(section.getChunkColumnReference(), RemoveReason.UNLOAD);
                    return null;
                })).thenCompose(ignored -> afterTicks(world, () -> {
                    check(BlockComponentAccess.sectionIfLoaded(world, LEVER) == null, "source section unloaded");
                    lamp(world, LAMP, false);
                    passed.add("unloaded source section disconnects its network without forced loading");
                })).thenCompose(ignored -> world.getChunkStore().getChunkSectionReferenceAtBlockAsync(
                        LEVER.x(), LEVER.y(), LEVER.z()))
                .thenCompose(ignored -> afterTicks(world, () -> {
                    var lever = BlockComponentAccess.getIfLoaded(world, LEVER,
                            LogicSimPlugin.getLeverComponentType());
                    var type = BlockComponentAccess.getBlockType(world, LEVER);
                    check(lever != null && lever.isOn(), "source state restored on section reload: type="
                            + (type == null ? null : type.getId()) + ", component=" + lever);
                    lamp(world, LAMP, true);
                    passed.add("section reload automatically restores source and updates lamp");
                }));
    }

    private CompletableFuture<Void> afterTicks(World world, Runnable action) {
        var result = new CompletableFuture<Void>();
        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS).execute(() ->
                inWorld(world, () -> { action.run(); return null; }).whenComplete((ignored, error) -> {
                    if (error == null) result.complete(null);
                    else result.completeExceptionally(error);
                }));
        return result;
    }

    private <T> CompletableFuture<T> inWorld(World world, Supplier<T> action) {
        var result = new CompletableFuture<T>();
        world.execute(() -> {
            try { result.complete(action.get()); }
            catch (Throwable error) { result.completeExceptionally(error); }
        });
        return result;
    }

    private void evaluate(World world) {
        EnergyPropagationSystem.recalculate(world);
    }

    private void lamp(World world, BlockPosition position, boolean expected) {
        String state = LogicBlockTypes.stateName(BlockComponentAccess.getBlockType(world, position));
        check(state != null && state.contains("_Connections_") && LogicBlockTypes.isOnState(state) == expected,
                "lamp " + position + " expected " + expected + ", got " + state);
    }

    private void place(World world, BlockPosition position, String id) {
        place(world, position, id, RotationTuple.NONE_INDEX);
    }

    private void place(World world, BlockPosition position, String id, int rotation) {
        var section = BlockComponentAccess.sectionIfLoaded(world, position);
        check(section != null, "test section loaded: " + position);
        var type = BlockType.getAssetMap().getAsset(id);
        check(type != null, "block asset exists: " + id);
        BlockOperations.setBlock(world.getChunkStore(), section, position.x(), position.y(), position.z(),
                BlockType.getAssetMap().getIndex(id), type, rotation, 0, 0);
    }

    private void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private void finish(String phase, Throwable failure) {
        try {
            var result = Map.of("phase", phase, "passed", failure == null,
                    "checks", List.copyOf(passed), "measurements", measurements, "error", failure == null ? "" : failure.toString());
            Files.writeString(Path.of("selftest-" + phase + ".json"),
                    new GsonBuilder().setPrettyPrinting().create().toJson(result));
        } catch (Exception error) { error.printStackTrace(); }
        if (failure != null) failure.printStackTrace();
        HytaleServer.get().shutdownServer(failure == null ? ShutdownReason.SHUTDOWN : ShutdownReason.CRASH);
    }
}
