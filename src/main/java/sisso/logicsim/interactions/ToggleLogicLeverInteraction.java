package sisso.logicsim.interactions;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.util.BlockComponentAccess;
import sisso.logicsim.util.LogicBlockTypes;
import org.joml.Vector3i;

public final class ToggleLogicLeverInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<ToggleLogicLeverInteraction> CODEC = BuilderCodec.builder(
            ToggleLogicLeverInteraction.class, ToggleLogicLeverInteraction::new,
            SimpleBlockInteraction.CODEC).build();

    @Override
    protected void interactWithBlock(World world, CommandBuffer<EntityStore> commandBuffer,
            InteractionType type, InteractionContext context, ItemStack itemStack,
            Vector3i target, CooldownHandler cooldownHandler) {
        var position = new BlockPosition(target.x, target.y, target.z);
        // Queue the authoritative operation outside both stores' ECS processing.
        var player=commandBuffer.getComponent(context.getEntity(),com.hypixel.hytale.server.core.universe.PlayerRef.getComponentType());
        world.execute(() -> {
            var areas=LogicSimPlugin.getAreas();
            if(player!=null && (areas==null || areas.allows(player.getUuid(),world.getWorldConfig().getUuid(),position.x(),position.y(),position.z()))) toggle(world, position);
        });
    }

    public static boolean toggle(World world, BlockPosition position) {
        var type = BlockComponentAccess.getBlockType(world, position);
        if (!LogicBlockTypes.isLogicLever(type)) return false;
        var lever = BlockComponentAccess.getIfLoaded(world, position,
                LogicSimPlugin.getLeverComponentType());
        if (lever == null) return false;
        lever.restoreFromStoredVisualStateIfNeeded(type);
        lever.toggle();
        BlockComponentAccess.markNeedsSavingIfLoaded(world, position);
        BlockComponentAccess.applyVisualState(world, position,
                LogicBlockTypes.terminalState(lever.isOn(),
                        LogicBlockTypes.connectionsInState(LogicBlockTypes.stateName(type))));
        world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType()).invalidate();
        return true;
    }

    @Override
    protected void simulateInteractWithBlock(InteractionType type, InteractionContext context,
            ItemStack itemStack, World world, Vector3i target) {
        // State is changed only by the server-side interaction.
    }
}
