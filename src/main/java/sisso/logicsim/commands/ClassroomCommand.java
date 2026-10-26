package sisso.logicsim.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.command.system.arguments.system.FlagArg;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.classroom.ui.ClassroomProbePage;
import sisso.logicsim.classroom.ui.ClassroomPage;
import sisso.logicsim.classroom.ClassroomRuntime;

/** AbstractPlayerCommand dispatches opening the page onto the player's world thread. */
public final class ClassroomCommand extends AbstractPlayerCommand {
    private final ClassroomRuntime runtime;
    private final FlagArg probe = withFlagArg("probe", "server.logicgates.args.classroomProbe");
    public ClassroomCommand(ClassroomRuntime runtime) {
        super("classroom", "server.logicgates.commands.classroom.description");
        requireNoPermission();
        this.runtime = runtime;
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef playerRef, World world) {
        var player = store.getComponent(ref, Player.getComponentType());
        if (player == null) return;
        if (Boolean.TRUE.equals(probe.get(context))) {
            player.getPageManager().openCustomPage(ref, store, new ClassroomProbePage(playerRef));
        } else if (sisso.logicsim.LogicSimPlugin.getAreas()!=null && sisso.logicsim.LogicSimPlugin.getAreas().current(playerRef,world)!=null) {
            var session=sisso.logicsim.LogicSimPlugin.getAreas().current(playerRef,world);
            sisso.logicsim.classroom.ui.LearningPage.open(ref,store,playerRef,runtime,world,session.activity,session.submission);
        } else {
            if(sisso.logicsim.LogicSimPlugin.getAreas()!=null)sisso.logicsim.LogicSimPlugin.getAreas().observe(playerRef,world);
            var page = new ClassroomPage(playerRef, runtime, world);
            player.getPageManager().openCustomPage(ref, store, page);
            page.load(ref, store);
        }
    }
}
