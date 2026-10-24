package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.util.BuildInfo;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import static sisso.logicsim.util.LogicMessages.text;

/** Native UI adapter. Every mutation goes through server policies and uses the revision presented by this page. */
public class ClassroomPage extends InteractiveCustomUIPage<ClassroomPage.ActionEvent> {
    public static final String LAYOUT = "Pages/LogicGates/Classroom.ui";
    public static final String ROW_LAYOUT = "Pages/LogicGates/ClassroomRow.ui";
    private final ClassroomRuntime runtime;
    private final World world;
    final ClassroomPageModel model = new ClassroomPageModel();
    final ClassroomPageAccess access = new ClassroomPageAccess();
    private ClassroomPageAccess.Stamp validatedAccess = access.current();
    private volatile boolean busy = true;
    private boolean refreshPending;
    private volatile java.util.function.Predicate<ClassroomRuntime.Change> notificationInterest=change->true;
    private final ClassroomPageUpdates updates=new ClassroomPageUpdates();
    private volatile boolean dismissed;
    private AutoCloseable subscription, authorizationSubscription;
    private static final String[] OPERATIONS = {"#Save","#Primary","#Secondary","#Danger","#CancelConfirm","#Previous","#Next","#Find","#NavClass","#NavIntakes","#NavInvite","#NavSettings"};

    public ClassroomPage(PlayerRef playerRef, ClassroomRuntime runtime, World world) {
        super(playerRef, CustomPageLifetime.CanDismiss, ActionEvent.CODEC);
        this.runtime = runtime; this.world = world;
    }
    @Override public void build(Ref<EntityStore> ref, UICommandBuilder c, UIEventBuilder e, Store<EntityStore> store) {
        c.append(LAYOUT);
        c.set("#Build.TextSpans", text("build").param("build", BuildInfo.BUILD_ID).param("hytale", BuildInfo.HYTALE_VERSION));
        for (String action : new String[]{"Close","Reload","NavClass","NavIntakes","NavInvite","NavSettings","Previous","Next","Secondary","Danger","CancelConfirm"}) bind(e,"#"+action,action);
        e.addEventBinding(CustomUIEventBindingType.Activating,"#Save",EventData.of("Action","Save")
                .append("@Name","#Name.Value").append("@Description","#Description.Value").append("@Message","#Message.Value"));
        e.addEventBinding(CustomUIEventBindingType.Activating,"#Primary",EventData.of("Action","Primary").append("@Enrollment","#Enrollment.Value"));
        e.addEventBinding(CustomUIEventBindingType.Activating,"#Find",EventData.of("Action","Find").append("@Query","#Search.Value"));
        controls(c,true);
    }
    public void load(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (authorizationSubscription == null) {
            authorizationSubscription = runtime.listenAuthorization(playerRef.getUuid(), allowed -> {
                access.changed(allowed); // Invalidates old callbacks before scheduling any world work.
                try { world.execute(() -> {
                    if (!active(ref,store)) { dispose(); return; }
                    var c = new UICommandBuilder(); discardRevokedDraft(c); controls(c,true);
                    publish(ref,store,c,new UIEventBuilder());
                    if (busy) refreshPending = true; else refresh(ref,store,false,null);
                }); } catch (RuntimeException stoppedWorld) { dispose(); }
            });
        }
        if (subscription == null) {
            subscription = runtime.listen(playerRef.getUuid(),change -> busy || notificationInterest.test(change), change -> {
                try { world.execute(() -> {
                    if (!active(ref,store)) { dispose(); return; }
                    if (!model.interested(change,playerRef.getUuid())) return;
                    if (busy) refreshPending = true; else refresh(ref,store,false,null);
                }); } catch (RuntimeException stoppedWorld) { dispose(); }
            });
        }
        busy = true;
        runtime.remember(playerRef.getUuid(),playerRef.getUsername()).whenComplete((ignored,failure) -> {
            try { world.execute(() -> {
                if (!active(ref,store)) return;
                if (failure != null) failed(ref,store,failure);
                else refresh(ref,store,true,null);
            }); } catch (RuntimeException stoppedWorld) { dispose(); }
        });
    }
    private void refresh(Ref<EntityStore> ref, Store<EntityStore> store, boolean reset, Message status) {
        busy = true; refreshPending = false;
        var waiting = new UICommandBuilder(); discardRevokedDraft(waiting); controls(waiting,true); publish(ref,store,waiting,new UIEventBuilder());
        notificationInterest=model.interest(playerRef.getUuid());
        var stamp = access.current();
        runtime.screen(playerRef.getUuid(),model.kind,model.query,model.page).whenComplete((screen,failure) -> {
            try { world.execute(() -> {
                if (!active(ref,store)) return;
                if (!access.current(stamp)) { busy=false; refresh(ref,store,false,null); return; }
                if (failure != null) {
                    validatedAccess = stamp;
                    failed(ref,store,failure);
                    if (refreshPending) refresh(ref,store,false,null);
                    return;
                }
                var c = new UICommandBuilder(); var e = new UIEventBuilder();
                if (!renderResponse(stamp,screen,reset,status,c,e)) { refresh(ref,store,false,null); return; }
                busy = false;
                validatedAccess = stamp;
                publish(ref,store,c,e);
                if (refreshPending) refresh(ref,store,false,null);
            }); } catch (RuntimeException stoppedWorld) { dispose(); }
        });
    }
    @Override public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ActionEvent event) {
        if (!active(ref,store)) return;
        if ("Close".equals(event.action)) { dispose(); close(); return; }
        if (busy || !access.current(validatedAccess) || event.action == null) { acknowledge(ref,store); return; }
        if (model.screen == null && !"Reload".equals(event.action)) { acknowledge(ref,store); return; }
        switch (event.action) {
            case "Reload" -> refresh(ref,store,true,null);
            case "NavClass" -> navigate(ref,store,model.teacher() ? Kind.MEMBERS : Kind.BROWSE);
            case "NavIntakes" -> navigate(ref,store,Kind.INTAKES);
            case "NavInvite" -> navigate(ref,store,Kind.PLAYERS);
            case "NavSettings" -> navigate(ref,store,Kind.SETTINGS);
            case "Previous", "Next" -> {
                if ("Next".equals(event.action) && !model.screen.rows().hasNext() || "Previous".equals(event.action) && model.page==0) { acknowledge(ref,store); return; }
                model.page += "Next".equals(event.action) ? 1 : -1; model.selected=null; refresh(ref,store,true,null);
            }
            case "Find" -> { model.query = event.query == null ? "" : event.query; model.page=0; model.selected=null; refresh(ref,store,true,null); }
            case "Select" -> {
                if (!model.select(event.target)) { acknowledge(ref,store); return; }
                var c=new UICommandBuilder(); var e=new UIEventBuilder(); render(c,e,true); publish(ref,store,c,e);
            }
            case "Save" -> {
                if (model.kind != Kind.SETTINGS) { acknowledge(ref,store); return; }
                var current=model.screen.home().classroom();
                run(ref,store,current == null ? runtime.create(playerRef.getUuid(),event.name,event.description)
                        : runtime.settings(playerRef.getUuid(),current.id(),model.editRevision,event.name,event.description,event.message));
            }
            case "Primary" -> primary(ref,store,event.enrollment);
            case "Secondary" -> {
                if (model.selected instanceof Intake i) run(ref,store,runtime.closeIntake(playerRef.getUuid(),i.id(),i.revision(),model.teacher()));
                else acknowledge(ref,store);
            }
            case "Danger" -> danger(ref,store);
            case "CancelConfirm" -> { model.confirming=false; var c=new UICommandBuilder(); var e=new UIEventBuilder(); render(c,e,false); publish(ref,store,c,e); }
            default -> acknowledge(ref,store);
        }
    }
    private void navigate(Ref<EntityStore> ref,Store<EntityStore> store,Kind kind) {
        model.navigate(kind); refresh(ref,store,true,null);
    }
    private void primary(Ref<EntityStore> ref,Store<EntityStore> store,String enrollment) {
        switch (model.selected) {
            case Summary s -> run(ref,store,runtime.request(playerRef.getUuid(),s.id(),enrollment));
            case Profile p -> run(ref,store,runtime.invite(playerRef.getUuid(),p.id()));
            case Intake i -> {
                if (model.teacher() && i.state()==State.REQUESTED) run(ref,store,runtime.approve(playerRef.getUuid(),i.id(),i.revision(),enrollment));
                else if (!model.teacher() && i.state()==State.INVITED) run(ref,store,runtime.accept(playerRef.getUuid(),i.id(),i.revision(),enrollment));
                else acknowledge(ref,store);
            }
            case Member m when model.teacher() -> run(ref,store,runtime.correct(playerRef.getUuid(),m.id(),m.revision(),enrollment));
            case null, default -> acknowledge(ref,store);
        }
    }
    private void danger(Ref<EntityStore> ref,Store<EntityStore> store) {
        Member target = model.teacher() && model.selected instanceof Member m ? m : model.kind==Kind.DETAIL ? model.screen.home().member() : null;
        if (target == null || target.role()!=Role.STUDENT) { acknowledge(ref,store); return; }
        if (model.confirming) run(ref,store,runtime.end(playerRef.getUuid(),target.id(),target.revision(),model.teacher()));
        else { model.confirming=true; var c=new UICommandBuilder(); var e=new UIEventBuilder(); render(c,e,false); c.set("#Status.TextSpans",text("classroom.confirmHint")); publish(ref,store,c,e); }
    }
    private void run(Ref<EntityStore> ref,Store<EntityStore> store,CompletableFuture<?> operation) {
        var stamp = access.current();
        busy=true; var c=new UICommandBuilder(); controls(c,true); c.set("#Status.TextSpans",text("classroom.saving")); publish(ref,store,c,new UIEventBuilder());
        operation.whenComplete((value,failure) -> {
            try { world.execute(() -> {
                if (!active(ref,store)) return;
                if (!access.current(stamp)) { busy=false; refresh(ref,store,false,null); return; }
                if (failure != null) failed(ref,store,failure);
                else refresh(ref,store,true,text("classroom.done"));
                if (failure != null && refreshPending) refresh(ref,store,false,null);
            }); } catch (RuntimeException stoppedWorld) { dispose(); }
        });
    }
    /** Rendering is independent from the session/authorization callbacks. */
    boolean renderResponse(ClassroomPageAccess.Stamp stamp,Screen screen,boolean reset,Message status,UICommandBuilder c,UIEventBuilder e) {
        if (!access.current(stamp)) return false;
        // A query can finish before the queued permission handler runs. Consume revocation here too.
        discardRevokedDraft(c);
        boolean fields=model.apply(screen,reset);
        if(playerRef!=null) notificationInterest=model.interest(playerRef.getUuid());
        render(c,e,fields);
        c.set("#Status.TextSpans",status!=null ? status : ClassroomScreenRenderer.status(model,!fields));
        return access.current(stamp);
    }
    void render(UICommandBuilder c, UIEventBuilder e, boolean resetFields) {
        controls(c,false);
        ClassroomScreenRenderer.render(model,c,e,resetFields);
    }
    private static void controls(UICommandBuilder c,boolean disabled) { for(String control:OPERATIONS) c.set(control+".Disabled",disabled); }
    private static void bind(UIEventBuilder e,String selector,String action) { e.addEventBinding(CustomUIEventBindingType.Activating,selector,EventData.of("Action",action)); }
    private void failed(Ref<EntityStore> ref,Store<EntityStore> store,Throwable failure) {
        busy=false; var c=new UICommandBuilder(); controls(c,false); c.set("#Status.TextSpans",error(failure));
        if (unwrap(failure) instanceof ClassroomException ex && ex.reason()==ClassroomException.Reason.FORBIDDEN) {
            model.revoked(); ClassroomScreenRenderer.denied(c);
        }
        if (model.screen!=null) {
            c.set("#Previous.Disabled",model.page==0);
            c.set("#Next.Disabled",!model.screen.rows().hasNext());
        }
        publish(ref,store,c,new UIEventBuilder());
    }
    private void discardRevokedDraft(UICommandBuilder c) {
        if (access.consumeRevocation() && (model.teacher() || model.kind==Kind.SETTINGS)) {
            model.revoked(); ClassroomScreenRenderer.denied(c);
        }
    }
    static Message error(Throwable failure) {
        Throwable cause=unwrap(failure);
        if (!(cause instanceof ClassroomException ex)) return text("classroom.error.unavailable");
        return switch(ex.reason()) {
            case FORBIDDEN->text("classroom.error.forbidden"); case ALREADY_MEMBER->text("classroom.error.alreadyMember");
            case INVALID_NAME->text("classroom.error.name"); case INVALID_DESCRIPTION->text("classroom.error.description");
            case INVALID_ENROLLMENT->text("classroom.error.enrollment"); case DUPLICATE_ENROLLMENT->text("classroom.error.duplicateEnrollment");
            case INVALID_MESSAGE->text("classroom.error.message"); case NOT_FOUND->text("classroom.error.notFound"); case CLOSED->text("classroom.error.closed");
            case STALE->text("classroom.error.stale"); case UNAVAILABLE->text("classroom.error.unavailable");
        };
    }
    private static Throwable unwrap(Throwable failure) { while(failure instanceof CompletionException && failure.getCause()!=null) failure=failure.getCause(); return failure; }
    private boolean active(Ref<EntityStore> ref,Store<EntityStore> store) {
        if (dismissed || !ref.isValid() || !ref.equals(playerRef.getReference()) || ref.getStore()!=store || store.getExternalData().getWorld()!=world) return false;
        var player=store.getComponent(ref,Player.getComponentType()); return player!=null && player.getPageManager().getCustomPage()==this;
    }
    private void acknowledge(Ref<EntityStore> ref,Store<EntityStore> store) { publish(ref,store,new UICommandBuilder(),new UIEventBuilder()); }
    private void publish(Ref<EntityStore> ref,Store<EntityStore> store,UICommandBuilder c,UIEventBuilder e) {
        if (!active(ref,store)) return;
        store.getComponent(ref,Player.getComponentType()).getPageManager().updateLegacyCustomPage(
                updates.packet(getClass().getName(),getLifetime(),c,e,model.screen==null ? null : model.screen.rows().items()));
    }
    private void dispose() {
        dismissed=true;
        if(subscription!=null) { try { subscription.close(); } catch(Exception ignored) { } subscription=null; }
        if(authorizationSubscription!=null) { try { authorizationSubscription.close(); } catch(Exception ignored) { } authorizationSubscription=null; }
    }
    @Override public void onDismiss(Ref<EntityStore> ref,Store<EntityStore> store) { dispose(); }
    public static final class ActionEvent {
        public static final BuilderCodec<ActionEvent> CODEC=BuilderCodec.builder(ActionEvent.class,ActionEvent::new)
            .append(new KeyedCodec<>("Action",Codec.STRING),(e,v)->e.action=v,e->e.action).add()
            .append(new KeyedCodec<>("Target",Codec.STRING),(e,v)->e.target=v,e->e.target).add()
            .append(new KeyedCodec<>("@Name",Codec.STRING),(e,v)->e.name=v,e->e.name).add()
            .append(new KeyedCodec<>("@Description",Codec.STRING),(e,v)->e.description=v,e->e.description).add()
            .append(new KeyedCodec<>("@Message",Codec.STRING),(e,v)->e.message=v,e->e.message).add()
            .append(new KeyedCodec<>("@Enrollment",Codec.STRING),(e,v)->e.enrollment=v,e->e.enrollment).add()
            .append(new KeyedCodec<>("@Query",Codec.STRING),(e,v)->e.query=v,e->e.query).add().build();
        String action,target,name,description,message,enrollment,query;
    }
}
