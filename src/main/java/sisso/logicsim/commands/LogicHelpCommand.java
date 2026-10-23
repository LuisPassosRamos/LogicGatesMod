package sisso.logicsim.commands;

import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.arguments.system.OptionalArg;
import com.hypixel.hytale.server.core.command.system.arguments.types.ArgTypes;
import com.hypixel.hytale.server.core.command.system.basecommands.CommandBase;
import sisso.logicsim.circuit.CircuitNode;
import sisso.logicsim.diagnostics.GateTruthTable;
import sisso.logicsim.util.GateCatalog;
import java.util.Locale;
import java.util.stream.Collectors;
import static sisso.logicsim.util.LogicMessages.text;

public final class LogicHelpCommand extends CommandBase {
    private final OptionalArg<String> gate = withOptionalArg("gate", "server.logicgates.args.gate", ArgTypes.STRING);
    private final OptionalArg<Integer> inputs = withOptionalArg("inputs", "server.logicgates.args.inputs", ArgTypes.INTEGER);

    public LogicHelpCommand() {
        super("logichelp", "server.logicgates.commands.help.description");
        requireNoPermission();
    }

    @Override protected void executeSync(CommandContext context) {
        context.sendMessage(text("help.intro"));
        context.sendMessage(text("help.optional"));
        context.sendMessage(text("help.feedback"));
        String requested = gate.get(context);
        if (requested == null) {
            context.sendMessage(text("help.usage"));
            for (var entry : GateCatalog.gates()) context.sendMessage(Message.translation(
                    "server.items.logic_" + entry.kind().name().toLowerCase(Locale.ROOT) + "_gate.description"));
            return;
        }
        CircuitNode.Kind kind;
        try { kind = CircuitNode.Kind.valueOf(requested.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException error) { context.sendMessage(text("help.invalidGate")); return; }
        Integer count = inputs.get(context);
        if (count == null) count = kind == CircuitNode.Kind.NOT ? 1 : 2;
        if (!kind.isGate() || count < 1 || count > (kind == CircuitNode.Kind.NOT ? 1 : 3)) {
            context.sendMessage(text("help.invalidInputs")); return;
        }
        String labels = kind == CircuitNode.Kind.NOT ? "C" : String.join(", ", java.util.List.of("A", "B", "C").subList(0, count));
        context.sendMessage(text("help.table").param("gate", kind.name()).param("inputs", labels));
        for (var row : GateTruthTable.rows(kind, count)) context.sendMessage(text("help.row")
                .param("inputs", row.inputs().stream().map(value -> value ? "1" : "0").collect(Collectors.joining(" ")))
                .param("output", row.output() ? 1 : 0));
    }
}
