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
        assertEquals(Set.of("Action","@Title","@Description","@Deadline","@Value"),fields.keySet());
        var grade=Arrays.stream(events.getEvents()).filter(e->e.selector.equals("#SaveGrade")).findFirst().orElseThrow();assertEquals(Set.of("Action","@Score","@Feedback"),BsonDocument.parse(grade.data).keySet());
    }
    @Test void codecAcceptsLiteralFieldsButNoClaimedStudentPublicationOrGradeRevision() throws Exception {
        var event=LearningPage.Event.CODEC.decodeJson(new RawJsonReader("{\"Action\":\"Publish\",\"@Title\":\"%server.fake\",\"@Description\":\"literal\\ntext\",\"Publication\":999,\"Student\":\"forged\"}".toCharArray()),new ExtraInfo());
        assertEquals("%server.fake",event.title);assertEquals("literal\ntext",event.description);assertNull(event.target);
        for(var code:LearningException.Code.values())assertEquals("server.logicgates.learning.error."+code,LearningPage.error(new LearningException(code)).getMessageId());
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
            var page=page(scenario.teacher,scenario.students,scenario.activity,scenario.submission);
            for(boolean reset:new boolean[]{true,false}) {
                var commands=new UICommandBuilder();page.render(commands,new UIEventBuilder(),reset,null,false);
                assertWidgetProperties(commands);
                if(!reset)assertTrue(Arrays.stream(commands.getCommands()).noneMatch(c->c.selector!=null && c.selector.endsWith(".Value")),"Background refresh must preserve drafts: "+scenario);
                if(reset && scenario.teacher && !scenario.students && scenario.activity!=null)
                    assertTrue(Arrays.stream(commands.getCommands()).anyMatch(c->"#TitleField.Value".equals(c.selector)));
                if(reset && scenario.teacher && scenario.submission!=null) {
                    var score=Arrays.stream(commands.getCommands()).filter(c->"#ScoreField.Value".equals(c.selector)).findFirst().orElseThrow();
                    assertEquals("7",BsonDocument.parse(score.data).getString("0").getValue());
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
            var target=Pattern.compile("#(\\w+)\\.(\\w+)$").matcher(command.selector);
            assertTrue(target.find(),command.selector);var type=types.get(target.group(1));assertNotNull(type,command.selector);
            switch(target.group(2)) {
                case "Disabled"->assertTrue(type.endsWith("Button"),"Disabled is not a TextField/MultilineTextField property: "+command.selector);
                case "Value"->assertTrue(Set.of("TextField","MultilineTextField").contains(type),command.selector);
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
