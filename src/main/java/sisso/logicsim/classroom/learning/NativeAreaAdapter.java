package sisso.logicsim.classroom.learning;

import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockOperations;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.chunk.section.FluidSection;
import com.hypixel.hytale.server.core.prefab.selection.standard.BlockSelection;
import com.hypixel.hytale.server.core.event.events.ecs.*;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.component.*;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.circuit.BlockPosition;
import sisso.logicsim.util.*;
import sisso.logicsim.classroom.learning.AreaSessions.Bounds;
import org.joml.Vector3i;
import java.util.*;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;

/** All world reads/writes run in the world context. Never asks the chunk loader for data. */
public final class NativeAreaAdapter {
    private NativeAreaAdapter() { }
    public static AreaSnapshot capture(World world,Bounds bounds,LearningLimits limits) {
        if(bounds.width()<1 || bounds.height()<1 || bounds.depth()<1 || bounds.width()>limits.width()
                || bounds.height()>limits.height() || bounds.depth()>limits.depth()) throw new LearningException(AREA_LIMIT);
        var result=new ArrayList<AreaSnapshot.Block>();
        for(int y=0;y<bounds.height();y++) for(int z=0;z<bounds.depth();z++) for(int x=0;x<bounds.width();x++) {
            var p=new BlockPosition(bounds.x()+x,bounds.y()+y,bounds.z()+z); var section=loaded(world,p);
            if(fluid(world,p))throw new LearningException(AREA_UNSUPPORTED).problem("fluid @ "+p.x()+","+p.y()+","+p.z());
            int id=section.get(p.x(),p.y(),p.z());if(id==0)continue;
            var type=BlockType.getAssetMap().getAsset(id);
            try{supported(type);if(section.getFiller(p.x(),p.y(),p.z())!=0)throw new LearningException(AREA_UNSUPPORTED);}
            catch(LearningException e){throw e.problem((type==null?"?":type.getId())+" @ "+p.x()+","+p.y()+","+p.z());}
            Boolean on=null;
            if(LogicBlockTypes.isLogicLever(type)) {
                var lever=BlockComponentAccess.getIfLoaded(world,p,LogicSimPlugin.getLeverComponentType());
                if(lever==null) throw new LearningException(AREA_UNSUPPORTED);
                lever.restoreFromStoredVisualStateIfNeeded(type); on=lever.isOn();
            }
            result.add(new AreaSnapshot.Block(x,y,z,type.getId(),section.getRotationIndex(p.x(),p.y(),p.z()),section.getFiller(p.x(),p.y(),p.z()),on));
        }
        var snapshot=new AreaSnapshot(1,bounds.width(),bounds.height(),bounds.depth(),result);snapshot.validate(limits);return snapshot;
    }
    private static BlockSection loaded(World world,BlockPosition p) {
        var ref=BlockComponentAccess.sectionIfLoaded(world,p);if(ref==null)throw new LearningException(AREA_UNLOADED);
        var blocks=world.getChunkStore().getStore().getComponent(ref,BlockSection.getComponentType());
        if(blocks==null)throw new LearningException(AREA_UNLOADED);return blocks;
    }
    private static boolean fluid(World world,BlockPosition p) {
        var section=BlockComponentAccess.sectionIfLoaded(world,p);if(section==null)throw new LearningException(AREA_UNLOADED);
        var fluids=world.getChunkStore().getStore().getComponent(section,FluidSection.getComponentType());
        return fluids!=null && fluids.getFluidId(p.x(),p.y(),p.z())!=0;
    }
    public static void supported(BlockType type) {
        if(type==null)throw new LearningException(AREA_UNSUPPORTED);
        if(LogicBlockTypes.kind(type)!=null)return;
        var hitbox=BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
        if(hitbox!=null && hitbox.protrudesUnitBox())throw new LearningException(AREA_UNSUPPORTED);
        if(type.getBlockEntity()!=null || type.getExplosionConfig()!=null || type.getFarming()!=null || type.getFallingBlockSettings()!=null || type.getTickProcedure()!=null || type.getRandomTickProcedure()!=null
                || type.getInteractions()!=null && !type.getInteractions().isEmpty()) throw new LearningException(AREA_UNSUPPORTED);
    }
    public static BlockSelection selection(AreaSnapshot snapshot) {
        var selection=new BlockSelection();
        for(var b:snapshot.blocks()) {
            int id=BlockType.getAssetMap().getIndex(b.asset());
            if(id<=0)throw new LearningException(AREA_UNSUPPORTED);
            supported(BlockType.getAssetMap().getAsset(id));
            selection.addBlockAtLocalPos(b.x(),b.y(),b.z(),id,b.rotation(),b.filler(),0);
        }
        selection.setSelectionArea(new Vector3i(),new Vector3i(snapshot.width()-1,snapshot.height()-1,snapshot.depth()-1));
        selection.setAnchor(0,0,0);return selection;
    }
    public static BuilderToolPrefabPreview preview(AreaSnapshot snapshot) {
        var selection=selection(snapshot);var data=selection.toPacket();var packet=new BuilderToolPrefabPreview();
        packet.blocksChange=data.blocksChange;packet.bounds=selection.toEditorSelectionBounds();packet.tilt=23;packet.spinSpeed=0;packet.previewScale=100;
        return packet;
    }
    public static void requireEmpty(World world,Bounds b) {
        for(int y=0;y<b.height();y++)for(int z=0;z<b.depth();z++)for(int x=0;x<b.width();x++) {
            var p=new BlockPosition(b.x()+x,b.y()+y,b.z()+z);
            if(loaded(world,p).get(p.x(),p.y(),p.z())!=0 || fluid(world,p))throw new LearningException(AREA_OCCUPIED);
        }
    }
    public static void preflight(World world,Ref<EntityStore> actor,AreaSnapshot snapshot,Bounds bounds) {
        requireEmpty(world,bounds);var store=actor.getStore();
        for(var b:snapshot.blocks()) {
            var type=BlockType.getAssetMap().getAsset(b.asset());supported(type);
            var target=new Vector3i(bounds.x()+b.x(),bounds.y()+b.y(),bounds.z()+b.z());
            var original=new Vector3i(target);
            var event=new PlaceBlockEvent(new ItemStack(type.getItem()==null?b.asset():type.getItem().getId()),target,RotationTuple.get(b.rotation()));
            event.setConsumeItem(false);store.invoke(actor,event);
            if(event.isCancelled() || !original.equals(event.getTargetBlock()) || event.getRotation().index()!=b.rotation()) throw new LearningException(AREA_PROTECTED);
        }
    }
    public static void paste(World world,AreaSnapshot snapshot,Bounds bounds) {
        requireEmpty(world,bounds);
        for(var b:snapshot.blocks()) {
            var p=new BlockPosition(bounds.x()+b.x(),bounds.y()+b.y(),bounds.z()+b.z());
            set(world,p,b.asset(),b.rotation(),b.filler());
            if(b.leverOn()!=null) {
                var lever=BlockComponentAccess.getIfLoaded(world,p,LogicSimPlugin.getLeverComponentType());
                if(lever==null)throw new LearningException(AREA_UNSUPPORTED);
                lever.restoreFromStoredVisualStateIfNeeded(BlockComponentAccess.getBlockType(world,p));
                if(lever.isOn()!=b.leverOn())lever.toggle();
                BlockComponentAccess.markNeedsSavingIfLoaded(world,p);
            }
        }
        world.getChunkStore().getStore().getResource(LogicSimPlugin.getCircuitStateType()).invalidate();
    }
    public static void discard(World world,Ref<EntityStore> actor,Bounds b) {
        // Complete validation precedes removal: cancelled protection events leave the copy intact.
        for(int y=0;y<b.height();y++)for(int z=0;z<b.depth();z++)for(int x=0;x<b.width();x++) {
            var p=new BlockPosition(b.x()+x,b.y()+y,b.z()+z);var blocks=loaded(world,p);int id=blocks.get(p.x(),p.y(),p.z());
            if(id==0)continue;
            var event=new BreakBlockEvent(ItemStack.EMPTY,new Vector3i(p.x(),p.y(),p.z()),BlockType.getAssetMap().getAsset(id));
            actor.getStore().invoke(actor,event);
            if(event.isCancelled() || !event.getTargetBlock().equals(new Vector3i(p.x(),p.y(),p.z())))throw new LearningException(AREA_PROTECTED);
        }
        clear(world,b);
    }
    /** Recovery of our exclusively leased, originally empty volume; never called for arbitrary coordinates. */
    public static void clear(World world,Bounds b) {
        requireLoaded(world,b);
        for(int y=0;y<b.height();y++)for(int z=0;z<b.depth();z++)for(int x=0;x<b.width();x++) {
            var p=new BlockPosition(b.x()+x,b.y()+y,b.z()+z);loaded(world,p);set(world,p,"Empty",0,0);
        }
    }
    public static void requireLoaded(World world,Bounds b) {
        // Check every intersecting section, without loading or generating terrain.
        for(int x=b.x()>>5;x<=((long)b.x()+b.width()-1)>>5;x++)
            for(int y=b.y()>>5;y<=((long)b.y()+b.height()-1)>>5;y++)
                for(int z=b.z()>>5;z<=((long)b.z()+b.depth()-1)>>5;z++)loaded(world,new BlockPosition(x<<5,y<<5,z<<5));
    }
    private static void set(World world,BlockPosition p,String asset,int rotation,int filler) {
        var ref=BlockComponentAccess.sectionIfLoaded(world,p);if(ref==null)throw new LearningException(AREA_UNLOADED);
        int id=asset.equals("Empty")?0:BlockType.getAssetMap().getIndex(asset);
        var type=BlockType.getAssetMap().getAsset(id);if(type==null)throw new LearningException(AREA_UNSUPPORTED);
        if(!BlockOperations.setBlock(world.getChunkStore(),ref,p.x(),p.y(),p.z(),id,type,rotation,filler,0)
                && (loaded(world,p).get(p.x(),p.y(),p.z())!=id || loaded(world,p).getRotationIndex(p.x(),p.y(),p.z())!=rotation))
            throw new LearningException(AREA_UNSUPPORTED).problem(asset+" @ "+p.x()+","+p.y()+","+p.z());
    }
}
