package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.Anchor;
import com.hypixel.hytale.server.core.ui.Value;
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
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import static sisso.logicsim.util.LogicMessages.text;

/** Native UI adapter. Every mutation goes through server policies and uses the revision presented by this page. */
public class ClassroomPage extends InteractiveCustomUIPage<ClassroomPage.ActionEvent> {
    public static final String LAYOUT = "Pages/LogicGates/Classroom.ui";
    public static final String ROW_LAYOUT = "Pages/LogicGates/ClassroomRow.ui";
    private final ClassroomRuntime runtime;
    private final World world;
    final ClassroomPageModel model = new ClassroomPageModel();
    private boolean busy = true, refreshPending;
    private volatile boolean dismissed;
    private AutoCloseable subscription;
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
        if (subscription == null) {
            subscription = runtime.listen(playerRef.getUuid(), change -> {
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
        var waiting = new UICommandBuilder(); controls(waiting,true); publish(ref,store,waiting,new UIEventBuilder());
        runtime.screen(playerRef.getUuid(),model.kind,model.query,model.page).whenComplete((screen,failure) -> {
            try { world.execute(() -> {
                if (!active(ref,store)) return;
                if (failure != null) { failed(ref,store,failure); return; }
                busy = false;
                boolean fields = model.apply(screen,reset);
                var c = new UICommandBuilder(); var e = new UIEventBuilder(); render(c,e,fields);
                c.set("#Status.TextSpans",status != null ? status : reset ? text("classroom.ready") : text("classroom.refreshed"));
                publish(ref,store,c,e);
                if (refreshPending) refresh(ref,store,false,null);
            }); } catch (RuntimeException stoppedWorld) { dispose(); }
        });
    }
    @Override public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ActionEvent event) {
        if (!active(ref,store)) return;
        if ("Close".equals(event.action)) { dispose(); close(); return; }
        if (busy || event.action == null) { acknowledge(ref,store); return; }
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
        busy=true; var c=new UICommandBuilder(); controls(c,true); c.set("#Status.TextSpans",text("classroom.saving")); publish(ref,store,c,new UIEventBuilder());
        operation.whenComplete((value,failure) -> {
            try { world.execute(() -> {
                if (!active(ref,store)) return;
                if (failure != null) failed(ref,store,failure);
                else refresh(ref,store,true,text("classroom.done"));
            }); } catch (RuntimeException stoppedWorld) { dispose(); }
        });
    }
    /** Applies only changed regions; field values are sent solely for explicit reload/navigation/selection. */
    void render(UICommandBuilder c,UIEventBuilder e,boolean resetFields) {
        var home=model.screen.home(); var classroom=home.classroom(); boolean teacher=model.teacher();
        boolean linked=home.member()!=null, settings=model.kind==Kind.SETTINGS;
        c.set("#Navigation.Visible",true); c.set("#Details.Visible",true); c.set("#NoAccess.Visible",false);
        c.set("#NavClass.Text",teacher ? text("classroom.nav.class") : linked ? text("classroom.nav.myClass") : text("classroom.nav.browse"));
        c.set("#NavIntakes.Visible",!linked || teacher); c.set("#NavInvite.Visible",teacher);
        c.set("#NavSettings.Visible",teacher || !linked && home.canTeach());
        c.set("#NavSettings.Text",linked ? text("classroom.nav.settings") : text("classroom.create"));
        c.set("#HeaderTitle.TextSpans",classroom==null ? text("classroom.title") : Message.raw(classroom.name()));
        height(c,"#HeaderTitle",classroom==null ? 34 : Math.max(34,((classroom.name().codePointCount(0,classroom.name().length())+29)/30)*28),0);
        c.set("#HeaderInfo.TextSpans",classroom==null ? text("classroom.oneClass") : teacher
                ? text("classroom.classInfo").param("teacher",home.teacherName()).param("count",model.screen.studentCount())
                : text("classroom.studentInfo").param("teacher",home.teacherName()).param("enrollment",home.member().enrollment()));
        boolean list=model.kind!=Kind.SETTINGS && model.kind!=Kind.DETAIL;
        c.set("#ListPanel.Visible",list); c.set("#SearchBar.Visible",model.kind==Kind.BROWSE || model.kind==Kind.PLAYERS);
        c.set("#Empty.Visible",model.screen.rows().items().isEmpty());
        c.set("#Page.TextSpans",text("classroom.page").param("page",model.page+1));
        c.clear("#Rows");
        int n=0;
        for (Object row:model.screen.rows().items()) {
            c.append("#Rows",ROW_LAYOUT); String selector="#Rows["+n+++"]";
            String name=switch(row) { case Summary s->s.name(); case Profile p->p.name(); case Member m->m.name(); case Intake i->teacher?i.studentName():i.classroomName(); default->""; };
            Message info=switch(row) { case Summary s->Message.raw(s.teacherName()); case Profile p->Message.raw(p.id().toString()); case Member m->Message.raw(m.enrollment()); case Intake i->stateText(i.state()); default->Message.raw(""); };
            c.set(selector+" #RowName.TextSpans",Message.raw(name)); c.set(selector+" #RowInfo.TextSpans",info);
            e.addEventBinding(CustomUIEventBindingType.Activating,selector,EventData.of("Action","Select").append("Target",ClassroomPageModel.id(row).toString()));
        }
        c.set("#Form.Visible",settings); c.set("#MessageEditor.Visible",settings && classroom!=null);
        c.set("#Actions.Visible",!settings);
        c.set("#Save.Text",classroom==null ? text("classroom.create") : text("classroom.save"));
        c.set("#DetailTitle.Visible",!settings); c.set("#DetailMeta.Visible",!settings); c.set("#DescriptionText.Visible",!settings);
        c.set("#MessageDisplay.Visible",!settings && classroom!=null && !classroom.message().isEmpty());
        if (classroom!=null) literal(c,"#MessageText",classroom.message());
        c.set("#EnrollmentForm.Visible",false);
        for (String action:new String[]{"Primary","Secondary","Danger","CancelConfirm"}) c.set("#"+action+".Visible",false);
        String enrollment="";
        if (!settings) {
            Message title=classroom==null ? text("classroom.selectHint") : Message.raw(classroom.name());
            Message meta=classroom==null ? text("classroom.ingressHint") : Message.raw(home.teacherName());
            String description=classroom==null ? "" : classroom.description();
            switch (model.selected) {
                case Summary s -> { title=Message.raw(s.name()); meta=Message.raw(s.teacherName()); description=s.description(); action(c,"Primary",text("classroom.request")); c.set("#EnrollmentForm.Visible",true); }
                case Profile p -> { title=Message.raw(p.name()); meta=Message.raw(p.id().toString()); description=""; action(c,"Primary",text("classroom.invite")); }
                case Member m -> {
                    title=Message.raw(m.name()); meta=memberText(m); description=""; enrollment=m.enrollment(); c.set("#EnrollmentForm.Visible",true);
                    action(c,"Primary",text("classroom.correct")); action(c,"Danger",model.confirming?text("classroom.confirmRemove"):text("classroom.remove"));
                }
                case Intake i -> {
                    title=Message.raw(teacher?i.studentName():i.classroomName());
                    meta=text("classroom.intakeInfo").param("state",stateText(i.state())).param("enrollment",i.enrollment()==null?"":i.enrollment()).param("uuid",i.studentId().toString());
                    description=""; enrollment=i.enrollment()==null?"":i.enrollment();
                    if (teacher && i.state()==State.REQUESTED) { action(c,"Primary",text("classroom.approve")); c.set("#EnrollmentForm.Visible",true); }
                    else if (!teacher && i.state()==State.INVITED) { action(c,"Primary",text("classroom.accept")); c.set("#EnrollmentForm.Visible",true); }
                    if (i.state()==State.INVITED || i.state()==State.REQUESTED) action(c,"Secondary",teacher && i.state()==State.REQUESTED ? text("classroom.reject") : !teacher && i.state()==State.INVITED ? text("classroom.decline") : text("classroom.cancel"));
                }
                case null, default -> { }
            }
            if (model.kind==Kind.DETAIL) { meta=memberText(home.member()); action(c,"Danger",model.confirming?text("classroom.confirmLeave"):text("classroom.leave")); }
            c.set("#DetailTitle.TextSpans",title); c.set("#DetailMeta.TextSpans",meta); literal(c,"#DescriptionText",description);
            String rawTitle=title.getRawText();
            height(c,"#DetailTitle",rawTitle==null ? 64 : Math.max(40,((rawTitle.codePointCount(0,rawTitle.length())+19)/20)*28),8);
            c.set("#CancelConfirm.Visible",model.confirming);
        }
        if (resetFields) {
            c.set("#Name.Value",classroom==null?"":classroom.name()); c.set("#Description.Value",classroom==null?"":classroom.description());
            c.set("#Message.Value",classroom==null?"":classroom.message()); c.set("#Enrollment.Value",enrollment); c.set("#Search.Value",model.query);
        }
        controls(c,false);
        c.set("#Previous.Disabled",model.page==0); c.set("#Next.Disabled",!model.screen.rows().hasNext());
    }
    private static Message memberText(Member m) {
        return text("classroom.memberInfo").param("name",m.name()).param("enrollment",m.enrollment()).param("uuid",m.playerId().toString())
                .param("date",DateTimeFormatter.ISO_LOCAL_DATE.withZone(ZoneOffset.UTC).format(Instant.ofEpochMilli(m.joinedAt())));
    }
    private static void literal(UICommandBuilder c,String selector,String content) {
        c.set(selector+".TextSpans",Message.raw(content));
        int lines=content.lines().mapToInt(line->Math.max(1,(line.codePointCount(0,line.length())+29)/30)).sum();
        height(c,selector,Math.max(40,lines*24+12),12);
    }
    private static void height(UICommandBuilder c,String selector,int height,int bottom) {
        // Hytale 0.7.4 resolves Anchor as a whole markup property, not Anchor.Height.
        var anchor=new Anchor();
        anchor.setHeight(Value.of(height));
        anchor.setBottom(Value.of(bottom));
        c.setObject(selector+".Anchor",anchor);
    }
    private static void action(UICommandBuilder c,String selector,Message label) { c.set("#"+selector+".Visible",true); c.set("#"+selector+".Text",label); }
    private static void controls(UICommandBuilder c,boolean disabled) { for(String control:OPERATIONS) c.set(control+".Disabled",disabled); }
    private static void bind(UIEventBuilder e,String selector,String action) { e.addEventBinding(CustomUIEventBindingType.Activating,selector,EventData.of("Action",action)); }
    private static Message stateText(State state) {
        return switch(state) {
            case INVITED->text("classroom.state.invited"); case REQUESTED->text("classroom.state.requested");
            case APPROVED->text("classroom.state.approved"); case REJECTED->text("classroom.state.rejected");
            case DECLINED->text("classroom.state.declined"); case CANCELLED->text("classroom.state.cancelled"); case SUPERSEDED->text("classroom.state.superseded");
        };
    }
    private void failed(Ref<EntityStore> ref,Store<EntityStore> store,Throwable failure) {
        busy=false; var c=new UICommandBuilder(); controls(c,false); c.set("#Status.TextSpans",error(failure));
        if (unwrap(failure) instanceof ClassroomException ex && ex.reason()==ClassroomException.Reason.FORBIDDEN) {
            model.screen=null; model.selected=null; c.clear("#Rows");
            c.set("#Navigation.Visible",false); c.set("#ListPanel.Visible",false); c.set("#Details.Visible",true);
            for(String panel:new String[]{"Form","EnrollmentForm","Actions","MessageDisplay","DetailTitle","DetailMeta","DescriptionText"}) c.set("#"+panel+".Visible",false);
            c.set("#NoAccess.Visible",true); c.set("#HeaderTitle.TextSpans",text("classroom.title")); c.set("#HeaderInfo.TextSpans",text("classroom.oneClass"));
        }
        publish(ref,store,c,new UIEventBuilder());
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
        store.getComponent(ref,Player.getComponentType()).getPageManager().updateLegacyCustomPage(new CustomPage(getClass().getName(),false,false,getLifetime(),c.getCommands(),e.getEvents()));
    }
    private void dispose() {
        dismissed=true;
        if(subscription!=null) { try { subscription.close(); } catch(Exception ignored) { } subscription=null; }
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
