package sisso.logicsim.classroom.ui;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.server.core.ui.builder.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import sisso.logicsim.classroom.domain.ClassroomViews.Page;
import sisso.logicsim.classroom.learning.*;
import sisso.logicsim.classroom.learning.LearningViews.*;
import static org.junit.jupiter.api.Assertions.*;

/** Transport/codec regression, not a substitute for testing the native client. */
class LearningPageTest {
    private static final UUID CLASSROOM=UUID.randomUUID(),ACTIVITY=UUID.randomUUID(),STUDENT=UUID.randomUUID();

    @Test void everyBoundControlExistsAndNoPartialAnchorOrClientAuthorityIsSent() throws Exception {
        var page=new LearningPage(null,null,null,null,null);var commands=new UICommandBuilder();var events=new UIEventBuilder();page.build(null,commands,events,null);
        try(var resource=getClass().getResourceAsStream("/Common/UI/Custom/"+LearningPage.LAYOUT)) {
            assertNotNull(resource);var layout=new String(resource.readAllBytes(),StandardCharsets.UTF_8);
            for(var binding:events.getEvents())assertTrue(layout.contains(binding.selector+" {"),binding.selector);
            assertTrue(layout.contains("PrefabPreviewComponent #PrefabPreview"));assertFalse(layout.contains("BuilderTool:"));
        }
        assertTrue(Arrays.stream(commands.getCommands()).noneMatch(c->c.selector!=null && c.selector.contains(".Anchor.")));
        assertWidgetProperties(commands);
        var publish=Arrays.stream(events.getEvents()).filter(e->e.selector.equals("#Publish")).findFirst().orElseThrow();var fields=BsonDocument.parse(publish.data);
        var expected=new HashSet<>(Set.of("Action","@Title","@Description","@Day","@Month","@Year","@Hour","@Minute","@Value","@TruthTable"));
        for(var kind:sisso.logicsim.circuit.CircuitNode.Kind.values())expected.add("@Required"+kind);
        assertEquals(expected,fields.keySet());
        var grade=Arrays.stream(events.getEvents()).filter(e->e.selector.equals("#SaveGrade")).findFirst().orElseThrow();assertEquals(Set.of("Action","@Score","@Feedback"),BsonDocument.parse(grade.data).keySet());
    }
    @Test void codecAcceptsLiteralFieldsButNoClaimedStudentPublicationOrGradeRevision() throws Exception {
        var event=LearningPage.Event.CODEC.decodeJson(new RawJsonReader("{\"Action\":\"Publish\",\"@Title\":\"%server.fake\",\"@Description\":\"literal\\ntext\",\"Publication\":999,\"Student\":\"forged\"}".toCharArray()),new ExtraInfo());
        assertEquals("%server.fake",event.title);assertEquals("literal\ntext",event.description);assertNull(event.target);
        for(var code:LearningException.Code.values())assertEquals("server.logicgates.learning.error."+code,LearningPage.error(new LearningException(code)).getMessageId());
    }
    @Test void verificationFieldsRoundTripAndOnlyTeacherReviewOffersAssistanceWithoutChangingManualGrade() throws Exception {
        var event=LearningPage.Event.CODEC.decodeJson(new RawJsonReader("{\"Action\":\"Publish\",\"@RequiredAND\":\"0\",\"@RequiredLEVER\":\"2\",\"@TruthTable\":\"00 -> 0\\n01 -> 0\\n10 -> 0\\n11 -> 1\"}".toCharArray()),new ExtraInfo());
        var rules=LearningVerification.rules(event.quantities,event.truthTable);
        assertEquals(0,rules.quantities().get(sisso.logicsim.circuit.CircuitNode.Kind.AND));assertEquals(4,rules.cases().size());
        var published=new Publication(ACTIVITY,1,"Title","Statement",Long.MAX_VALUE,1000,1,rules);
        var activity=new Activity(ACTIVITY,CLASSROOM,2,false,null,published);
        var submission=new Submission(UUID.randomUUID(),ACTIVITY,1,STUDENT,UUID.randomUUID(),1,2,false,UUID.randomUUID(),"hash",10,3,1,3,1000,new Assessment(1,700,"Keep feedback",3));
        for(boolean teacher:new boolean[]{false,true}){
            var page=page(teacher,teacher,activity,submission);var c=new UICommandBuilder();page.render(c,new UIEventBuilder(),true,null,false);assertWidgetProperties(c);
            assertEquals(teacher,value(c,"#Verify.Visible").asBoolean().getValue());
            if(teacher){
                var report=new SubmissionVerifier().verify(rules,Map.of());set(page,"verificationId",submission.id());set(page,"verificationReport",report);
                c=new UICommandBuilder();page.render(c,new UIEventBuilder(),false,null,false);assertWidgetProperties(c);
                assertTrue(value(c,"#VerificationResults.Visible").asBoolean().getValue());
                assertTrue(Arrays.stream(c.getCommands()).noneMatch(x->x.selector!=null && x.selector.endsWith(".Value")));
                assertFalse(value(c,"#ReadTable.Visible").asBoolean().getValue());
                assertTrue(value(c,"#SubmissionRules.TextSpans").toString().contains("11 -> 1"));
                set(page,"verificationId",UUID.randomUUID());c=new UICommandBuilder();page.render(c,new UIEventBuilder(),false,null,false);
                assertFalse(value(c,"#VerificationResults.Visible").asBoolean().getValue());
            }
        }
    }

    @Test void renderedTeacherStudentArchivedAndAssessmentUpdatesUseCompatibleWidgetProperties() throws Exception {
        var publication=new Publication(ACTIVITY,1,"Circuito","Enunciado",System.currentTimeMillis()+86_400_000,1000,1);
        var draft=new Activity(ACTIVITY,CLASSROOM,1,false,Draft.empty(),null);
        var published=new Activity(ACTIVITY,CLASSROOM,2,false,null,publication);
        var archived=new Activity(ACTIVITY,CLASSROOM,3,true,Draft.empty(),publication);
        var submission=new Submission(UUID.randomUUID(),ACTIVITY,1,STUDENT,UUID.randomUUID(),1,2,false,
                UUID.randomUUID(),"checksum",200,3,2,4,1000,new Assessment(1,700,"Feedback",3));
        record Scenario(boolean teacher,boolean students,Activity activity,Submission submission) { }
        for(var scenario:List.of(new Scenario(true,false,null,null),new Scenario(true,false,draft,null),
                new Scenario(true,false,published,null),new Scenario(true,false,archived,null),
                new Scenario(true,true,published,null),new Scenario(true,true,published,submission),
                new Scenario(false,false,published,null),new Scenario(false,false,published,submission))) {
            for(boolean editing:new boolean[]{false,true}) {
            var page=page(scenario.teacher,scenario.students,scenario.activity,scenario.submission);
            if(editing && !page.beginEditing())continue;
            for(boolean reset:new boolean[]{true,false}) {
                var commands=new UICommandBuilder();page.render(commands,new UIEventBuilder(),reset,null,false);
                assertWidgetProperties(commands);
                if(!reset)assertTrue(Arrays.stream(commands.getCommands()).noneMatch(c->c.selector!=null && c.selector.endsWith(".Value")),"Background refresh must preserve drafts: "+scenario);
                if(reset && editing)
                    assertTrue(Arrays.stream(commands.getCommands()).anyMatch(c->"#TitleField.Value".equals(c.selector)));
                if(reset && scenario.teacher && scenario.submission!=null) {
                    var score=Arrays.stream(commands.getCommands()).filter(c->"#ScoreField.Value".equals(c.selector)).findFirst().orElseThrow();
                    assertEquals("7",BsonDocument.parse(score.data).getString("0").getValue());
                }
            }
            }
        }
    }

    @Test void savingAndEveryFailureBlockOnlyActionsAndPreserveInputValues() throws Exception {
        var waiting=LearningPage.operationState(true,sisso.logicsim.util.LogicMessages.text("classroom.saving"));
        assertOperationState(waiting,true);
        for(var code:LearningException.Code.values())
            assertOperationState(LearningPage.operationState(false,LearningPage.error(new LearningException(code))),false);
    }

    @Test void creationStaysWideAfterSaveAndReturnsToTheListAfterPublishOrBack() throws Exception {
        var activity=new Activity(ACTIVITY,CLASSROOM,1,false,Draft.empty(),null);
        var page=page(true,false,activity,null);page.created(activity);
        for(boolean saved:new boolean[]{false,true}) {
            if(saved)page.saved(false);
            var c=new UICommandBuilder();page.render(c,new UIEventBuilder(),!saved,null,true);
            assertWidgetProperties(c);
            for(String id:List.of("ListPanel","New","Filter","Archive","Students"))assertFalse(value(c,"#"+id+".Visible").asBoolean().getValue(),id);
            assertTrue(value(c,"#Discard.Visible").asBoolean().getValue());
            assertEquals(260,value(c,"#DescriptionField.Anchor").asDocument().getInt32("Height").getValue());
            assertTrue(value(c,"#Activities.TextSpans").toString().contains("backToActivities"));
            if(saved)assertTrue(Arrays.stream(c.getCommands()).noneMatch(x->x.selector!=null && x.selector.endsWith(".Value")));
        }
        page.saved(true);var c=new UICommandBuilder();page.render(c,new UIEventBuilder(),true,null,false);
        assertTrue(value(c,"#ListPanel.Visible").asBoolean().getValue());assertTrue(value(c,"#Archive.Visible").asBoolean().getValue());
        page.created(activity);page.backToActivities();set(page,"activity",null);
        c=new UICommandBuilder();page.render(c,new UIEventBuilder(),true,null,false);
        assertTrue(value(c,"#ListPanel.Visible").asBoolean().getValue());assertFalse(value(c,"#Editor.Visible").asBoolean().getValue());
    }

    @Test void selectingAnActivityOnlyViewsItAndEditingRequiresAnExplicitTeacherAction() throws Exception {
        var publication=new Publication(ACTIVITY,1,"Saved title","Saved statement",System.currentTimeMillis()+86_400_000,1000,1);
        var activity=new Activity(ACTIVITY,CLASSROOM,4,false,null,publication);var page=page(true,false,activity,null);
        assertTrue(page.selectActivity(ACTIVITY));assertFalse(page.editingActivity());
        var c=new UICommandBuilder();page.render(c,new UIEventBuilder(),true,null,false);
        assertTrue(value(c,"#Read.Visible").asBoolean().getValue());assertFalse(value(c,"#Editor.Visible").asBoolean().getValue());
        assertTrue(value(c,"#Edit.Visible").asBoolean().getValue());assertFalse(value(c,"#ActivityActions.Visible").asBoolean().getValue());
        assertTrue(value(c,"#ReadTitle.TextSpans").toString().contains("Saved title"));
        assertTrue(Arrays.stream(c.getCommands()).noneMatch(x->x.selector!=null && x.selector.endsWith(".Value")));
        assertTrue(page.beginEditing());assertFalse(page.beginEditing());
        c=new UICommandBuilder();page.render(c,new UIEventBuilder(),true,null,false);assertWidgetProperties(c);
        assertTrue(value(c,"#Editor.Visible").asBoolean().getValue());assertFalse(value(c,"#Read.Visible").asBoolean().getValue());
        assertFalse(value(c,"#Edit.Visible").asBoolean().getValue());assertTrue(value(c,"#CancelEdit.Visible").asBoolean().getValue());
        assertEquals("Saved title",value(c,"#TitleField.Value").asString().getValue());
        set(page,"activity",new Activity(ACTIVITY,CLASSROOM,5,false,new Draft("Remote edit","Remote statement",publication.deadline(),1000L),publication));
        c=new UICommandBuilder();page.render(c,new UIEventBuilder(),false,null,false);
        assertTrue(page.editingActivity());assertTrue(Arrays.stream(c.getCommands()).noneMatch(x->x.selector!=null && x.selector.endsWith(".Value")));
        var revision=LearningPage.class.getDeclaredField("editRevision");revision.setAccessible(true);assertEquals(4,revision.getLong(page));
        assertTrue(page.viewActivity());assertFalse(page.viewActivity());
        c=new UICommandBuilder();page.render(c,new UIEventBuilder(),false,null,false);
        assertTrue(value(c,"#ReadTitle.TextSpans").toString().contains("Remote edit"));
        assertTrue(page.beginEditing());assertTrue(page.selectActivity(ACTIVITY));assertFalse(page.editingActivity());
    }

    @Test void archivedStudentAndStudentListContextsNeverOfferOrEnterActivityEditing() throws Exception {
        var publication=new Publication(ACTIVITY,1,"Title","Statement",System.currentTimeMillis()+86_400_000,1000,1);
        for(var page:List.of(page(false,false,new Activity(ACTIVITY,CLASSROOM,1,false,null,publication),null),
                page(true,true,new Activity(ACTIVITY,CLASSROOM,1,false,null,publication),null),
                page(true,false,new Activity(ACTIVITY,CLASSROOM,1,true,Draft.empty(),publication),null),page(true,false,null,null))) {
            assertFalse(page.beginEditing());assertFalse(page.editingActivity());
            var c=new UICommandBuilder();page.render(c,new UIEventBuilder(),true,null,false);assertWidgetProperties(c);
            assertFalse(value(c,"#Edit.Visible").asBoolean().getValue());assertFalse(value(c,"#Editor.Visible").asBoolean().getValue());
            assertFalse(value(c,"#CancelEdit.Visible").asBoolean().getValue());
        }
    }

    @Test void teachersCanReadSavedPrivateDraftsButStudentsOnlySeeThePublishedVersion() throws Exception {
        var publication=new Publication(ACTIVITY,3,"Published title","Published statement",System.currentTimeMillis()+86_400_000,1000,1);
        var activity=new Activity(ACTIVITY,CLASSROOM,4,false,new Draft("Private title","Private statement",null,null),publication);
        var teacher=page(true,false,activity,null);var c=new UICommandBuilder();teacher.render(c,new UIEventBuilder(),true,null,false);
        assertTrue(value(c,"#ReadTitle.TextSpans").toString().contains("Private title"));
        assertTrue(value(c,"#ReadDescription.TextSpans").toString().contains("Private statement"));
        assertTrue(value(c,"#DraftNotice.Visible").asBoolean().getValue());assertTrue(value(c,"#ReadMeta.TextSpans").toString().contains("notSet"));
        assertTrue(value(c,"#ReadState.TextSpans").toString().contains("learning.draft"));
        var student=page(false,false,activity,null);c=new UICommandBuilder();student.render(c,new UIEventBuilder(),true,null,false);
        assertTrue(value(c,"#ReadTitle.TextSpans").toString().contains("Published title"));
        assertTrue(value(c,"#ReadDescription.TextSpans").toString().contains("Published statement"));
        assertFalse(value(c,"#DraftNotice.Visible").asBoolean().getValue());
        assertTrue(Arrays.stream(c.getCommands()).noneMatch(x->x.data!=null && (x.data.contains("Private title") || x.data.contains("Private statement"))));
    }

    private static org.bson.BsonValue value(UICommandBuilder c,String selector) {
        return BsonDocument.parse(Arrays.stream(c.getCommands()).filter(x->selector.equals(x.selector)).findFirst().orElseThrow().data).get("0");
    }

    private static void assertOperationState(UICommandBuilder commands,boolean busy) throws Exception {
        assertWidgetProperties(commands);
        assertTrue(Arrays.stream(commands.getCommands()).noneMatch(c->c.selector!=null && c.selector.endsWith(".Value")));
        var disabled=Arrays.stream(commands.getCommands()).filter(c->c.selector!=null && c.selector.endsWith(".Disabled")).toList();
        assertFalse(disabled.isEmpty());
        for(var command:disabled)assertEquals(busy,BsonDocument.parse(command.data).getBoolean("0").getValue(),command.selector);
        assertTrue(Arrays.stream(commands.getCommands()).anyMatch(c->"#Status.TextSpans".equals(c.selector)));
    }

    /** Check actual rendered commands against the target widget in the packaged layouts. */
    private static void assertWidgetProperties(UICommandBuilder commands) throws Exception {
        var types=new HashMap<String,String>();
        for(String layout:List.of(LearningPage.LAYOUT,ClassroomPage.ROW_LAYOUT))try(var input=LearningPageTest.class.getResourceAsStream("/Common/UI/Custom/"+layout)) {
            assertNotNull(input);var matcher=Pattern.compile("(?:\\$C\\.@)?(\\w+)\\s+#(\\w+)\\s*\\{").matcher(new String(input.readAllBytes(),StandardCharsets.UTF_8));
            while(matcher.find())types.put(matcher.group(2),matcher.group(1));
        }
        for(var command:commands.getCommands()) {
            if(command.selector==null || !command.selector.contains("."))continue;
            assertFalse(command.selector.contains(".Anchor."),command.selector);
            if(command.selector.matches("#Rows\\[\\d+\\]\\.Anchor")) {
                assertEquals(Set.of("Height","Bottom"),BsonDocument.parse(command.data).getDocument("0").keySet());continue;
            }
            var target=Pattern.compile("#(\\w+)\\.(\\w+)$").matcher(command.selector);
            assertTrue(target.find(),command.selector);var type=types.get(target.group(1));assertNotNull(type,command.selector);
            switch(target.group(2)) {
                case "Disabled"->assertTrue(type.endsWith("Button"),"Disabled is not a TextField/MultilineTextField property: "+command.selector);
                case "Value"->assertTrue(Set.of("TextField","MultilineTextField","DropdownBox").contains(type),command.selector);
                case "Entries"->assertEquals("DropdownBox",type,command.selector);
                case "TextSpans"->assertTrue(type.equals("Label") || type.endsWith("Button"),command.selector);
                case "Visible","Anchor"->{ }
                default->fail("Unreviewed native widget property: "+command.selector);
            }
        }
    }

    private static LearningPage page(boolean teacher,boolean students,Activity activity,Submission submission) throws Exception {
        var page=new LearningPage(null,null,null,null,null);
        set(page,"context",new Context(CLASSROOM,UUID.randomUUID(),teacher,"Turma"));
        set(page,"activity",activity);set(page,"activityId",activity==null?null:activity.id());
        set(page,"students",students);set(page,"studentId",students?STUDENT:null);
        set(page,"rows",new Page<>(students?List.of(new Student(STUDENT,"Aluno","00123",true,submission)):activity==null?List.of():List.of(activity),0,false));
        set(page,"submission",submission);set(page,"submissionId",submission==null?null:submission.id());
        set(page,"submittedVersion",activity==null?null:activity.published());
        set(page,"revisions",new Page<>(submission==null?List.of():List.of(submission),0,false));
        return page;
    }
    private static void set(LearningPage page,String name,Object value) throws Exception {
        var field=LearningPage.class.getDeclaredField(name);field.setAccessible(true);field.set(page,value);
    }
}
