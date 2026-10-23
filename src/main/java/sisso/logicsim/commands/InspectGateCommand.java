package sisso.logicsim.commands;

import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.RequiredArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractWorldCommand;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.diagnostics.GateInspector;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BlockComponentAccess;
import java.util.Locale;
import static sisso.logicsim.util.LogicMessages.text;

/** Coordinates are explicit; the command never loads a section to inspect it. */
public final class InspectGateCommand extends AbstractWorldCommand {
    private final RequiredArg<Integer> x = withRequiredArg("x", "server.logicgates.args.x", ArgTypes.INTEGER);
    private final RequiredArg<Integer> y = withRequiredArg("y", "server.logicgates.args.y", ArgTypes.INTEGER);
    private final RequiredArg<Integer> z = withRequiredArg("z", "server.logicgates.args.z", ArgTypes.INTEGER);

    public InspectGateCommand() {
        super("logicinspect", "server.logicgates.commands.inspect.description");
        requirePermission("sisso.logicgates.inspect");
    }

    @Override protected void execute(CommandContext context, World world, Store<EntityStore> ignored) {
        var position = new BlockPosition(x.get(context), y.get(context), z.get(context));
        if (BlockComponentAccess.sectionIfLoaded(world, position) == null) {
            context.sendMessage(text("inspect.unloaded"));
            return;
        }
        var state = world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType());
        if (state.dirty) EnergyPropagationSystem.recalculate(world);
        var inspection = GateInspector.inspect(state.lastSnapshot, position);
        if (inspection.isEmpty()) { context.sendMessage(text("inspect.notGate")); return; }
        var gate = inspection.get();
        context.sendMessage(text("inspect.header").param("gate", gate.kind().name()).param("world", world.getName())
                .param("x", position.x()).param("y", position.y()).param("z", position.z())
                .param("evaluation", state.evaluations));
        context.sendMessage(text("inspect.output").param("direction", text("direction." + gate.outputDirection().name().toLowerCase(Locale.ROOT)))
                .param("value", gate.outputOn() ? 1 : 0));
        for (var input : gate.inputs()) context.sendMessage(text("inspect.input").param("label", input.label())
                .param("direction", text("direction." + input.direction().name().toLowerCase(Locale.ROOT)))
                .param("state", text("input." + input.state().name().toLowerCase(Locale.ROOT)))
                .param("x", input.neighbor().x()).param("y", input.neighbor().y()).param("z", input.neighbor().z()));
        context.sendMessage(text("reason." + gate.reason().name().toLowerCase(Locale.ROOT)));
    }
}
