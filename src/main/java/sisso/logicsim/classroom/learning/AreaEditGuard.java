package sisso.logicsim.classroom.learning;

import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.*;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.event.events.ecs.*;
import org.joml.Vector3i;

/** Player edits consult leases only at interaction time; there is no per-tick region scan. */
public final class AreaEditGuard {
    private AreaEditGuard() { }
    private abstract static class Guard<E extends EcsEvent> extends EntityEventSystem<EntityStore,E> {
        final AreaRuntime areas;
        Guard(AreaRuntime areas,Class<E> type){super(type);this.areas=areas;}
        @Override public Query<EntityStore> getQuery(){return PlayerRef.getComponentType();}
        boolean denied(int i,ArchetypeChunk<EntityStore> chunk,Store<EntityStore> store,Vector3i p){
            var player=chunk.getComponent(i,PlayerRef.getComponentType());
            return player==null || !areas.allows(player.getUuid(),store.getExternalData().getWorld().getWorldConfig().getUuid(),p.x,p.y,p.z);
        }
    }
    public static final class Place extends Guard<PlaceBlockEvent> {
        public Place(AreaRuntime a){super(a,PlaceBlockEvent.class);}
        @Override public void handle(int i,ArchetypeChunk<EntityStore> c,Store<EntityStore> s,CommandBuffer<EntityStore> b,PlaceBlockEvent e){
            if(e.isCancelled())return;
            var player=c.getComponent(i,PlayerRef.getComponentType());var stack=e.getItemInHand();
            String block=stack==null?null:stack.getItem().getBlockId();
            var type=block==null?null:com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType.getAssetMap().getAsset(block);
            if(player==null || !areas.allowsPlacement(player.getUuid(),s.getExternalData().getWorld().getWorldConfig().getUuid(),e.getTargetBlock(),type,e.getRotation().index()))e.setCancelled(true);
        }
    }
    public static final class Break extends Guard<BreakBlockEvent> {
        public Break(AreaRuntime a){super(a,BreakBlockEvent.class);}
        @Override public void handle(int i,ArchetypeChunk<EntityStore> c,Store<EntityStore> s,CommandBuffer<EntityStore> b,BreakBlockEvent e){if(denied(i,c,s,e.getTargetBlock()))e.setCancelled(true);}
    }
    public static final class Damage extends Guard<DamageBlockEvent> {
        public Damage(AreaRuntime a){super(a,DamageBlockEvent.class);}
        @Override public void handle(int i,ArchetypeChunk<EntityStore> c,Store<EntityStore> s,CommandBuffer<EntityStore> b,DamageBlockEvent e){if(denied(i,c,s,e.getTargetBlock()))e.setCancelled(true);}
    }
    public static final class Use extends Guard<UseBlockEvent.Pre> {
        public Use(AreaRuntime a){super(a,UseBlockEvent.Pre.class);}
        @Override public void handle(int i,ArchetypeChunk<EntityStore> c,Store<EntityStore> s,CommandBuffer<EntityStore> b,UseBlockEvent.Pre e){if(denied(i,c,s,e.getTargetBlock()))e.setCancelled(true);}
    }
}
