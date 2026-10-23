package sisso.logicsim.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import com.hypixel.hytale.server.core.universe.Universe;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.util.BuildInfo;

/** Read-only diagnostics; console and operators with the explicit permission. */
public final class DebugEnergyCommand extends CommandBase {
    public DebugEnergyCommand() {
        super("logicenergy", "Shows loaded LogicGates circuit statistics.");
        requirePermission("sisso.logicgates.debug");
    }

    @Override
    protected void executeSync(CommandContext context) {
        context.sendMessage(Message.raw("LogicGates " + BuildInfo.BUILD_ID + " | Hytale " + BuildInfo.HYTALE_VERSION));
        var worlds = Universe.get().getWorlds();
        if (worlds.isEmpty()) context.sendMessage(Message.raw("LogicGates: no worlds loaded."));
        for (var world : worlds.values()) {
            world.execute(() -> {
                var state = world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
                context.sendMessage(Message.raw("LogicGates [" + world.getName() + "]: "
                        + state.nodeCount + " nodes, " + state.poweredCableCount + " powered cables, "
                        + state.litLampCount + " lit lamps, " + state.gateCount + " gates ("
                        + state.activeGateCount + " active, " + state.pendingGateCount + " awaiting sections, "
                        + state.feedbackGateCount + " indeterminate feedback), "
                        + state.evaluations + " evaluations."));
            });
        }
    }
}
