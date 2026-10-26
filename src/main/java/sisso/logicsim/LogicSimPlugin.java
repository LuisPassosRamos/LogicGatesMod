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
import sisso.logicsim.classroom.ClassroomPresence;
import sisso.logicsim.classroom.ClassroomPermissions;
import com.hypixel.hytale.server.core.event.events.permissions.PlayerPermissionChangeEvent;
import com.hypixel.hytale.server.core.event.events.permissions.PlayerGroupEvent;
import com.hypixel.hytale.server.core.event.events.permissions.GroupPermissionChangeEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;

public final class LogicSimPlugin extends JavaPlugin {
    private static sisso.logicsim.classroom.learning.AreaRuntime areas;
    public static sisso.logicsim.classroom.learning.AreaRuntime getAreas() { return areas; }
    private static ComponentType<ChunkStore, LeverComponent> leverComponentType;
    private static ComponentType<ChunkStore, LampComponent> lampComponentType;
    private static ComponentType<ChunkStore, CableComponent> cableComponentType;
    private static ComponentType<ChunkStore, GateComponent> gateComponentType;
    private static ResourceType<ChunkStore, CircuitWorldState> circuitStateType;
    private static ClassroomRuntime classroom;
    private ClassroomPresence classroomPresence;

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
        getCodecRegistry(Interaction.CODEC).register("LogicGates_ClassroomSelection",
                sisso.logicsim.interactions.ClassroomSelectionInteraction.class,sisso.logicsim.interactions.ClassroomSelectionInteraction.CODEC);
        getCommandRegistry().registerCommand(new DebugEnergyCommand());
        getCommandRegistry().registerCommand(new InspectGateCommand());
        getCommandRegistry().registerCommand(new LogicHelpCommand());
        PermissionsModule.registerPermission(ClassroomRuntime.TEACH_PERMISSION);
        getLogger().atInfo().log("Classroom database: %s", getDataDirectory().resolve("classroom.db").toAbsolutePath().normalize());
        classroom = new ClassroomRuntime(getDataDirectory(),
                actor -> PermissionsModule.get().hasPermission(actor, ClassroomRuntime.TEACH_PERMISSION, false),
                failure -> getLogger().atSevere().withCause(failure).log("Classroom storage unavailable; circuit systems remain active"));
        areas=new sisso.logicsim.classroom.learning.AreaRuntime(classroom);
        registry.registerSystem(new sisso.logicsim.classroom.learning.AreaRecoverySystem(areas));
        var entities=getEntityStoreRegistry();
        entities.registerSystem(new sisso.logicsim.classroom.learning.AreaEditGuard.Place(areas));
        entities.registerSystem(new sisso.logicsim.classroom.learning.AreaEditGuard.Break(areas));
        entities.registerSystem(new sisso.logicsim.classroom.learning.AreaEditGuard.Damage(areas));
        entities.registerSystem(new sisso.logicsim.classroom.learning.AreaEditGuard.Use(areas));
        getCommandRegistry().registerCommand(new ClassroomCommand(classroom));
        classroomPresence = new ClassroomPresence(classroom);
        getEventRegistry().registerGlobal(PlayerReadyEvent.class, classroomPresence::ready);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class, classroomPresence::disconnected);
        getEventRegistry().registerGlobal(PlayerDisconnectEvent.class,event->areas.disconnected(event.getPlayerRef().getUuid()));
        getEventRegistry().registerGlobal(PlayerReadyEvent.class,event->{
            var ref=event.getPlayerRef();
            if(ref!=null && ref.isValid()) {
                var player=ref.getStore().getComponent(ref,com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
                if(player!=null)areas.observe(player,ref.getStore().getExternalData().getWorld());
            }
        });
        getEventRegistry().registerGlobal(com.hypixel.hytale.server.core.event.events.player.RemovedPlayerFromWorldEvent.class,event->{
            var player=event.getHolder().getComponent(com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
            if(player!=null)areas.removedFromWorld(player.getUuid(),event.getHolder().getComponent(com.hypixel.hytale.server.core.entity.entities.Player.getComponentType()));
        });
        var classroomPermissions = new ClassroomPermissions(classroom);
        getEventRegistry().registerGlobal(PlayerPermissionChangeEvent.PermissionsAdded.class, classroomPermissions::playerChanged);
        getEventRegistry().registerGlobal(PlayerPermissionChangeEvent.PermissionsRemoved.class, classroomPermissions::playerChanged);
        getEventRegistry().registerGlobal(PlayerGroupEvent.Added.class, classroomPermissions::playerChanged);
        getEventRegistry().registerGlobal(PlayerGroupEvent.Removed.class, classroomPermissions::playerChanged);
        getEventRegistry().registerGlobal(GroupPermissionChangeEvent.Added.class, classroomPermissions::groupChanged);
        getEventRegistry().registerGlobal(GroupPermissionChangeEvent.Removed.class, classroomPermissions::groupChanged);
    }

    @Override protected void shutdown() {
        if(areas!=null)areas.close();
        if (classroomPresence != null) classroomPresence.close();
        if (classroom != null) classroom.close();
    }
}
