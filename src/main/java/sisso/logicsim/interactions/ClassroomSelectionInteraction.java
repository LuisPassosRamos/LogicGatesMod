package sisso.logicsim.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.classroom.learning.*;
import org.joml.Vector3i;
import static sisso.logicsim.util.LogicMessages.text;

/** A narrow server interaction: it selects points and cannot invoke editor commands. */
public final class ClassroomSelectionInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<ClassroomSelectionInteraction> CODEC=BuilderCodec.builder(ClassroomSelectionInteraction.class,ClassroomSelectionInteraction::new,SimpleBlockInteraction.CODEC).build();
    @Override protected void interactWithBlock(World world,CommandBuffer<EntityStore> buffer,InteractionType type,InteractionContext context,ItemStack stack,Vector3i target,CooldownHandler cooldown) {
        var player=buffer.getComponent(context.getEntity(),PlayerRef.getComponentType());
        if(player==null || stack==null)return;
        var point=new AreaSessions.Point(target.x,target.y,target.z);
        world.execute(()->{
            try{LogicSimPlugin.getAreas().point(player,world,stack,point,type==InteractionType.Secondary);}
            catch(LearningException e){player.sendMessage(text("learning.error."+e.code().name()).param("problem",e.problem()));}
        });
    }
    @Override protected void simulateInteractWithBlock(InteractionType type,InteractionContext context,ItemStack stack,World world,Vector3i target) { }
}
