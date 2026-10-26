package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.*;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.protocol.packets.interface_.*;
import com.hypixel.hytale.protocol.packets.buildertools.BuilderToolPrefabPreview;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.*;
import com.hypixel.hytale.server.core.ui.builder.*;
import com.hypixel.hytale.server.core.universe.*;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.LogicSimPlugin;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.domain.ClassroomViews.Page;
import sisso.logicsim.classroom.learning.*;
import sisso.logicsim.classroom.learning.LearningViews.*;
import sisso.logicsim.util.BuildInfo;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import static sisso.logicsim.util.LogicMessages.text;

/** Role-scoped metadata page. Native 3D data is fetched only for the selected immutable revision. */
public final class LearningPage extends InteractiveCustomUIPage<LearningPage.Event> {
    public static final String LAYOUT="Pages/LogicGates/Learning.ui";
    private final ClassroomRuntime runtime;
    private final AreaRuntime areas;
    private final World world;
    private final ClassroomPageAccess access=new ClassroomPageAccess();
    private final ClassroomPageRefresh refreshGate=new ClassroomPageRefresh();
    private final ClassroomPageUpdates updates=new ClassroomPageUpdates();
    private ClassroomPageAccess.Stamp validated=access.current();
    private AutoCloseable dataSubscription,authSubscription,learningSubscription;
    private volatile Context context;
    private Activity activity;
    private UUID activityId,studentId,submissionId,previewId,previewLoading;
    private Submission submission;
    private Publication submittedVersion;
    private Page<?> rows=new Page<>(List.of(),0,false);
    private Page<Submission> revisions=new Page<>(List.of(),0,false);
    private LearningStore.Filter filter=LearningStore.Filter.ALL;
    private boolean students,busy=true,dismissed,pending,saving,discardDrafts;
    private int page,revisionPage,gradeRevision;
    private long editRevision;
    private static final String[] ACTIONS={"New","Filter","Activities","Previous","Next","SaveDraft","Publish","Archive","Restore","Students","Deliver","SubmitArea","CancelSelection","Newer","Older","SaveGrade","Paste","Discard"};
    private static final String[] FIELDS={"TitleField","DescriptionField","DeadlineField","ValueField","ScoreField","FeedbackField"};
    public LearningPage(PlayerRef player,ClassroomRuntime runtime,World world,UUID activity,UUID submission) {
        super(player,CustomPageLifetime.CanDismiss,Event.CODEC);this.runtime=runtime;this.world=world;this.areas=LogicSimPlugin.getAreas();
        activityId=activity;submissionId=submission;
    }
    public static void open(Ref<EntityStore> ref,Store<EntityStore> store,PlayerRef player,ClassroomRuntime runtime,World world,UUID activity,UUID submission) {
        var page=new LearningPage(player,runtime,world,activity,submission);
        store.getComponent(ref,Player.getComponentType()).getPageManager().openCustomPage(ref,store,page);page.load(ref,store);
    }
    @Override public void build(Ref<EntityStore> ref,UICommandBuilder c,UIEventBuilder e,Store<EntityStore> store) {
        c.append(LAYOUT);c.set("#Build.TextSpans",text("build").param("build",BuildInfo.BUILD_ID).param("hytale",BuildInfo.HYTALE_VERSION));
        for(String action:new String[]{"Back","Close","Reload","New","Filter","Activities","Previous","Next","Archive","Restore","Students","Deliver","SubmitArea","CancelSelection","Newer","Older","Paste","Discard"})
            e.addEventBinding(CustomUIEventBindingType.Activating,"#"+action,EventData.of("Action",action));
        for(String action:new String[]{"SaveDraft","Publish"})e.addEventBinding(CustomUIEventBindingType.Activating,"#"+action,EventData.of("Action",action)
                .append("@Title","#TitleField.Value").append("@Description","#DescriptionField.Value").append("@Deadline","#DeadlineField.Value").append("@Value","#ValueField.Value"));
        e.addEventBinding(CustomUIEventBindingType.Activating,"#SaveGrade",EventData.of("Action","SaveGrade").append("@Score","#ScoreField.Value").append("@Feedback","#FeedbackField.Value"));
        controls(c,true);
    }
    private void load(Ref<EntityStore> ref,Store<EntityStore> store) {
        areas.observe(playerRef,world);
        authSubscription=runtime.listenAuthorization(playerRef.getUuid(),allowed->{
            access.changed(allowed);
            schedule(ref,store,()->{
                if(!allowed && (context==null || context.teacher())){
                    discardDrafts=true;
                    var c=new UICommandBuilder();for(String f:FIELDS)c.set("#"+f+".Value","");c.set("#Details.Visible",false);publish(ref,store,c,new UIEventBuilder());clearPreview();
                }
                if(busy)pending=true;else refresh(ref,store,false,null);
            });
        });
        dataSubscription=runtime.listen(playerRef.getUuid(),change->context==null || context.classroom().equals(change.classroom()) || playerRef.getUuid().equals(change.player()),change->
            schedule(ref,store,()->{if(busy)pending=true;else refresh(ref,store,false,null);}));
        learningSubscription=runtime.listenLearning(playerRef.getUuid(),classroom->{
            var ctx=context;if(ctx==null || ctx.classroom().equals(classroom))schedule(ref,store,()->{if(busy)pending=true;else refresh(ref,store,false,null);});
        });
        refresh(ref,store,true,null);
    }
    private record Data(Context context,Page<?> rows,Activity activity,UUID student,Page<Submission> revisions,Submission selected,Publication version) { }
    private void refresh(Ref<EntityStore> ref,Store<EntityStore> store,boolean reset,Message status) {
        busy=true;pending=false;var stamp=access.current();var waiting=new UICommandBuilder();controls(waiting,true);publish(ref,store,waiting,new UIEventBuilder());
        boolean resetFields=reset || discardDrafts;
        UUID a=activityId,who=studentId,selected=submissionId;boolean showStudents=students;int number=page,history=revisionPage;var selectedFilter=filter;
        runtime.learn(s->{
            var ctx=s.context(playerRef.getUuid());var list=showStudents && ctx.teacher() && a!=null?s.students(playerRef.getUuid(),a,number):s.activities(playerRef.getUuid(),selectedFilter,number);
            Activity activity=a==null?null:s.activity(playerRef.getUuid(),a);
            UUID target=ctx.teacher()?who:playerRef.getUuid();
            Submission chosen=selected==null?null:s.submission(playerRef.getUuid(),selected);
            if(chosen!=null && a!=null && !chosen.activity().equals(a))chosen=null;
            if(chosen!=null && ctx.teacher())target=chosen.student();
            var revisions=a!=null && target!=null?s.submissions(playerRef.getUuid(),a,target,history):new Page<Submission>(List.of(),history,false);
            if(chosen==null && !revisions.items().isEmpty())chosen=revisions.items().getFirst();
            return new Data(ctx,list,activity,target,revisions,chosen,chosen==null?null:s.publication(playerRef.getUuid(),chosen.activity(),chosen.publication()));
        }).whenComplete((data,failure)->inWorld(ref,store,()->{
            if(!access.current(stamp)){busy=false;refresh(ref,store,false,null);return;}
            busy=false;validated=stamp;
            if(failure!=null){fail(ref,store,failure);return;}
            context=data.context;rows=data.rows;activity=data.activity;studentId=data.student;revisions=data.revisions;submission=data.selected;submittedVersion=data.version;discardDrafts=false;
            submissionId=submission==null?null:submission.id();
            var c=new UICommandBuilder();var e=new UIEventBuilder();render(c,e,resetFields);
            c.set("#Status.TextSpans",status!=null?status:guidance());publish(ref,store,c,e);
            fetchPreview(ref,store,stamp);
            if(pending)refresh(ref,store,false,null);
        }));
    }
    private void render(UICommandBuilder c,UIEventBuilder e,boolean reset) {
        boolean teacher=context.teacher(),editing=teacher && !students && activity!=null;
        c.set("#Body.Visible",true);c.set("#Details.Visible",true);controls(c,false);
        c.set("#Heading.TextSpans",Message.raw(context.name()));height(c,"#Heading",ClassroomScreenRenderer.textHeight(context.name(),900,20,26,32));c.set("#New.Visible",teacher);c.set("#Filter.Visible",teacher && !students);
        c.set("#Filter.TextSpans",text("learning.filter."+filter.name()));
        c.set("#Empty.Visible",rows.items().isEmpty());c.set("#Empty.TextSpans",text(students?"learning.noStudents":"learning.noActivities"));
        c.set("#Page.TextSpans",text("classroom.page").param("page",page+1));c.set("#Previous.Disabled",page==0);c.set("#Next.Disabled",!rows.hasNext());
        c.clear("#Rows");
        int i=0;for(Object row:rows.items()) {
            c.append("#Rows",ClassroomPage.ROW_LAYOUT);String selector="#Rows["+(i++)+"]";
            UUID id;String title;Message info;boolean selected;
            if(row instanceof Activity a){id=a.id();title=a.title().isBlank()?null:shortText(a.title());info=text(a.archived()?"learning.archived":a.draft()!=null?"learning.draft":"learning.published");selected=id.equals(activityId);}
            else{var s=(Student)row;id=s.id();title=shortText(s.name());info=s.latest()==null?text("learning.notSubmitted"):submissionLabel(s.latest());selected=id.equals(studentId);}
            c.set(selector+" #RowName.TextSpans",title==null?text("learning.untitled"):Message.raw(title));c.set(selector+" #RowInfo.TextSpans",info);
            c.set(selector+" #SelectionMark.Visible",selected);c.set(selector+" #RowStamp.Visible",row instanceof Student);if(row instanceof Student student)c.set(selector+" #RowStamp.TextSpans",Message.raw(shortText(student.enrollment(),20)));
            e.addEventBinding(CustomUIEventBindingType.Activating,selector,EventData.of("Action","Select").append("Target",id.toString()));
        }
        c.set("#Editor.Visible",editing);c.set("#Read.Visible",activity!=null && !editing);
        c.set("#ActivityActions.Visible",editing && !activity.archived());c.set("#Restore.Visible",editing && activity.archived());
        c.set("#Students.Visible",teacher && activity!=null && !students);
        c.set("#Deliver.Visible",!teacher && activity!=null);
        var selection=areas.current(playerRef,world);
        c.set("#SubmitArea.Visible",!teacher && selection!=null && selection.submission==null && selection.activity.equals(activityId));
        c.set("#CancelSelection.Visible",selection!=null);
        c.set("#Discard.Visible",teacher && areas.review(playerRef.getUuid())!=null);
        c.set("#ScoreField.Disabled",false);c.set("#FeedbackField.Disabled",false);
        for(String field:new String[]{"TitleField","DescriptionField","DeadlineField","ValueField"})c.set("#"+field+".Disabled",editing && activity.archived());
        if(activity!=null){
            if(editing && reset){var d=activity.editable();editRevision=activity.revision();c.set("#TitleField.Value",d.title());c.set("#DescriptionField.Value",d.description());c.set("#DeadlineField.Value",LearningViews.date(d.deadline()));c.set("#ValueField.Value",LearningViews.points(d.value()));}
            c.set("#StudentInfo.Visible",teacher && students && studentId!=null);
            if(teacher && students && studentId!=null) rows.items().stream().filter(r->r instanceof Student student && student.id().equals(studentId)).findFirst().ifPresent(r->{var student=(Student)r;c.set("#StudentInfo.TextSpans",text("learning.studentInfo").param("name",student.name()).param("enrollment",student.enrollment()).param("state",text(student.active()?"learning.activeStudent":"learning.formerStudent")));});
            if(!editing){
                var p=activity.published();c.set("#ReadTitle.TextSpans",p==null?text("learning.untitled"):Message.raw(p.title()));
                height(c,"#ReadTitle",ClassroomScreenRenderer.textHeight(p==null?"W".repeat(20):p.title(),600,21,27,32));
                c.set("#ReadDescription.TextSpans",Message.raw(p==null?"":p.description()));height(c,"#ReadDescription",textHeight(p==null?"":p.description()));
                c.set("#DeadlineState.Visible",p!=null && p.deadline()<=System.currentTimeMillis());
                c.set("#ReadMeta.TextSpans",p==null?text("learning.draft"):text("learning.activityMeta").param("version",p.number()).param("deadline",LearningViews.date(p.deadline())).param("value",LearningViews.points(p.value())));
            }
        }
        boolean show=submission!=null && (!teacher || students);
        c.set("#ActivityScore.Visible",!teacher && activity!=null && submission==null);
        if(!teacher && activity!=null && submission==null)c.set("#ActivityScore.TextSpans",text("learning.assessment").param("score","—").param("value",LearningViews.points(activity.published().value())));
        c.set("#NoSubmission.Visible",teacher && students && studentId!=null && submission==null);
        c.set("#Submission.Visible",show);c.set("#RevisionActions.Visible",show);c.set("#ReviewActions.Visible",show && teacher);c.set("#GradeEditor.Visible",show && teacher);
        if(show){
            c.set("#SubmissionMeta.TextSpans",text("learning.submissionMeta").param("revision",submission.revision()).param("version",submission.publication()).param("date",LearningViews.date(submission.at())).param("width",submission.width()).param("height",submission.height()).param("depth",submission.depth()).param("state",submissionLabel(submission)));
            boolean oldVersion=activity.published()!=null && submission.publication()!=activity.published().number();
            c.set("#VersionSnapshot.Visible",oldVersion);
            if(oldVersion){c.set("#VersionSnapshot.TextSpans",text("learning.deliveredVersion").param("version",submittedVersion.number()).param("title",submittedVersion.title()).param("description",submittedVersion.description()).param("deadline",LearningViews.date(submittedVersion.deadline())));height(c,"#VersionSnapshot",textHeight(submittedVersion.title()+"\n"+submittedVersion.description())+80);}
            var grade=submission.assessment();c.set("#Assessment.TextSpans",text("learning.assessment").param("score",grade==null?"—":LearningViews.points(grade.score())).param("value",LearningViews.points(submission.value())));
            c.set("#FeedbackText.TextSpans",Message.raw(grade==null?"":grade.feedback()));height(c,"#FeedbackText",textHeight(grade==null?"":grade.feedback()));
            c.set("#Older.Disabled",submission.revision()==1);c.set("#Newer.Disabled",revisionPage==0 && !revisions.items().isEmpty() && revisions.items().getFirst().id().equals(submission.id()));
            if(reset){gradeRevision=grade==null?0:grade.revision();c.set("#ScoreField.Value",grade==null?"":LearningViews.points(grade.score()));c.set("#FeedbackField.Value",grade==null?"":grade.feedback());}
        } else if(previewId!=null)clearPreview();
    }
    private Message guidance() {
        var session=areas.current(playerRef,world);
        if(session!=null && session.submission==null){
            try{var b=areas.sessions.bounds(session,runtime.learningLimits());return text("learning.selectionReady").param("width",b.width()).param("height",b.height()).param("depth",b.depth());}
            catch(LearningException ignored){return text("learning.selectionHelp");}
        }
        if(context.teacher() && students && studentId==null)return text("learning.chooseStudent");
        return text(activity==null?"learning.chooseActivity":context.teacher() && !students?"learning.editHelp":"learning.viewHelp");
    }
    private static Message submissionLabel(Submission s){return text(s.late()?"learning.late":"learning.submitted").param("revision",s.revision());}
    private void fetchPreview(Ref<EntityStore> ref,Store<EntityStore> store,ClassroomPageAccess.Stamp stamp) {
        if(submission==null || context.teacher() && !students || submission.id().equals(previewId) || previewLoading!=null)return;
        UUID requested=submission.id();previewId=requested;previewLoading=requested;
        areas.preview(playerRef.getUuid(),requested).whenComplete((packet,failure)->inWorld(ref,store,()->{
            previewLoading=null;
            if(!access.current(stamp) || !requested.equals(submissionId)){
                if(requested.equals(previewId))previewId=null;
                if(!busy && access.current(validated))fetchPreview(ref,store,access.current());return;
            }
            if(failure!=null){previewId=null;if(!busy){var c=new UICommandBuilder();c.set("#Status.TextSpans",error(failure));publish(ref,store,c,new UIEventBuilder());}return;}
            playerRef.getPacketHandler().writeNoCache(packet);
        }));
    }
    @Override public void handleDataEvent(Ref<EntityStore> ref,Store<EntityStore> store,Event event) {
        if("Close".equals(event.action)){dispose();close();return;}
        if("Back".equals(event.action)){dispose();var next=new ClassroomPage(playerRef,runtime,world);store.getComponent(ref,Player.getComponentType()).getPageManager().openCustomPage(ref,store,next);next.load(ref,store);return;}
        if(busy || !access.current(validated)){acknowledge(ref,store);return;}
        try {
            switch(event.action==null?"":event.action){
                case "Reload"->{previewId=null;refresh(ref,store,true,null);}
                case "Activities"->{students=false;studentId=null;submissionId=null;page=0;revisionPage=0;clearPreview();refresh(ref,store,true,null);}
                case "Filter"->{filter=LearningStore.Filter.values()[(filter.ordinal()+1)%LearningStore.Filter.values().length];activityId=null;submissionId=null;page=0;refresh(ref,store,true,null);}
                case "Previous"->{if(students){studentId=null;submissionId=null;clearPreview();}if(page>0)page--;refresh(ref,store,true,null);}
                case "Next"->{if(students){studentId=null;submissionId=null;clearPreview();}if(rows.hasNext())page++;refresh(ref,store,true,null);}
                case "New"->mutate(ref,store,runtime.learn(s->s.create(playerRef.getUuid())),a->{activityId=((Activity)a).id();students=false;page=0;});
                case "Select"->{
                    UUID id=UUID.fromString(event.target);
                    if(students){if(rows.items().stream().noneMatch(r->r instanceof Student s && s.id().equals(id))){acknowledge(ref,store);return;}studentId=id;}
                    else{if(rows.items().stream().noneMatch(r->r instanceof Activity a && a.id().equals(id))){acknowledge(ref,store);return;}activityId=id;studentId=null;}
                    submissionId=null;revisionPage=0;clearPreview();refresh(ref,store,true,null);
                }
                case "SaveDraft","Publish"->{
                    if(activity==null || students){acknowledge(ref,store);return;}
                    var draft=new Draft(event.title,event.description,LearningViews.deadline(event.deadline),LearningViews.points(event.value));
                    UUID id=activity.id();long revision=editRevision;boolean publish="Publish".equals(event.action);
                    mutate(ref,store,runtime.learn(s->{var a=s.save(playerRef.getUuid(),id,revision,draft);return publish?s.publish(playerRef.getUuid(),id,a.revision()):a;}),ignored->{});
                }
                case "Archive","Restore"->{if(activity!=null){var a=activity;mutate(ref,store,runtime.learn(s->s.archive(playerRef.getUuid(),a.id(),editRevision,"Archive".equals(event.action))),ignored->{});}}
                case "Students"->{if(activity!=null){students=true;studentId=null;submissionId=null;page=0;clearPreview();refresh(ref,store,true,null);}}
                case "Deliver"->{if(activity!=null)mode(ref,store,areas.beginSelection(playerRef,world,activity.id()));}
                case "SubmitArea"->mutate(ref,store,areas.submit(playerRef,world),ignored->submissionId=null,false);
                case "CancelSelection"->{areas.cancel(playerRef,world);refresh(ref,store,false,null);}
                case "Older","Newer"->revision(ref,store,"Older".equals(event.action));
                case "SaveGrade"->{
                    if(submission!=null){Long score=LearningViews.points(event.score);if(score==null)throw new LearningException(LearningException.Code.INVALID_SCORE);
                        UUID id=submission.id();int expected=gradeRevision;mutate(ref,store,runtime.learn(s->s.grade(playerRef.getUuid(),id,expected,score,event.feedback)),ignored->{});}
                }
                case "Paste"->{if(submission!=null)mode(ref,store,areas.beginPaste(playerRef,world,submission.id()));}
                case "Discard"->mutate(ref,store,areas.discard(playerRef,world),ignored->{},false);
                default->acknowledge(ref,store);
            }
        } catch(RuntimeException failure){fail(ref,store,failure);}
    }
    private void revision(Ref<EntityStore> ref,Store<EntityStore> store,boolean older) {
        if(submission==null){acknowledge(ref,store);return;}
        int index=-1;for(int i=0;i<revisions.items().size();i++)if(revisions.items().get(i).id().equals(submissionId))index=i;
        int target=index+(older?1:-1);
        if(target>=0 && target<revisions.items().size()){submissionId=revisions.items().get(target).id();clearPreview();refresh(ref,store,true,null);}
        else if(older && revisions.hasNext()){revisionPage++;submissionId=null;clearPreview();refresh(ref,store,true,null);}
        else if(!older && revisionPage>0){revisionPage--;submissionId=null;clearPreview();refresh(ref,store,true,null);}
        else acknowledge(ref,store);
    }
    private void mode(Ref<EntityStore> ref,Store<EntityStore> store,CompletableFuture<?> future){
        busy=true;var waiting=new UICommandBuilder();controls(waiting,true);waiting.set("#Status.TextSpans",text("learning.loading"));publish(ref,store,waiting,new UIEventBuilder());var stamp=access.current();future.whenComplete((value,failure)->inWorld(ref,store,()->{
            busy=false;if(!access.current(stamp)){refresh(ref,store,false,null);return;}if(failure!=null)fail(ref,store,failure);else {dispose();close();}
        }));
    }
    private void mutate(Ref<EntityStore> ref,Store<EntityStore> store,CompletableFuture<?> future,Consumer<Object> done){
        mutate(ref,store,future,done,true);
    }
    private void mutate(Ref<EntityStore> ref,Store<EntityStore> store,CompletableFuture<?> future,Consumer<Object> done,boolean notify){
        busy=true;saving=true;var stamp=access.current();var c=new UICommandBuilder();controls(c,true);for(String f:FIELDS)c.set("#"+f+".Disabled",true);c.set("#Status.TextSpans",text("classroom.saving"));publish(ref,store,c,new UIEventBuilder());
        future.whenComplete((value,failure)->inWorld(ref,store,()->{
            busy=false;saving=false;if(!access.current(stamp)){refresh(ref,store,false,null);return;}
            if(failure!=null){fail(ref,store,failure);return;}done.accept(value);if(notify)runtime.learningChanged(context.classroom());refresh(ref,store,true,text("classroom.done"));
        }));
    }
    private void fail(Ref<EntityStore> ref,Store<EntityStore> store,Throwable failure){
        busy=false;saving=false;var c=new UICommandBuilder();controls(c,false);for(String f:FIELDS)c.set("#"+f+".Disabled",false);c.set("#Status.TextSpans",error(failure));
        var cause=unwrap(failure);
        if(cause instanceof sisso.logicsim.classroom.domain.ClassroomException ex && ex.reason()==sisso.logicsim.classroom.domain.ClassroomException.Reason.FORBIDDEN){
            c.set("#Body.Visible",false);c.set("#New.Visible",false);c.set("#Filter.Visible",false);for(String f:FIELDS)c.set("#"+f+".Value","");clearPreview();context=null;
        }
        publish(ref,store,c,new UIEventBuilder());
    }
    public static Message error(Throwable failure){var cause=unwrap(failure);return cause instanceof LearningException e?text("learning.error."+e.code().name()).param("problem",e.problem()):ClassroomPage.error(cause);}
    private static Throwable unwrap(Throwable e){while((e instanceof CompletionException || e instanceof ExecutionException)&&e.getCause()!=null)e=e.getCause();return e;}
    private boolean active(Ref<EntityStore> ref,Store<EntityStore> store){return !dismissed && ref.isValid() && ref.equals(playerRef.getReference()) && ref.getStore()==store && store.getExternalData().getWorld()==world && store.getComponent(ref,Player.getComponentType())!=null && store.getComponent(ref,Player.getComponentType()).getPageManager().getCustomPage()==this;}
    private void inWorld(Ref<EntityStore> ref,Store<EntityStore> store,Runnable action){try{world.execute(()->{if(active(ref,store))action.run();});}catch(RuntimeException stopped){dispose();}}
    private void schedule(Ref<EntityStore> ref,Store<EntityStore> store,Runnable action){try{refreshGate.schedule(world::execute,()->{if(active(ref,store))action.run();});}catch(RuntimeException stopped){dispose();}}
    private static void controls(UICommandBuilder c,boolean disabled){for(String id:ACTIONS)c.set("#"+id+".Disabled",disabled);}
    private void publish(Ref<EntityStore> ref,Store<EntityStore> store,UICommandBuilder c,UIEventBuilder e){if(active(ref,store))store.getComponent(ref,Player.getComponentType()).getPageManager().updateLegacyCustomPage(updates.packet(getClass().getName(),getLifetime(),c,e,rows.items()));}
    private void acknowledge(Ref<EntityStore> ref,Store<EntityStore> store){publish(ref,store,new UICommandBuilder(),new UIEventBuilder());}
    private static void height(UICommandBuilder c,String id,int pixels){var a=new Anchor();a.setHeight(Value.of(pixels));a.setBottom(Value.of(8));c.setObject(id+".Anchor",a);}
    private static String shortText(String s){return shortText(s,28);}
    private static String shortText(String s,int limit){return s.codePointCount(0,s.length())<=limit?s:s.substring(0,s.offsetByCodePoints(0,limit-3))+"...";}
    private static int textHeight(String s){return ClassroomScreenRenderer.textHeight(s,600,17,24,24);}
    private void clearPreview(){previewId=null;if(!dismissed)try{playerRef.getPacketHandler().writeNoCache(new BuilderToolPrefabPreview());}catch(RuntimeException disconnected){ /* Connection already closed. */ }}
    private static void close(AutoCloseable c){if(c!=null)try{c.close();}catch(Exception ignored){}}
    private void dispose(){if(dismissed)return;clearPreview();dismissed=true;close(dataSubscription);close(authSubscription);close(learningSubscription);}
    @Override public void onDismiss(Ref<EntityStore> ref,Store<EntityStore> store){dispose();}
    public static final class Event {
        public static final BuilderCodec<Event> CODEC=BuilderCodec.builder(Event.class,Event::new)
            .append(new KeyedCodec<>("Action",Codec.STRING),(e,v)->e.action=v,e->e.action).add()
            .append(new KeyedCodec<>("Target",Codec.STRING),(e,v)->e.target=v,e->e.target).add()
            .append(new KeyedCodec<>("@Title",Codec.STRING),(e,v)->e.title=v,e->e.title).add()
            .append(new KeyedCodec<>("@Description",Codec.STRING),(e,v)->e.description=v,e->e.description).add()
            .append(new KeyedCodec<>("@Deadline",Codec.STRING),(e,v)->e.deadline=v,e->e.deadline).add()
            .append(new KeyedCodec<>("@Value",Codec.STRING),(e,v)->e.value=v,e->e.value).add()
            .append(new KeyedCodec<>("@Score",Codec.STRING),(e,v)->e.score=v,e->e.score).add()
            .append(new KeyedCodec<>("@Feedback",Codec.STRING),(e,v)->e.feedback=v,e->e.feedback).add().build();
        public String action,target,title,description,deadline,value,score,feedback;
    }
}
