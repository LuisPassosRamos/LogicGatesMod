package sisso.logicsim.commands;

import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractAsyncCommand;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import com.hypixel.hytale.server.core.universe.Universe;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.util.BuildInfo;
import static sisso.logicsim.util.LogicMessages.text;

/** Read-only diagnostics; console and operators with the explicit permission. */
public final class DebugEnergyCommand extends AbstractAsyncCommand {
    public DebugEnergyCommand() {
        super("logicenergy", "server.logicgates.commands.energy.description");
        requirePermission("sisso.logicgates.debug");
    }

    @Override
    protected CompletableFuture<Void> executeAsync(CommandContext context) {
        context.sendMessage(text("build").param("build", BuildInfo.BUILD_ID).param("hytale", BuildInfo.HYTALE_VERSION));
        var worlds = Universe.get().getWorlds();
        if (worlds.isEmpty()) context.sendMessage(text("energy.noWorlds"));
        var pending = new ArrayList<CompletableFuture<Void>>();
        for (var world : worlds.values()) {
            pending.add(CompletableFuture.runAsync(() -> sendReport(context, world), world));
        }
        return CompletableFuture.allOf(pending.toArray(CompletableFuture[]::new));
    }


    private static void sendReport(CommandContext context, com.hypixel.hytale.server.core.universe.world.World world) {
        var state = world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
        context.sendMessage(text("energy.world").param("world", world.getName())
                .param("nodes", state.nodeCount).param("cables", state.poweredCableCount)
                .param("lamps", state.litLampCount).param("gates", state.gateCount)
                .param("active", state.activeGateCount).param("pending", state.pendingGateCount)
                .param("feedback", state.feedbackGateCount).param("evaluations", state.evaluations));
        var timing = state.metrics.summary();
        context.sendMessage(text("energy.timing").param("last", milliseconds(timing.lastMs()))
                .param("mean", milliseconds(timing.meanMs())).param("p95", milliseconds(timing.p95Ms()))
                .param("max", milliseconds(timing.maxMs())).param("solver", milliseconds(timing.solverMs()))
                .param("monitor", milliseconds(timing.monitoringMeanMs())).param("samples", timing.samples()));
    }

    private static String milliseconds(double value) { return String.format(java.util.Locale.ROOT, "%.3f", value); }
}
