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
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomViews.Page;
import sisso.logicsim.classroom.learning.*;
import sisso.logicsim.classroom.learning.LearningViews.*;
import sisso.logicsim.circuit.CircuitNode.Kind;
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
    private UUID verificationId;
    private SubmissionVerifier.Report verificationReport;
    private Publication submittedVersion;
    private Page<?> rows=new Page<>(List.of(),0,false);
    private Page<Submission> revisions=new Page<>(List.of(),0,false);
    private LearningStore.Filter filter=LearningStore.Filter.ALL;
    private boolean students,creating,editing,busy=true,dismissed,pending,saving,discardDrafts;
    private int page,revisionPage,gradeRevision;
    private long editRevision;
    private static final String[] ACTIONS={"New","Filter","Activities","Previous","Next","Edit","CancelEdit","SaveDraft","Publish","Archive","Restore","Students","Deliver","SubmitArea","CancelSelection","Newer","Older","SaveGrade","Paste","Discard","Verify"};
    private static final String[] FIELDS={"TitleField","DescriptionField","ValueField","ScoreField","FeedbackField"};
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
        for (String action : new String[]{"Back", "Close", "Reload"}) bind(e, action, EventData.of("Action", action));
        for (String action : ACTIONS) {
            EventData fields = switch (action) {
                case "SaveDraft", "Publish" -> activityFields(action);
                case "SaveGrade" -> EventData.of("Action", action)
                        .append("@Score", "#ScoreField.Value").append("@Feedback", "#FeedbackField.Value");
                default -> EventData.of("Action", action);
            };
            bind(e, action, fields);
        }
        for(String id:new String[]{"DeadlineMonth","DeadlineYear"})e.addEventBinding(CustomUIEventBindingType.ValueChanged,"#"+id,EventData.of("Action","DeadlineChanged")
                .append("@Day","#DeadlineDay.Value").append("@Month","#DeadlineMonth.Value").append("@Year","#DeadlineYear.Value"));
        controls(c,true);
    }
    private static void bind(UIEventBuilder events, String action, EventData fields) {
        events.addEventBinding(CustomUIEventBindingType.Activating, "#" + action, fields);
    }

    private static EventData activityFields(String action) {
        var fields = EventData.of("Action", action)
                .append("@Title", "#TitleField.Value")
                .append("@Description", "#DescriptionField.Value")
                .append("@Value", "#ValueField.Value")
                .append("@Day", "#DeadlineDay.Value")
                .append("@Month", "#DeadlineMonth.Value")
                .append("@Year", "#DeadlineYear.Value")
                .append("@Hour", "#DeadlineHour.Value")
                .append("@Minute", "#DeadlineMinute.Value")
                .append("@TruthTable", "#TruthTableField.Value");
        for (var kind : Kind.values()) fields.append("@Required" + kind, "#Required" + kind + ".Value");
        return fields;
    }
    private void load(Ref<EntityStore> ref,Store<EntityStore> store) {
        areas.observe(playerRef,world);
        authSubscription=runtime.listenAuthorization(playerRef.getUuid(),allowed->{
            access.changed(allowed);
            schedule(ref,store,()->{
                if(!allowed && (context==null || context.teacher())){
                    discardDrafts=true;
                    var c=new UICommandBuilder();discardEditor(c);c.set("#Details.Visible",false);publish(ref,store,c,new UIEventBuilder());clearPreview();
                }
                requestRefresh(ref,store);
            });
        });
        dataSubscription=runtime.listen(playerRef.getUuid(),this::interested,change->
            schedule(ref,store,()->requestRefresh(ref,store)));
        learningSubscription=runtime.listenLearning(playerRef.getUuid(),classroom->{
            var ctx=context;if(ctx==null || ctx.classroom().equals(classroom))schedule(ref,store,()->requestRefresh(ref,store));
        });
        refresh(ref,store,true,null);
    }
    private boolean interested(ClassroomRuntime.Change change) {
        var ctx=context;return ctx==null || ctx.classroom().equals(change.classroom()) || playerRef.getUuid().equals(change.player());
    }
    private void requestRefresh(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (busy) pending = true;
        else refresh(ref, store, false, null);
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
            if(!context.teacher() || activity==null || activity.archived()){creating=false;editing=false;}
            submissionId=submission==null?null:submission.id();
            publishRendered(ref,store,resetFields,status);
            fetchPreview(ref,store,stamp);
            if(pending)refresh(ref,store,false,null);
        }));
    }
    private void render(UICommandBuilder c,UIEventBuilder e,boolean reset) {
        render(c,e,reset,areas.current(playerRef,world),context.teacher() && areas.review(playerRef.getUuid())!=null);
    }
    /** Render metadata independently of the world/connection; native input fields have no Disabled property. */
    void render(UICommandBuilder c,UIEventBuilder e,boolean reset,AreaSessions.Session selection,boolean hasReview) {
        boolean teacher = context.teacher();
        boolean editor = editingActivity();
        c.set("#Body.Visible", true);
        c.set("#Details.Visible", true);
        controls(c, false);
        c.set("#Heading.TextSpans", Message.raw(context.name()));
        renderList(c, e, teacher);
        renderActions(c, teacher, editor, selection, hasReview);
        if (activity != null) {
            if (editor && reset) renderEditor(c);
            renderStudent(c, teacher);
            if (!editor) renderActivity(c, teacher && !students);
        }
        renderSubmission(c, teacher, reset);
    }

    private void renderList(UICommandBuilder c, UIEventBuilder e, boolean teacher) {
        c.set("#New.Visible", teacher && !creating);
        c.set("#Filter.Visible", teacher && !students && !creating);
        c.set("#ListPanel.Visible", !creating);
        c.set("#Activities.TextSpans", text(creating ? "learning.backToActivities" : "learning.activities"));
        c.set("#Filter.TextSpans", text("learning.filter." + filter.name()));
        c.set("#Empty.Visible", rows.items().isEmpty());
        c.set("#Empty.TextSpans", text(students ? "learning.noStudents" : "learning.noActivities"));
        c.set("#Page.TextSpans", text("classroom.page").param("page", page + 1));
        c.set("#Previous.Disabled", page == 0);
        c.set("#Next.Disabled", !rows.hasNext());
        c.clear("#Rows");
        int index = 0;
        for (Object row : rows.items()) {
            String selector = "#Rows[" + index++ + "]";
            c.append("#Rows", ClassroomPage.ROW_LAYOUT);
            renderRow(c, e, selector, row, teacher);
        }
    }

    private void renderRow(UICommandBuilder c, UIEventBuilder e, String selector, Object row, boolean teacher) {
        height(c, selector, row instanceof Student ? 132 : 112);
        UUID id;
        String title;
        Message info;
        boolean selected;
        if (row instanceof Activity listed) {
            id = listed.id();
            String name = teacher ? listed.title() : listed.published() == null ? "" : listed.published().title();
            title = name.isBlank() ? null : shortText(name);
            info = text(listed.archived() ? "learning.archived"
                    : teacher && listed.draft() != null ? "learning.draft" : "learning.published");
            selected = id.equals(activityId);
        } else {
            var student = (Student) row;
            id = student.id();
            title = shortText(student.name());
            info = student.latest() == null ? text("learning.notSubmitted") : submissionLabel(student.latest());
            selected = id.equals(studentId);
        }
        c.set(selector + " #RowName.TextSpans", title == null ? text("learning.untitled") : Message.raw(title));
        c.set(selector + " #RowInfo.TextSpans", info);
        c.set(selector + " #SelectionMark.Visible", selected);
        c.set(selector + " #RowStamp.Visible", row instanceof Student);
        if (row instanceof Student student)
            c.set(selector + " #RowStamp.TextSpans", Message.raw(shortText(student.enrollment(), 20)));
        e.addEventBinding(CustomUIEventBindingType.Activating, selector,
                EventData.of("Action", "Select").append("Target", id.toString()));
    }

    private void renderActions(UICommandBuilder c, boolean teacher, boolean editor,
                               AreaSessions.Session selection, boolean hasReview) {
        height(c, "#DescriptionField", creating ? 260 : 220);
        boolean catalog = teacher && !students && activity != null && !creating;
        c.set("#Editor.Visible", editor);
        c.set("#Read.Visible", activity != null && !editor);
        c.set("#ActivityActions.Visible", editor);
        c.set("#CatalogActions.Visible", catalog);
        c.set("#Edit.Visible", catalog && !editor && !activity.archived());
        c.set("#CancelEdit.Visible", editor && !creating);
        c.set("#Restore.Visible", catalog && activity.archived());
        c.set("#Archive.Visible", catalog && !activity.archived());
        c.set("#Students.Visible", catalog && !editor);
        c.set("#Deliver.Visible", !teacher && activity != null);
        c.set("#SubmitArea.Visible", !teacher && selection != null && selection.submission == null
                && selection.activity.equals(activityId));
        c.set("#CancelSelection.Visible", selection != null);
        c.set("#Discard.Visible", teacher && hasReview);
    }

    private void renderEditor(UICommandBuilder c) {
        var draft = activity.editable();
        editRevision = activity.revision();
        c.set("#TitleField.Value", draft.title());
        c.set("#DescriptionField.Value", draft.description());
        LearningDeadline.render(c, draft.deadline(), currentYear());
        c.set("#ValueField.Value", LearningViews.points(draft.value()));
        LearningVerification.render(c, draft.rules());
    }

    private void renderStudent(UICommandBuilder c, boolean teacher) {
        boolean show = teacher && students && studentId != null;
        c.set("#StudentInfo.Visible", show);
        if (!show) return;
        rows.items().stream().filter(Student.class::isInstance).map(Student.class::cast)
                .filter(student -> student.id().equals(studentId)).findFirst()
                .ifPresent(student -> c.set("#StudentInfo.TextSpans", text("learning.studentInfo")
                        .param("name", student.name()).param("enrollment", student.enrollment())
                        .param("state", text(student.active() ? "learning.activeStudent" : "learning.formerStudent"))));
    }

    private void renderSubmission(UICommandBuilder c, boolean teacher, boolean reset) {
        boolean show = submission != null && (!teacher || students);
        boolean report = show && teacher && submission.id().equals(verificationId) && verificationReport != null;
        c.set("#ActivityScore.Visible", !teacher && activity != null && submission == null);
        if (!teacher && activity != null && submission == null)
            c.set("#ActivityScore.TextSpans", text("learning.assessment").param("score", "—")
                    .param("value", LearningViews.points(activity.published().value())));
        c.set("#NoSubmission.Visible", teacher && students && studentId != null && submission == null);
        c.set("#VerificationResults.Visible", report);
        c.set("#Verify.Visible", show && teacher);
        c.set("#Submission.Visible", show);
        c.set("#RevisionActions.Visible", show);
        c.set("#ReviewActions.Visible", show && teacher);
        c.set("#GradeEditor.Visible", show && teacher);
        if (!show) {
            if (previewId != null) clearPreview();
            return;
        }
        if (report) c.set("#VerificationResults.TextSpans", LearningVerification.report(verificationReport, submission));
        c.set("#SubmissionRules.Visible", submittedVersion != null && submittedVersion.rules().configured());
        if (submittedVersion != null)
            c.set("#SubmissionRules.TextSpans", LearningVerification.deliveredCriteria(submittedVersion));
        c.set("#SubmissionMeta.TextSpans", text("learning.submissionMeta")
                .param("revision", submission.revision()).param("version", submission.publication())
                .param("date", LearningViews.date(submission.at())).param("width", submission.width())
                .param("height", submission.height()).param("depth", submission.depth())
                .param("state", submissionLabel(submission)));
        boolean oldVersion = activity.published() != null && submission.publication() != activity.published().number();
        c.set("#VersionSnapshot.Visible", oldVersion);
        if (oldVersion) c.set("#VersionSnapshot.TextSpans", text("learning.deliveredVersion")
                .param("version", submittedVersion.number()).param("title", submittedVersion.title())
                .param("description", submittedVersion.description()).param("deadline", LearningViews.date(submittedVersion.deadline())));
        c.set("#Older.Disabled", submission.revision() == 1);
        c.set("#Newer.Disabled", revisionPage == 0 && !revisions.items().isEmpty()
                && revisions.items().getFirst().id().equals(submission.id()));
        renderAssessment(c, reset);
    }

    /** A report/background read must not overwrite an unsaved manual assessment. */
    private void renderAssessment(UICommandBuilder c, boolean reset) {
        var grade = submission.assessment();
        c.set("#Assessment.TextSpans", text("learning.assessment")
                .param("score", grade == null ? "—" : LearningViews.points(grade.score()))
                .param("value", LearningViews.points(submission.value())));
        c.set("#FeedbackText.Visible", grade != null && !grade.feedback().isEmpty());
        c.set("#FeedbackText.TextSpans", Message.raw(grade == null ? "" : grade.feedback()));
        if (reset) {
            gradeRevision = grade == null ? 0 : grade.revision();
            c.set("#ScoreField.Value", grade == null ? "" : LearningViews.points(grade.score()));
            c.set("#FeedbackField.Value", grade == null ? "" : grade.feedback());
        }
    }
    private void renderActivity(UICommandBuilder c, boolean privateView) {
        var publication = activity.published();
        Draft visible;
        if (privateView) visible = activity.editable();
        else if (publication != null) visible = publication.draft();
        else visible = context.teacher() ? activity.editable() : Draft.empty();

        boolean draft = publication == null || privateView && activity.draft() != null;
        boolean delivered = !privateView && submission != null;
        c.set("#ReadRequirements.Visible", !delivered && visible.rules().configured());
        c.set("#ReadRequirements.TextSpans", LearningVerification.summary(visible.rules()));
        c.set("#ReadTable.Visible", !delivered && !visible.rules().table().isEmpty());
        c.set("#ReadTable.TextSpans", LearningVerification.table(visible.rules()));
        c.set("#ReadTitle.TextSpans", visible.title().isBlank() ? text("learning.untitled") : Message.raw(visible.title()));
        c.set("#ReadDescription.TextSpans", Message.raw(visible.description()));
        c.set("#ReadDescription.Visible", !visible.description().isEmpty());
        c.set("#DeadlineState.Visible", visible.deadline() != null && visible.deadline() <= System.currentTimeMillis());
        c.set("#ReadState.TextSpans", text(activity.archived() ? "learning.archived" : draft ? "learning.draft" : "learning.published"));
        c.set("#DraftNotice.Visible", draft && publication != null);
        if (draft && publication != null)
            c.set("#DraftNotice.TextSpans", text("learning.privateDraftNotice").param("version", publication.number()));
        var metadata = text(draft ? "learning.draftMeta" : "learning.activityMeta")
                .param("deadline", visible.deadline() == null ? text("learning.notSet") : Message.raw(LearningViews.date(visible.deadline())))
                .param("value", visible.value() == null ? text("learning.notSet") : Message.raw(LearningViews.points(visible.value())));
        if (!draft) metadata = metadata.param("version", publication.number());
        c.set("#ReadMeta.TextSpans", metadata);
    }
    private Message guidance() {
        var session=areas.current(playerRef,world);
        if(session!=null && session.submission==null){
            try{var b=areas.sessions.bounds(session,runtime.learningLimits());return text("learning.selectionReady").param("width",b.width()).param("height",b.height()).param("depth",b.depth());}
            catch(LearningException ignored){return text("learning.selectionHelp");}
        }
        if(context.teacher() && students && studentId==null)return text("learning.chooseStudent");
        if(context.teacher() && !students && activity!=null && activity.archived())return text("learning.archivedViewHelp");
        return text(activity==null?"learning.chooseActivity":editingActivity()?"learning.editHelp":context.teacher() && !students?"learning.activityViewHelp":"learning.viewHelp");
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
        // Date changes are local presentation work: keep their day list current during a background read.
        if((busy && !"DeadlineChanged".equals(event.action)) || saving || !access.current(validated)){acknowledge(ref,store);return;}
        if(context==null && !"Reload".equals(event.action)){acknowledge(ref,store);return;}
        try {
            switch(event.action==null?"":event.action){
                case "Reload"->{previewId=null;refresh(ref,store,true,null);}
                case "Activities"->{backToActivities();clearPreview();refresh(ref,store,true,null);}
                case "Filter"->{editing=false;filter=LearningStore.Filter.values()[(filter.ordinal()+1)%LearningStore.Filter.values().length];activityId=null;submissionId=null;page=0;refresh(ref,store,true,null);}
                case "Previous", "Next" -> changePage(ref, store, "Next".equals(event.action));
                case "Edit" -> {
                    if (!beginEditing()) { acknowledge(ref, store); return; }
                    publishRendered(ref, store, true, null);
                }
                case "CancelEdit" -> {
                    if (!viewActivity()) { acknowledge(ref, store); return; }
                    publishRendered(ref, store, false, null);
                }
                case "New"->{if(!context.teacher() || creating){acknowledge(ref,store);return;}mutate(ref,store,runtime.learn(s->s.create(playerRef.getUuid())),a->{created((Activity)a);clearPreview();},true,true);}
                case "DeadlineChanged"->{
                    if(!editingActivity()){acknowledge(ref,store);return;}
                    var c=new UICommandBuilder();
                    try{if(LearningDeadline.days(c,event.dateParts()))c.set("#Status.TextSpans",text("learning.dayReset"));}
                    catch(IllegalArgumentException invalid){c.set("#Status.TextSpans",error(invalid));}
                    publish(ref,store,c,new UIEventBuilder());
                }
                case "Select"->{
                    UUID id=UUID.fromString(event.target);
                    if(students){if(rows.items().stream().noneMatch(r->r instanceof Student s && s.id().equals(id))){acknowledge(ref,store);return;}studentId=id;}
                    else{if(!selectActivity(id)){acknowledge(ref,store);return;}}
                    submissionId=null;revisionPage=0;clearPreview();refresh(ref,store,true,null);
                }
                case "SaveDraft","Publish"->{
                    if(!editingActivity()){acknowledge(ref,store);return;}
                    var draft=new Draft(event.title,event.description,LearningDeadline.deadline(event.dateParts()),LearningViews.points(event.value),LearningVerification.rules(event.quantities,event.truthTable));
                    UUID id=activity.id();long revision=editRevision;boolean publish="Publish".equals(event.action);
                    mutate(ref,store,runtime.learn(s->publish?s.saveAndPublish(playerRef.getUuid(),id,revision,draft):s.save(playerRef.getUuid(),id,revision,draft)),ignored->saved(publish),true,publish);
                }
                case "Archive","Restore"->{if(context.teacher() && !students && activity!=null && !creating){var a=activity;long revision=editingActivity()?editRevision:a.revision();mutate(ref,store,runtime.learn(s->s.archive(playerRef.getUuid(),a.id(),revision,"Archive".equals(event.action))),ignored->{editing=false;},true,true);}}
                case "Students"->{if(context.teacher() && activity!=null && !creating){editing=false;students=true;studentId=null;submissionId=null;page=0;clearPreview();refresh(ref,store,true,null);}}
                case "Deliver"->{if(activity!=null)mode(ref,store,areas.beginSelection(playerRef,world,activity.id()));}
                case "SubmitArea"->mutate(ref,store,areas.submit(playerRef,world),ignored->submissionId=null,false);
                case "CancelSelection"->{areas.cancel(playerRef,world);refresh(ref,store,false,null);}
                case "Older","Newer"->revision(ref,store,"Older".equals(event.action));
                case "SaveGrade"->{
                    if(submission!=null){Long score=LearningViews.points(event.score);if(score==null)throw new LearningException(LearningException.Code.INVALID_SCORE);
                        UUID id=submission.id();int expected=gradeRevision;mutate(ref,store,runtime.learn(s->s.grade(playerRef.getUuid(),id,expected,score,event.feedback)),ignored->{});}
                }
                case "Verify"->{if(context.teacher() && students && submission!=null)verify(ref,store);else acknowledge(ref,store);}
                case "Paste"->{if(submission!=null)mode(ref,store,areas.beginPaste(playerRef,world,submission.id()));}
                case "Discard"->mutate(ref,store,areas.discard(playerRef,world),ignored->{},false);
                default->acknowledge(ref,store);
            }
        } catch(RuntimeException failure){fail(ref,store,failure);}
    }
    private void verify(Ref<EntityStore> ref,Store<EntityStore> store){
        UUID requested=submission.id();var stamp=access.current();busy=true;
        publish(ref,store,operationState(true,text("learning.verifying")),new UIEventBuilder());
        areas.verify(playerRef.getUuid(),requested).whenComplete((report,failure)->inWorld(ref,store,()->{
            busy=false;
            if(!access.current(stamp)){refresh(ref,store,false,null);return;}
            if(!requested.equals(submissionId)){acknowledge(ref,store);return;}
            if(failure!=null){fail(ref,store,failure);return;}
            verificationId=requested;verificationReport=report;
            publishRendered(ref,store,false,text("learning.verificationReady"));
            if(pending)refresh(ref,store,false,null);
        }));
    }
    private void changePage(Ref<EntityStore> ref, Store<EntityStore> store, boolean next) {
        editing = false;
        if (students) {
            studentId = null;
            submissionId = null;
            clearPreview();
        }
        if (next && rows.hasNext()) page++;
        else if (!next && page > 0) page--;
        refresh(ref, store, true, null);
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
    boolean editingActivity() {
        return context != null && context.teacher() && !students && activity != null && !activity.archived() && editing;
    }

    boolean beginEditing() {
        if (context == null || !context.teacher() || students || activity == null
                || activity.archived() || creating || editing) return false;
        editing = true;
        return true;
    }

    boolean viewActivity() {
        if (!editingActivity() || creating) return false;
        editing = false;
        return true;
    }

    boolean selectActivity(UUID id) {
        if (rows.items().stream().noneMatch(row -> row instanceof Activity listed && listed.id().equals(id))) return false;
        activityId = id;
        studentId = null;
        editing = false;
        creating = false;
        return true;
    }

    void created(Activity value) {
        activityId = value.id();
        students = false;
        creating = true;
        editing = true;
        studentId = null;
        submissionId = null;
        page = 0;
        revisionPage = 0;
    }

    void saved(boolean published) {
        if (published) {
            creating = false;
            editing = false;
            filter = LearningStore.Filter.ALL;
        }
    }

    void backToActivities() {
        if (creating) {
            creating = false;
            activityId = null;
            filter = LearningStore.Filter.ALL;
        }
        editing = false;
        students = false;
        studentId = null;
        submissionId = null;
        page = 0;
        revisionPage = 0;
    }

    /** Forget all sensitive inputs together when teacher authorization is lost. */
    void discardEditor(UICommandBuilder c) {
        creating = false;
        editing = false;
        for (String field : FIELDS) c.set("#" + field + ".Value", "");
        LearningDeadline.render(c, null, currentYear());
        LearningVerification.render(c, VerificationRules.EMPTY);
    }
    private void mode(Ref<EntityStore> ref, Store<EntityStore> store, CompletableFuture<?> future) {
        busy = true;
        publish(ref, store, operationState(true, text("learning.loading")), new UIEventBuilder());
        var stamp = access.current();
        future.whenComplete((value, failure) -> inWorld(ref, store, () -> {
            busy = false;
            if (!access.current(stamp)) {
                refresh(ref, store, false, null);
                return;
            }
            if (failure != null) fail(ref, store, failure);
            else {
                dispose();
                close();
            }
        }));
    }
    private void mutate(Ref<EntityStore> ref,Store<EntityStore> store,CompletableFuture<?> future,Consumer<Object> done){
        mutate(ref,store,future,done,true);
    }
    private void mutate(Ref<EntityStore> ref,Store<EntityStore> store,CompletableFuture<?> future,Consumer<Object> done,boolean notify){
        mutate(ref,store,future,done,notify,false);
    }
    private void mutate(Ref<EntityStore> ref, Store<EntityStore> store, CompletableFuture<?> future,
                        Consumer<Object> done, boolean notify, boolean catalog) {
        busy = true;
        saving = true;
        var stamp = access.current();
        publish(ref, store, operationState(true, text("classroom.saving")), new UIEventBuilder());
        UUID classroom = context.classroom();
        future.whenComplete((value, failure) -> {
            // Notify committed changes even when the initiating page has closed or lost permission.
            if (failure == null && notify) {
                if (catalog) runtime.activitiesChanged(classroom);
                else runtime.learningChanged(classroom);
            }
            inWorld(ref, store, () -> {
                busy = false;
                saving = false;
                if (!access.current(stamp)) {
                    refresh(ref, store, false, null);
                    return;
                }
                if (failure != null) {
                    fail(ref, store, failure);
                    return;
                }
                done.accept(value);
                refresh(ref, store, true, text("classroom.done"));
            });
        });
    }
    private void fail(Ref<EntityStore> ref, Store<EntityStore> store, Throwable failure) {
        busy = false;
        saving = false;
        var commands = operationState(false, error(failure));
        var cause = unwrap(failure);
        if (cause instanceof ClassroomException ex && ex.reason() == ClassroomException.Reason.FORBIDDEN) {
            discardEditor(commands);
            commands.set("#Body.Visible", false);
            commands.set("#New.Visible", false);
            commands.set("#Filter.Visible", false);
            clearPreview();
            context = null;
        }
        commands.set("#Previous.Disabled", page == 0);
        commands.set("#Next.Disabled", !rows.hasNext());
        publish(ref, store, commands, new UIEventBuilder());
    }

    public static Message error(Throwable failure) {
        var cause = unwrap(failure);
        if (cause instanceof LearningDeadline.Invalid invalid) return text(invalid.key);
        if (cause instanceof LearningException ex)
            return text("learning.error." + ex.code().name()).param("problem", ex.problem());
        return ClassroomPage.error(cause);
    }

    private static Throwable unwrap(Throwable failure) {
        while ((failure instanceof CompletionException || failure instanceof ExecutionException) && failure.getCause() != null)
            failure = failure.getCause();
        return failure;
    }

    private boolean active(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (dismissed || !ref.isValid() || !ref.equals(playerRef.getReference())
                || ref.getStore() != store || store.getExternalData().getWorld() != world) return false;
        var player = store.getComponent(ref, Player.getComponentType());
        return player != null && player.getPageManager().getCustomPage() == this;
    }

    private void inWorld(Ref<EntityStore> ref, Store<EntityStore> store, Runnable action) {
        try {
            world.execute(() -> { if (active(ref, store)) action.run(); });
        } catch (RuntimeException stopped) {
            dispose();
        }
    }

    private void schedule(Ref<EntityStore> ref, Store<EntityStore> store, Runnable action) {
        try {
            refreshGate.schedule(world::execute, () -> { if (active(ref, store)) action.run(); });
        } catch (RuntimeException stopped) {
            dispose();
        }
    }
    private static void controls(UICommandBuilder c,boolean disabled){for(String id:ACTIONS)c.set("#"+id+".Disabled",disabled);}
    /** Busy events are also rejected by handleDataEvent; only buttons support Disabled in Hytale 0.7.4. */
    static UICommandBuilder operationState(boolean busy,Message status){var c=new UICommandBuilder();controls(c,busy);c.set("#Status.TextSpans",status);return c;}
    private void publishRendered(Ref<EntityStore> ref, Store<EntityStore> store, boolean reset, Message status) {
        var commands = new UICommandBuilder();
        var events = new UIEventBuilder();
        render(commands, events, reset);
        commands.set("#Status.TextSpans", status == null ? guidance() : status);
        publish(ref, store, commands, events);
    }
    private void publish(Ref<EntityStore> ref,Store<EntityStore> store,UICommandBuilder c,UIEventBuilder e){if(active(ref,store))store.getComponent(ref,Player.getComponentType()).getPageManager().updateLegacyCustomPage(updates.packet(getClass().getName(),getLifetime(),c,e,rows.items()));}
    private void acknowledge(Ref<EntityStore> ref,Store<EntityStore> store){publish(ref,store,new UICommandBuilder(),new UIEventBuilder());}
    private static void height(UICommandBuilder c,String id,int pixels){var a=new Anchor();a.setHeight(Value.of(pixels));a.setBottom(Value.of(8));c.setObject(id+".Anchor",a);}
    private static String shortText(String s){return shortText(s,28);}
    private static String shortText(String s,int limit){return s.codePointCount(0,s.length())<=limit?s:s.substring(0,s.offsetByCodePoints(0,limit-3))+"...";}
    private static int currentYear(){return java.time.LocalDate.now(LearningViews.BRASILIA).getYear();}
    private void clearPreview(){verificationId=null;verificationReport=null;previewId=null;if(!dismissed)try{playerRef.getPacketHandler().writeNoCache(new BuilderToolPrefabPreview());}catch(RuntimeException disconnected){ /* Connection already closed. */ }}
    private static void close(AutoCloseable c){if(c!=null)try{c.close();}catch(Exception ignored){}}
    private void dispose(){if(dismissed)return;clearPreview();dismissed=true;close(dataSubscription);close(authSubscription);close(learningSubscription);}
    @Override public void onDismiss(Ref<EntityStore> ref,Store<EntityStore> store){dispose();}
    public static final class Event {
        public static final BuilderCodec<Event> CODEC=codec();
        private static BuilderCodec<Event> codec(){var builder=BuilderCodec.builder(Event.class,Event::new)
            .append(new KeyedCodec<>("Action",Codec.STRING),(e,v)->e.action=v,e->e.action).add()
            .append(new KeyedCodec<>("Target",Codec.STRING),(e,v)->e.target=v,e->e.target).add()
            .append(new KeyedCodec<>("@Title",Codec.STRING),(e,v)->e.title=v,e->e.title).add()
            .append(new KeyedCodec<>("@Description",Codec.STRING),(e,v)->e.description=v,e->e.description).add()
            .append(new KeyedCodec<>("@Day",Codec.STRING),(e,v)->e.day=v,e->e.day).add()
            .append(new KeyedCodec<>("@Month",Codec.STRING),(e,v)->e.month=v,e->e.month).add()
            .append(new KeyedCodec<>("@Year",Codec.STRING),(e,v)->e.year=v,e->e.year).add()
            .append(new KeyedCodec<>("@Hour",Codec.STRING),(e,v)->e.hour=v,e->e.hour).add()
            .append(new KeyedCodec<>("@Minute",Codec.STRING),(e,v)->e.minute=v,e->e.minute).add()
            .append(new KeyedCodec<>("@Value",Codec.STRING),(e,v)->e.value=v,e->e.value).add()
            .append(new KeyedCodec<>("@Score",Codec.STRING),(e,v)->e.score=v,e->e.score).add()
            .append(new KeyedCodec<>("@Feedback",Codec.STRING),(e,v)->e.feedback=v,e->e.feedback).add()
            .append(new KeyedCodec<>("@TruthTable",Codec.STRING),(e,v)->e.truthTable=v,e->e.truthTable).add();
            var kinds=Kind.values();
            for(int i=0;i<kinds.length;i++){final int index=i;builder.append(new KeyedCodec<>("@Required"+kinds[i],Codec.STRING),(e,v)->e.quantities[index]=v,e->e.quantities[index]).add();}
            return builder.build();
        }
        public String action,target,title,description,day,month,year,hour,minute,value,score,feedback,truthTable;
        public final String[] quantities=new String[Kind.values().length];
        LearningDeadline.Parts dateParts(){return new LearningDeadline.Parts(day,month,year,hour,minute);}
    }
}
