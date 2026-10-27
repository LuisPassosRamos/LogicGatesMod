package sisso.logicsim.classroom.learning;

import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.protocol.packets.interface_.EditorBlocksChange;
import com.hypixel.hytale.protocol.packets.interface_.EditorSelection;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.bson.BsonString;
import com.hypixel.hytale.server.core.inventory.container.CombinedItemContainer;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.learning.AreaSessions.*;
import sisso.logicsim.classroom.learning.LearningViews.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;
import static sisso.logicsim.classroom.learning.LearningException.Code.*;
import static sisso.logicsim.util.LogicMessages.text;

/** Coordinates detached file work, scoped selection tools and durable temporary review volumes. */
public final class AreaRuntime implements AutoCloseable {
    public static final String TOOL="Logic_Classroom_Select";
    private final ClassroomRuntime runtime;
    public final AreaSessions sessions=new AreaSessions();
    private volatile AreaWorkQueue queue;
    private final Map<UUID,Review> reviews=new ConcurrentHashMap<>();
    private final Set<UUID> recovery=ConcurrentHashMap.newKeySet(),invalid=ConcurrentHashMap.newKeySet(),checking=ConcurrentHashMap.newKeySet();
    private final Map<UUID,AutoCloseable> authorizations=new ConcurrentHashMap<>();
    private final ThreadLocal<Boolean> cleanup=ThreadLocal.withInitial(()->false);
    private final AutoCloseable changes;
    private final Set<UUID> recovering=ConcurrentHashMap.newKeySet();
    private final Set<UUID> recoveryWorlds=ConcurrentHashMap.newKeySet(),retryWorlds=ConcurrentHashMap.newKeySet();
    private volatile boolean closed;
    private final CompletableFuture<Void> ready;
    public AreaRuntime(ClassroomRuntime runtime) {
        this.runtime=runtime;
        ready=runtime.learn(LearningStore::reviews).thenCompose(rows->{
            queue=new AreaWorkQueue(runtime.learningLimits().queue());
            if(closed){queue.close();return CompletableFuture.completedFuture(null);}
            for(var r:rows){reviews.put(r.teacher(),r);recovery.add(r.id());}
            return queue.submit(UUID.randomUUID(),()->{
                runtime.submissionFiles().recover(await(runtime.learn(LearningStore::contentIds)));return null;
            });
        });
        changes=runtime.listen(null,change->{for(var review:reviews.values())if(review.classroom().equals(change.classroom()) || review.teacher().equals(change.player()))revalidate(review);});
    }
    public CompletableFuture<Void> ready() { return ready; }
    public Session current(PlayerRef player,World world) { return sessions.current(player.getUuid(),world.getWorldConfig().getUuid(),player.getPacketHandler()); }
    public Review review(UUID teacher) { return reviews.get(teacher); }
    public boolean allows(UUID actor,UUID world,int x,int y,int z) {
        for(var r:reviews.values()) if(r.contains(world,x,y,z))
            return r.teacher().equals(actor) && (cleanup.get() || !invalid.contains(actor) && !recovery.contains(r.id()) && runtime.teaching(actor));
        return true;
    }
    public boolean reserved(UUID world,Bounds bounds) {
        var candidate=new Review(UUID.randomUUID(),UUID.randomUUID(),UUID.randomUUID(),null,world,bounds.x(),bounds.y(),bounds.z(),bounds.width(),bounds.height(),bounds.depth());
        return reviews.values().stream().anyMatch(candidate::overlaps);
    }
    public boolean allowsPlacement(UUID actor,UUID world,org.joml.Vector3i target,
            com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType type,int rotation) {
        if(!allows(actor,world,target.x,target.y,target.z))return false;
        for(var r:reviews.values())if(r.contains(world,target.x,target.y,target.z)) {
            try{NativeAreaAdapter.supported(type);}catch(LearningException unsupported){return false;}
        }
        if(type==null)return true;
        var boxes=com.hypixel.hytale.server.core.asset.type.blockhitbox.BlockBoundingBoxes.getAssetMap().getAsset(type.getHitboxTypeIndex());
        if(boxes!=null && boxes.protrudesUnitBox()) {
            var box=boxes.get(rotation).getBoundingBox();
            for(var r:reviews.values())if(r.world().equals(world) && target.x+box.min.x<r.x()+(long)r.width() && target.x+box.max.x>r.x()
                    && target.y+box.min.y<r.y()+(long)r.height() && target.y+box.max.y>r.y()
                    && target.z+box.min.z<r.z()+(long)r.depth() && target.z+box.max.z>r.z())return false;
        }
        return true;
    }
    public CompletableFuture<Void> beginSelection(PlayerRef player,World world,UUID activity) {
        return runtime.learn(s->{var c=s.context(player.getUuid());var a=s.activity(player.getUuid(),activity);
            if(c.teacher() || a.published()==null || a.archived())throw new sisso.logicsim.classroom.domain.ClassroomException(sisso.logicsim.classroom.domain.ClassroomException.Reason.FORBIDDEN);
            return new Object[]{c,a};}).thenCompose(data->onWorld(world,()->{
                var ctx=(Context)data[0];var a=(Activity)data[1];
                var session=sessions.begin(player.getUuid(),world.getWorldConfig().getUuid(),player.getPacketHandler(),activity,ctx.membership(),a.published().number(),null,runtime.learningLimits());
                grantTool(player,world,session);player.sendMessage(text("learning.selectionHelp"));return null;
            }));
    }
    public CompletableFuture<Void> beginPaste(PlayerRef player,World world,UUID submission) {
        return ready.thenCompose(ignored->runtime.learn(s->{var c=s.context(player.getUuid());if(!c.teacher())throw new LearningException(AREA_PROTECTED);return s.submission(player.getUuid(),submission);}))
            .thenCompose(s->onWorld(world,()->{
                if(reviews.containsKey(player.getUuid()))throw new LearningException(AREA_PROTECTED);
                var session=sessions.begin(player.getUuid(),world.getWorldConfig().getUuid(),player.getPacketHandler(),s.activity(),s.membership(),s.publication(),submission,runtime.learningLimits());
                grantTool(player,world,session);player.sendMessage(text("learning.pasteHelp"));return null;
            }));
    }
    @SuppressWarnings("removal") // Inventory bridge is the supported Player inventory in the pinned 0.7.4 SDK.
    private static CombinedItemContainer inventory(Player player) {
        var inv=player.getInventory();return new CombinedItemContainer(inv.getHotbar(),inv.getStorage(),inv.getBackpack(),inv.getTools());
    }
    private void grantTool(PlayerRef player,World world,Session session) {
        var entity=entity(player,world);var inventory=inventory(entity.getStore().getComponent(entity,Player.getComponentType()));
        removeTools(player,world);
        var stack=new ItemStack(TOOL,1).withMetadata("ClassroomToken",new BsonString(session.token.toString()));
        if(!inventory.addItemStack(stack).succeeded()){sessions.cancel(player.getUuid());throw new LearningException(INVENTORY_FULL);}
    }
    public void removedFromWorld(UUID actor,Player player) {
        disconnected(actor);if(player!=null)removeTools(player);
    }
    private void removeTools(PlayerRef player,World world) {
        var ref=entity(player,world);removeTools(ref.getStore().getComponent(ref,Player.getComponentType()));
    }
    private static void removeTools(Player player) {
        var inventory=inventory(player);
        for(short i=0;i<inventory.getCapacity();i++){var item=inventory.getItemStack(i);if(item!=null && TOOL.equals(item.getItemId()))inventory.removeItemStackFromSlot(i);}
    }
    public void cancel(PlayerRef player,World world) {
        sessions.cancel(player.getUuid());removeTools(player,world);clearOutline(player);
    }
    public void observe(PlayerRef player,World world) {
        recoverLoaded(world);
        sessions.current(player.getUuid(),world.getWorldConfig().getUuid(),player.getPacketHandler());
        ready.thenRun(()->{
            var r=reviews.get(player.getUuid());
            if(r!=null){subscribe(r);if(r.world().equals(world.getWorldConfig().getUuid()) && recovery.contains(r.id()))discard(player,world);}
        });
    }
    public void disconnected(UUID actor) {
        sessions.cancel(actor);var review=reviews.get(actor);
        if(review!=null){recovery.add(review.id());var world=Universe.get().getWorld(review.world());if(world!=null)recoverLoaded(world);}
    }
    public void recoverLoaded(World world) {
        UUID worldId=world.getWorldConfig().getUuid();
        if(closed || retryWorlds.contains(worldId) || ready.isDone() && recovery.isEmpty() || !recoveryWorlds.add(worldId))return;
        ready.thenRun(()->{
            for(var review:reviews.values()) {
                if(!review.world().equals(world.getWorldConfig().getUuid()) || !recovery.contains(review.id()) || !recovering.add(review.id()))continue;
                queue.submit(review.id(),()->{
                    if(reviews.get(review.teacher())!=review || !recovery.contains(review.id()))return null;
                    await(onWorld(world,()->{
                        if(reviews.get(review.teacher())!=review || !recovery.contains(review.id()))return null;
                        NativeAreaAdapter.clear(world,new Bounds(review.x(),review.y(),review.z(),review.width(),review.height(),review.depth()));return null;
                    }));
                    await(runtime.learn(s->{s.removedReview(review.id());return null;}));
                    reviews.remove(review.teacher(),review);recovery.remove(review.id());invalid.remove(review.teacher());close(authorizations.remove(review.teacher()));return null;
                }).whenComplete((ignored,failure)->{
                    recovering.remove(review.id());
                    var cause=failure;while(cause instanceof CompletionException && cause.getCause()!=null)cause=cause.getCause();
                    if(!closed && cause instanceof LearningException e && e.code()==BUSY && retryWorlds.add(worldId))
                        CompletableFuture.delayedExecutor(1,TimeUnit.SECONDS).execute(()->{
                            retryWorlds.remove(worldId);
                            if(!closed){var loaded=Universe.get().getWorld(worldId);if(loaded!=null)recoverLoaded(loaded);}
                        });
                });
            }
        }).whenComplete((ignored,failure)->recoveryWorlds.remove(worldId));
    }
    public void point(PlayerRef player,World world,ItemStack tool,Point point,boolean second) {
        var session=sessions.require(player.getUuid(),world.getWorldConfig().getUuid(),player.getPacketHandler());
        if(tool.getMetadata()==null || !session.token.toString().equals(tool.getMetadata().getString("ClassroomToken",new BsonString("")).getValue()))throw new LearningException(NO_SELECTION);
        if(session.submission!=null) {
            if(second){cancel(player,world);return;}
            paste(player,world,session,point).whenComplete((r,e)->message(player,e,"learning.pasted"));return;
        }
        if(second)session.second=point;else session.first=point;
        if(session.first!=null && session.second!=null) {
            var b=sessions.bounds(session,runtime.learningLimits());outline(player,b);
            player.sendMessage(text("learning.selectionReady").param("width",b.width()).param("height",b.height()).param("depth",b.depth()));
        } else player.sendMessage(text(second?"learning.secondPoint":"learning.firstPoint"));
    }
    public CompletableFuture<Submission> submit(PlayerRef player,World world) {
        Session session;
        try{session=sessions.require(player.getUuid(),world.getWorldConfig().getUuid(),player.getPacketHandler());if(session.submission!=null)throw new LearningException(NO_SELECTION);}
        catch(RuntimeException e){return CompletableFuture.failedFuture(e);}
        return ready.thenCompose(ignored->queue.submit(player.getUuid(),()->{
            var ctx=await(runtime.learn(s->s.context(player.getUuid())));
            var bounds=sessions.bounds(session,runtime.learningLimits());
            var snapshot=await(onWorld(world,()->{
                entity(player,world);
                if(current(player,world)!=session || reserved(session.world,bounds))throw new LearningException(AREA_PROTECTED);
                return NativeAreaAdapter.capture(world,bounds,runtime.learningLimits());
            }));
            var content=runtime.submissionFiles().write(snapshot);
            var persisted=runtime.learn(s->{
                if(!sessions.active(session))throw new LearningException(NO_SELECTION);
                return s.submit(player.getUuid(),session.activity,session.publication,session.membership,session.token,content,()->sessions.active(session));
            });
            // Resolve file ownership from the actual transaction result, even if shutdown interrupts this worker.
            // On an uncertain shutdown failure retain the file; startup recovery removes only unreferenced files.
            persisted.whenComplete((result,failure)->{
                if(failure==null) {
                    if(!result.content().equals(content.id()))runtime.submissionFiles().discardUncommitted(content.id());
                    sessions.complete(session);runtime.learningChanged(ctx.classroom());
                    onWorld(world,()->{if(player.getReference()!=null && player.getReference().isValid()) {removeTools(player,world);clearOutline(player);}return null;});
                } else if(!closed)runtime.submissionFiles().discardUncommitted(content.id());
            });
            return await(persisted);
        }));
    }
    public CompletableFuture<com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview> preview(UUID actor,UUID submission) {
        return ready.thenCompose(ignored->queue.submit(actor,()->{var s=await(runtime.learn(store->store.submission(actor,submission)));return NativeAreaAdapter.preview(read(s));}));
    }
    public CompletableFuture<SubmissionVerifier.Report> verify(UUID actor,UUID submission) {
        return ready.thenCompose(ignored->queue.submit(actor,()->{
            var ctx=await(runtime.learn(store->store.verification(actor,submission)));
            var result=new SubmissionVerifier().verify(ctx.publication().rules(),NativeAreaAdapter.verificationNodes(read(ctx.submission())));
            // Recheck effective authorization/membership after heavy work, including native APIs without events.
            await(runtime.learn(store->store.verification(actor,submission)));
            return result;
        }));
    }
    private AreaSnapshot read(Submission s) {
        var snapshot=runtime.submissionFiles().read(s.content(),s.checksum());
        if(snapshot.width()!=s.width() || snapshot.height()!=s.height() || snapshot.depth()!=s.depth())throw new LearningException(CORRUPT_CONTENT);
        return snapshot;
    }
    private CompletableFuture<Review> paste(PlayerRef player,World world,Session session,Point point) {
        return ready.thenCompose(ignored->queue.submit(player.getUuid(),()->{
            var s=await(runtime.learn(store->{if(!store.context(player.getUuid()).teacher())throw new LearningException(AREA_PROTECTED);return store.submission(player.getUuid(),session.submission);}));
            var snapshot=read(s);
            var b=new Bounds(point.x(),point.y()+1,point.z(),snapshot.width(),snapshot.height(),snapshot.depth());
            await(onWorld(world,()->{if(current(player,world)!=session)throw new LearningException(NO_SELECTION);NativeAreaAdapter.preflight(world,entity(player,world),snapshot,b);return null;}));
            var r=await(runtime.learn(store->store.reserve(player.getUuid(),s.id(),session.world,b.x(),b.y(),b.z())));
            reviews.put(player.getUuid(),r);invalid.remove(player.getUuid());subscribe(r);
            try {
                await(runtime.learn(store->{if(!store.context(player.getUuid()).teacher())throw new LearningException(AREA_PROTECTED);return store.submission(player.getUuid(),s.id());}));
                await(onWorld(world,()->{
                    var ref=entity(player,world);
                    if(!runtime.teaching(player.getUuid()) || invalid.contains(player.getUuid()) || recovery.contains(r.id()) || current(player,world)!=session)throw new LearningException(AREA_PROTECTED);
                    NativeAreaAdapter.preflight(world,ref,snapshot,b);NativeAreaAdapter.paste(world,snapshot,b);
                    sessions.complete(session);removeTools(player,world);clearOutline(player);return null;
                }));return r;
            } catch(RuntimeException failure) {recovery.add(r.id());throw failure;}
        }));
    }
    public CompletableFuture<Void> discard(PlayerRef player,World world) {
        return ready.thenCompose(ignored->queue.submit(player.getUuid(),()->{
            await(ready);var r=reviews.get(player.getUuid());if(r==null)return null;
            if(!r.world().equals(world.getWorldConfig().getUuid()))throw new LearningException(AREA_UNLOADED);
            await(onWorld(world,()->{
                cleanup.set(true);
                try{NativeAreaAdapter.discard(world,entity(player,world),new Bounds(r.x(),r.y(),r.z(),r.width(),r.height(),r.depth()));}
                finally{cleanup.remove();}return null;
            }));
            await(runtime.learn(s->{s.removedReview(r.id());return null;}));reviews.remove(player.getUuid(),r);recovery.remove(r.id());invalid.remove(player.getUuid());
            close(authorizations.remove(player.getUuid()));return null;
        }));
    }
    private void subscribe(Review r) {
        authorizations.computeIfAbsent(r.teacher(),actor->runtime.listenAuthorization(actor,allowed->{if(!allowed){invalid.add(actor);recovery.add(r.id());cleanupConnected(r);}}));
    }
    private void revalidate(Review r) {
        if(!checking.add(r.teacher()))return;
        runtime.learn(s->{if(!s.context(r.teacher()).teacher())throw new LearningException(AREA_PROTECTED);return s.submission(r.teacher(),r.submission());}).whenComplete((s,e)->{
            checking.remove(r.teacher());if(e!=null){invalid.add(r.teacher());recovery.add(r.id());cleanupConnected(r);}
        });
    }
    private void cleanupConnected(Review r) {
        var player=Universe.get().getPlayer(r.teacher());
        if(player!=null && player.getReference()!=null && player.getReference().isValid()) {
            var world=player.getReference().getStore().getExternalData().getWorld();
            if(world.getWorldConfig().getUuid().equals(r.world()))discard(player,world);
        }
    }
    private static Ref<EntityStore> entity(PlayerRef player,World world) {
        var ref=player.getReference();
        if(ref==null || !ref.isValid() || ref.getStore().getExternalData().getWorld()!=world)throw new LearningException(NO_SELECTION);return ref;
    }
    public <T> CompletableFuture<T> onWorld(World world,Supplier<T> action) {
        var result=new CompletableFuture<T>();
        try{world.execute(()->{if(result.isDone())return;if(closed){result.completeExceptionally(new LearningException(BUSY));return;}try{result.complete(action.get());}catch(Throwable e){result.completeExceptionally(e);}});}
        catch(RuntimeException e){result.completeExceptionally(e);}return result;
    }
    private static <T> T await(CompletableFuture<T> future) {
        try{return future.get();}
        catch(InterruptedException e){Thread.currentThread().interrupt();throw new CompletionException(e);}
        catch(Exception e){throw new CompletionException(e.getCause()==null?e:e.getCause());}
    }
    private static void outline(PlayerRef player,Bounds b) {
        var packet=new EditorBlocksChange();packet.selection=new EditorSelection(b.x(),b.y(),b.z(),b.x()+b.width()-1,b.y()+b.height()-1,b.z()+b.depth()-1);
        player.getPacketHandler().writeNoCache(packet);
    }
    private static void clearOutline(PlayerRef player) { player.getPacketHandler().writeNoCache(new EditorBlocksChange()); }
    private static void message(PlayerRef player,Throwable failure,String success) {
        if(failure==null)player.sendMessage(text(success));else {
            while(failure instanceof CompletionException && failure.getCause()!=null)failure=failure.getCause();
            player.sendMessage(failure instanceof LearningException e?text("learning.error."+e.code().name()).param("problem",e.problem()):text("learning.operationFailed"));
        }
    }
    private static void close(AutoCloseable c){if(c!=null)try{c.close();}catch(Exception ignored){}}
    @Override public void close(){closed=true;sessions.clear();if(queue!=null)queue.close();close(changes);authorizations.values().forEach(AreaRuntime::close);authorizations.clear();}
}
