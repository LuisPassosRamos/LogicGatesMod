package sisso.logicsim;

import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.ResourceType;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import sisso.logicsim.components.CableComponent;
import sisso.logicsim.components.GateComponent;
import sisso.logicsim.commands.DebugEnergyCommand;
import sisso.logicsim.commands.ClassroomCommand;
import sisso.logicsim.commands.InspectGateCommand;
import sisso.logicsim.commands.LogicHelpCommand;
import sisso.logicsim.components.LampComponent;
import sisso.logicsim.components.LeverComponent;
import sisso.logicsim.interactions.ToggleLogicLeverInteraction;
import sisso.logicsim.systems.CircuitWorldState;
import sisso.logicsim.systems.EnergyInitializerSystem;
import sisso.logicsim.systems.EnergyPropagationSystem;
import sisso.logicsim.util.BuildInfo;
import sisso.logicsim.classroom.ClassroomRuntime;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

public final class LogicSimPlugin extends JavaPlugin {
    private static ComponentType<ChunkStore, LeverComponent> leverComponentType;
    private static ComponentType<ChunkStore, LampComponent> lampComponentType;
    private static ComponentType<ChunkStore, CableComponent> cableComponentType;
    private static ComponentType<ChunkStore, GateComponent> gateComponentType;
    private static ResourceType<ChunkStore, CircuitWorldState> circuitStateType;
    private static ClassroomRuntime classroom;

    public LogicSimPlugin(JavaPluginInit init) { super(init); }

    public static ComponentType<ChunkStore, LeverComponent> getLeverComponentType() { return leverComponentType; }
    public static ComponentType<ChunkStore, LampComponent> getLampComponentType() { return lampComponentType; }
    public static ComponentType<ChunkStore, CableComponent> getCableComponentType() { return cableComponentType; }
    public static ComponentType<ChunkStore, GateComponent> getGateComponentType() { return gateComponentType; }
    public static ResourceType<ChunkStore, CircuitWorldState> getCircuitStateType() { return circuitStateType; }
    public static ClassroomRuntime getClassroom() { return classroom; }

    @Override
    protected void setup() {
        getLogger().atInfo().log("LogicGates %s | Hytale %s", BuildInfo.BUILD_ID, BuildInfo.HYTALE_VERSION);
        var registry = getChunkStoreRegistry();
        leverComponentType = registry.registerComponent(LeverComponent.class, "LogicGates_Lever", LeverComponent.CODEC);
        lampComponentType = registry.registerComponent(LampComponent.class, "LogicGates_Lamp", LampComponent.CODEC);
        cableComponentType = registry.registerComponent(CableComponent.class, "LogicGates_Cable", CableComponent.CODEC);
        gateComponentType = registry.registerComponent(GateComponent.class, "LogicGates_Gate", GateComponent.CODEC);
        circuitStateType = registry.registerResource(CircuitWorldState.class, CircuitWorldState::new);
        registry.registerSystem(new EnergyInitializerSystem());
        registry.registerSystem(new EnergyInitializerSystem.LoadedSections());
        registry.registerSystem(new EnergyPropagationSystem());
        getCodecRegistry(Interaction.CODEC).register("LogicGates_ToggleLogicLever",
                ToggleLogicLeverInteraction.class, ToggleLogicLeverInteraction.CODEC);
        getCommandRegistry().registerCommand(new DebugEnergyCommand());
        getCommandRegistry().registerCommand(new InspectGateCommand());
        getCommandRegistry().registerCommand(new LogicHelpCommand());
        PermissionsModule.registerPermission(ClassroomRuntime.TEACH_PERMISSION);
        getLogger().atInfo().log("Classroom database: %s", getDataDirectory().resolve("classroom.db").toAbsolutePath().normalize());
        classroom = new ClassroomRuntime(getDataDirectory(),
                actor -> PermissionsModule.get().hasPermission(actor, ClassroomRuntime.TEACH_PERMISSION, false),
                failure -> getLogger().atSevere().withCause(failure).log("Classroom storage unavailable; circuit systems remain active"));
        getCommandRegistry().registerCommand(new ClassroomCommand(classroom));
    }

    @Override protected void shutdown() {
        if (classroom != null) classroom.close();
    }
}
