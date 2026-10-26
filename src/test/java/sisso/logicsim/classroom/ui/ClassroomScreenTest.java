package sisso.logicsim.classroom.ui;

import org.junit.jupiter.api.Test;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import org.bson.BsonDocument;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Transport/model regressions; these tests do not render a Hytale client. */
class ClassroomScreenTest {
    final UUID teacher=UUID.randomUUID(), student=UUID.randomUUID(), id=UUID.randomUUID();
    final Classroom classroom=new Classroom(id,teacher,"Turma %server.fake","<b>literal</b>",3,"Olá\n00123");
    final Member teacherLink=member(teacher,Role.TEACHER,null,0);
    Member member(UUID player,Role role,String enrollment,long revision) {
        return new Member(UUID.randomUUID(),id,player,"Nome",role,enrollment,true,1770000000000L,null,revision);
    }
    Home teacherHome(Classroom c) { return new Home(c,teacherLink,"Professor",true); }
    Screen screen(Home home,Kind kind,Object...rows) { return new Screen(home,kind,new Page<>(List.of(rows),0,false),rows.length); }
    @Test void allScreensSendWholeAnchorObjectsWithTheNativeHeightAndMargins() {
        var longClassroom=new Classroom(id,teacher,"T".repeat(64),"D".repeat(90)+"\nSegunda linha",3,"M".repeat(60)+"\n00123");
        var pupil=member(student,Role.STUDENT,"00123",0);
        var applicant=new Home(null,null,"",true);
        var request=new Intake(UUID.randomUUID(),id,longClassroom.name(),student,"Aluno",Origin.REQUEST,State.REQUESTED,"00123",0,1770000000000L);
        var invite=new Intake(UUID.randomUUID(),id,longClassroom.name(),student,"Aluno",Origin.INVITE,State.INVITED,null,0,1770000000000L);
        var screens=List.of(
                screen(teacherHome(longClassroom),Kind.MEMBERS),
                screen(teacherHome(longClassroom),Kind.MEMBERS,pupil),
                screen(teacherHome(longClassroom),Kind.INTAKES,request),
                screen(teacherHome(longClassroom),Kind.INTAKES,invite),
                screen(teacherHome(longClassroom),Kind.PLAYERS,new Profile(student,"Aluno")),
                screen(teacherHome(longClassroom),Kind.SETTINGS),
                screen(new Home(longClassroom,pupil,"Professor",false),Kind.DETAIL),
                screen(applicant,Kind.BROWSE),
                screen(applicant,Kind.BROWSE,new Summary(id,longClassroom.name(),longClassroom.description(),teacher,"Professor")),
                screen(applicant,Kind.INTAKES,invite),
                screen(applicant,Kind.INTAKES,request),
                screen(applicant,Kind.SETTINGS));
        for (var screen:screens) for (boolean reset:new boolean[]{true,false}) {
            var page=new ClassroomPage(null,null,null);
            page.model.apply(screen,true);
            if (!screen.rows().items().isEmpty()) assertTrue(page.model.select(ClassroomPageModel.id(screen.rows().items().getFirst()).toString()));
            var commands=new UICommandBuilder(); page.render(commands,new UIEventBuilder(),reset);
            var anchors=new HashMap<String,BsonDocument>();
            for (var command:commands.getCommands()) {
                assertFalse(command.selector!=null && command.selector.contains(".Anchor."),
                        "Nested Anchor selectors disconnect the 0.7.4 client: "+command.selector);
                if (command.selector!=null && command.selector.endsWith(".Anchor")) {
                    assertEquals(CustomUICommandType.Set,command.type);
                    var anchor=BsonDocument.parse(command.data).getDocument("0");
                    assertEquals(Set.of("Height","Bottom"),anchor.keySet());
                    assertTrue(anchor.getInt32("Height").getValue()>0);
                    anchors.put(command.selector,anchor);
                }
            }
            for(String label:List.of("HeaderTitle","HeaderInfo","DetailTitle","DescriptionText","MessageText"))
                assertFalse(anchors.containsKey("#"+label+".Anchor"),"Wrapped labels must measure their own height: "+label);
            for (int i=0;i<screen.rows().items().size();i++) assertEquals(6,anchors.get("#Rows["+i+"].Anchor").getInt32("Bottom").getValue());
        }
    }
    @Test void selectionUsesStableMembershipUuidAndRefreshKeepsThePresentedRevision() {
        var model=new ClassroomPageModel(); var a=member(student,Role.STUDENT,"00123",0);
        var b=member(UUID.randomUUID(),Role.STUDENT,"002",0);
        model.apply(screen(teacherHome(classroom),Kind.MEMBERS,a,b),true);
        assertTrue(model.select(a.id().toString()));
        var edited=new Member(a.id(),id,student,"Renamed",Role.STUDENT,"Changed",true,a.joinedAt(),null,1);
        assertFalse(model.apply(screen(teacherHome(classroom),Kind.MEMBERS,b,edited),false));
        assertEquals(a,model.selected); // A subsequent save must detect STALE, not overwrite a remote correction.
        assertFalse(model.select("not-a-uuid")); assertFalse(model.select(UUID.randomUUID().toString()));
        assertEquals(a,model.selected);
        model.apply(screen(teacherHome(classroom),Kind.MEMBERS,edited),true);
        assertEquals(1,((Member)model.selected).revision());
        model.confirming=true;
        model.apply(screen(teacherHome(classroom),Kind.MEMBERS),false);
        assertEquals(edited,model.selected); assertFalse(model.confirming);
    }
    @Test void automaticSettingsRefreshPreservesDraftFieldsAndOriginalEditRevision() {
        var page=new ClassroomPage(null,null,null);
        page.model.kind=Kind.SETTINGS;
        page.model.apply(screen(teacherHome(classroom),Kind.SETTINGS),true);
        var remote=new Classroom(id,teacher,"Other","Other",4,"Other");
        assertFalse(page.model.apply(screen(teacherHome(remote),Kind.SETTINGS),false));
        assertEquals(3,page.model.editRevision);
        var commands=new UICommandBuilder(); page.render(commands,new UIEventBuilder(),false);
        assertTrue(Arrays.stream(commands.getCommands()).noneMatch(c->c.selector!=null && c.selector.endsWith(".Value")));
        page.model.apply(screen(teacherHome(remote),Kind.SETTINGS),true);
        assertEquals(4,page.model.editRevision);
    }
    @Test void dynamicBindingsCarryOnlyActionAndStableTargetAndAreRecreatedWithRows() {
        var page=new ClassroomPage(null,null,null); var m=member(student,Role.STUDENT,"00123",0);
        page.model.apply(screen(teacherHome(classroom),Kind.MEMBERS,m),true);
        var commands=new UICommandBuilder(); var events=new UIEventBuilder(); page.render(commands,events,true);
        assertEquals(1,events.getEvents().length);
        var binding=events.getEvents()[0]; assertEquals("#Rows[0]",binding.selector);
        var event=BsonDocument.parse(binding.data);
        assertEquals(Set.of("Action","Target"),event.keySet()); assertEquals(m.id().toString(),event.getString("Target").getValue());
        assertTrue(Arrays.stream(commands.getCommands()).anyMatch(c->c.type==CustomUICommandType.Append && ClassroomPage.ROW_LAYOUT.equals(c.text)));
    }
    @Test void studentScreenHasNoOtherStudentRowsAndPublicSummaryHasNoMessageOrEnrollmentFields() {
        var page=new ClassroomPage(null,null,null); var self=member(student,Role.STUDENT,"00123",0);
        page.model.apply(screen(new Home(classroom,self,"Professor",false),Kind.DETAIL),true);
        var c=new UICommandBuilder(); var e=new UIEventBuilder(); page.render(c,e,true);
        assertEquals(0,e.getEvents().length);
        assertEquals(Set.of("id","name","description","teacherId","teacherName"),
                Arrays.stream(Summary.class.getRecordComponents()).map(java.lang.reflect.RecordComponent::getName).collect(java.util.stream.Collectors.toSet()));
    }
    @Test void aRoleOrClassChangeInvalidatesOldSelectionAndConfirmation() {
        var model=new ClassroomPageModel(); var m=member(student,Role.STUDENT,"00123",0);
        model.apply(screen(teacherHome(classroom),Kind.MEMBERS,m),true); model.select(m.id().toString()); model.confirming=true;
        assertTrue(model.apply(screen(new Home(null,null,"",false),Kind.BROWSE),false));
        assertNull(model.selected); assertFalse(model.confirming);
    }
    @Test void reentryInTheSameClassRequiresANewLeaveConfirmation() {
        var model=new ClassroomPageModel();
        model.apply(screen(new Home(classroom,member(student,Role.STUDENT,"00123",0),"Professor",false),Kind.DETAIL),true);
        model.confirming=true;
        assertTrue(model.apply(screen(new Home(classroom,member(student,Role.STUDENT,"00123",0),"Professor",false),Kind.DETAIL),false));
        assertFalse(model.confirming);
    }
    @Test void approvalInAnotherClassRefreshesPendingListsAndKnownStudentRenamesRefreshMemberLists() {
        var model=new ClassroomPageModel();
        model.apply(screen(teacherHome(classroom),Kind.INTAKES),true);
        var admittedElsewhere=new sisso.logicsim.classroom.ClassroomRuntime.Change(UUID.randomUUID(),student,true);
        assertTrue(model.interested(admittedElsewhere,teacher));
        model.navigate(Kind.MEMBERS);
        model.apply(screen(teacherHome(classroom),Kind.MEMBERS,member(student,Role.STUDENT,"00123",0)),true);
        assertTrue(model.interested(new sisso.logicsim.classroom.ClassroomRuntime.Change(null,student,true),teacher));
        assertFalse(model.interested(new sisso.logicsim.classroom.ClassroomRuntime.Change(null,UUID.randomUUID(),true),teacher));
    }
}
