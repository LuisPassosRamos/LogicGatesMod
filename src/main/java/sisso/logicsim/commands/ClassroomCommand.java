package sisso.logicsim.commands;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.server.core.command.system.CommandContext;
import com.hypixel.hytale.server.core.command.system.basecommands.AbstractPlayerCommand;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.classroom.ui.ClassroomProbePage;

/** AbstractPlayerCommand dispatches opening the page onto the player's world thread. */
public final class ClassroomCommand extends AbstractPlayerCommand {
    public ClassroomCommand() {
        super("classroom", "server.logicgates.commands.classroom.description");
        requireNoPermission();
    }

    @Override
    protected void execute(CommandContext context, Store<EntityStore> store, Ref<EntityStore> ref,
                           PlayerRef playerRef, World world) {
        var player = store.getComponent(ref, Player.getComponentType());
        if (player != null) player.getPageManager().openCustomPage(ref, store, new ClassroomProbePage(playerRef));
    }
}
