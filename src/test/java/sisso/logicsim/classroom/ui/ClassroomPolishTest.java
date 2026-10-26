package sisso.logicsim.classroom.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import sisso.logicsim.classroom.ClassroomUiFixture;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.util.LogicMessages.text;

/** Transport and resource topology only; no native client renderer is emulated here. */
class ClassroomPolishTest {
    @TempDir Path directory;
    private static BsonValue value(UICommandBuilder c,String selector) {
        CustomUICommand command=Arrays.stream(c.getCommands()).filter(v->selector.equals(v.selector)).reduce((a,b)->b).orElseThrow();
        return BsonDocument.parse(command.data).get("0");
    }
    @Test void isolatedFixtureUsesProductionPoliciesAndPaginatesMembersRequestsAndHistoryWithoutOverwriting() throws Exception {
        UUID teacher=UUID.randomUUID(), student=UUID.randomUUID(); Path output=directory.resolve("fixture");
        ClassroomUiFixture.create(output,teacher,student);
        var service=new ClassroomService(new SqliteClassroomRepository(output.resolve("classroom.db")),teacher::equals);
        var first=service.screen(teacher,Kind.MEMBERS,"",0); var second=service.screen(teacher,Kind.MEMBERS,"",1);
        assertEquals(25,first.studentCount()); assertEquals(20,first.rows().items().size()); assertTrue(first.rows().hasNext());
        assertEquals(5,second.rows().items().size()); assertFalse(second.rows().hasNext());
        assertEquals(2048,first.home().classroom().message().length()); assertEquals("00123",service.home(student).member().enrollment());
        assertEquals(20,service.screen(teacher,Kind.INTAKES,"",0).rows().items().size());
        var page=new ClassroomPage(null,null,null); page.model.apply(first,true);
        String selected=ClassroomPageModel.id(first.rows().items().getFirst()).toString(); assertTrue(page.model.select(selected));
        var commands=new UICommandBuilder(); var events=new UIEventBuilder(); page.render(commands,events,true);
        assertEquals(20,events.getEvents().length);
        assertTrue(value(commands,"#Rows[0] #SelectionMark.Visible").asBoolean().getValue());
        assertFalse(value(commands,"#Rows[1] #SelectionMark.Visible").asBoolean().getValue());
        assertTrue(value(commands,"#Previous.Disabled").asBoolean().getValue()); assertFalse(value(commands,"#Next.Disabled").asBoolean().getValue());
        assertTrue(value(commands,"#MetaUuid.TextSpans").toString().contains(((Member)first.rows().items().getFirst()).playerId().toString()));
        page.model.selected=null; page.model.apply(second,true); commands=new UICommandBuilder(); events=new UIEventBuilder(); page.render(commands,events,true);
        assertEquals(5,events.getEvents().length);
        assertFalse(value(commands,"#Previous.Disabled").asBoolean().getValue()); assertTrue(value(commands,"#Next.Disabled").asBoolean().getValue());
        var self=service.home(student).member(); service.end(student,self.id(),self.revision(),false);
        var history=service.screen(student,Kind.INTAKES,"",0); assertTrue(history.rows().hasNext());
        page.model.apply(history,true); assertTrue(page.model.select(ClassroomPageModel.id(history.rows().items().getFirst()).toString()));
        commands=new UICommandBuilder(); page.render(commands,new UIEventBuilder(),true);
        assertTrue(value(commands,"#Rows[0] #RowStamp.Visible").asBoolean().getValue());
        assertTrue(value(commands,"#MetaDate.TextSpans").toString().contains("UTC"));
        assertTrue(value(commands,"#MetaType.Visible").asBoolean().getValue());
        byte[] before=Files.readAllBytes(output.resolve("classroom.db"));
        assertThrows(FileAlreadyExistsException.class,()->ClassroomUiFixture.create(output,teacher,student));
        assertArrayEquals(before,Files.readAllBytes(output.resolve("classroom.db")));
    }
    @Test void emptyContentCollapsesAndStudentStatusDoesNotAskForASelection() {
        UUID teacher=UUID.randomUUID(), student=UUID.randomUUID(), id=UUID.randomUUID();
        var classroom=new Classroom(id,teacher,"Class","",0,"");
        var self=new Member(UUID.randomUUID(),id,student,"Student",Role.STUDENT,"00123",true,1,null,0);
        var page=new ClassroomPage(null,null,null);
        page.model.apply(new Screen(new Home(classroom,self,"Teacher",false),Kind.DETAIL,new Page<>(List.of(),0,false),1),true);
        var c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),true);
        assertFalse(value(c,"#DescriptionDisplay.Visible").asBoolean().getValue()); assertFalse(value(c,"#MessageDisplay.Visible").asBoolean().getValue());
        assertEquals("server.logicgates.classroom.myClassHint",ClassroomScreenRenderer.status(page.model,false).getMessageId());
        assertEquals("server.logicgates.classroom.updated",ClassroomScreenRenderer.status(page.model,true).getMessageId());
    }
    @Test void emptyListsGuideEachSectionAndRoleWithoutOverridingRefreshOrConfirmation() {
        UUID teacher=UUID.randomUUID(), id=UUID.randomUUID(); var classroom=new Classroom(id,teacher,"Class","",0,"");
        var member=new Member(UUID.randomUUID(),id,teacher,"Teacher",Role.TEACHER,null,true,1,null,0);
        var owner=new Home(classroom,member,"Teacher",true); var applicant=new Home(null,null,"",false);
        var screens=List.of(new Screen(owner,Kind.MEMBERS,new Page<>(List.of(),0,false),0),
                new Screen(owner,Kind.INTAKES,new Page<>(List.of(),0,false),0),
                new Screen(owner,Kind.PLAYERS,new Page<>(List.of(),0,false),0),
                new Screen(applicant,Kind.BROWSE,new Page<>(List.of(),0,false),0),
                new Screen(applicant,Kind.INTAKES,new Page<>(List.of(),0,false),0));
        var hints=List.of("emptyMembersHint","emptyPendingHint","emptyPlayersHint","emptyBrowseHint","emptyHistoryHint");
        for(int i=0;i<screens.size();i++) {
            var model=new ClassroomPageModel(); model.apply(screens.get(i),true);
            assertEquals("server.logicgates.classroom."+hints.get(i),ClassroomScreenRenderer.status(model,false).getMessageId());
            assertEquals("server.logicgates.classroom.updated",ClassroomScreenRenderer.status(model,true).getMessageId());
            model.confirming=true;
            for(boolean refreshed:new boolean[]{false,true}) assertEquals("server.logicgates.classroom.confirmHint",ClassroomScreenRenderer.status(model,refreshed).getMessageId());
        }
    }
    @Test void emptyLaterPageOffersPreviousAndRetainedSelectionKeepsItsRecordGuidance() {
        UUID teacher=UUID.randomUUID(), id=UUID.randomUUID(); var classroom=new Classroom(id,teacher,"Class","",0,"");
        var link=new Member(UUID.randomUUID(),id,teacher,"Teacher",Role.TEACHER,null,true,1,null,0);
        var home=new Home(classroom,link,"Teacher",true); var page=new ClassroomPage(null,null,null);
        var empty=new Screen(home,Kind.MEMBERS,new Page<>(List.of(),1,false),20); page.model.apply(empty,true);
        assertEquals("server.logicgates.classroom.emptyPageHint",ClassroomScreenRenderer.status(page.model,false).getMessageId());
        var c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),true);
        assertTrue(value(c,"#Empty.TextSpans").toString().contains("server.logicgates.classroom.emptyList"));
        assertFalse(value(c,"#Previous.Disabled").asBoolean().getValue());
        var student=new Member(UUID.randomUUID(),id,UUID.randomUUID(),"Student",Role.STUDENT,"00123",true,1,null,0);
        page.model.apply(new Screen(home,Kind.MEMBERS,new Page<>(List.of(student),0,false),1),true);
        assertTrue(page.model.select(student.id().toString()));
        assertFalse(page.model.apply(empty,false));
        assertEquals("server.logicgates.classroom.recordHint",ClassroomScreenRenderer.status(page.model,false).getMessageId());
        assertEquals("server.logicgates.classroom.refreshed",ClassroomScreenRenderer.status(page.model,true).getMessageId());
    }
    @Test void explicitOperationFeedbackRemainsAboveTheEmptyListGuidance() {
        var page=new ClassroomPage(null,null,null);
        var screen=new Screen(new Home(null,null,"",false),Kind.BROWSE,new Page<>(List.of(),0,false),0);
        for(var feedback:List.of(text("classroom.done"),text("classroom.saving"),
                ClassroomPage.error(new ClassroomException(ClassroomException.Reason.INVALID_ENROLLMENT)))) {
            var c=new UICommandBuilder();
            assertTrue(page.renderResponse(page.access.current(),screen,true,feedback,c,new UIEventBuilder()));
            assertTrue(value(c,"#Status.TextSpans").toString().contains(feedback.getMessageId()));
        }
    }
    @Test void actionDockAndSaveAreOutsideTheScrollingContentAndEveryRenderedIdExists() throws Exception {
        String layout=resource(ClassroomPage.LAYOUT), row=resource(ClassroomPage.ROW_LAYOUT);
        int scrollStart=layout.indexOf("Group #DetailsScroll {"); int scrollEnd=end(layout,layout.indexOf('{',scrollStart));
        int dock=layout.indexOf("Group #ActionDock {"); assertTrue(dock>scrollEnd);
        for(String id:List.of("#Save {","#Primary {","#Danger {","#CancelConfirm {","#Enrollment {")) assertTrue(layout.indexOf(id)>dock,id);
        // Audit emitted ID/property references; this intentionally does not interpret native UI layout or fonts.
        UUID owner=UUID.randomUUID(), id=UUID.randomUUID(); var classroom=new Classroom(id,owner,"Title","literal\n<b>text</b>",0,"Message");
        var member=new Member(UUID.randomUUID(),id,owner,"Teacher",Role.TEACHER,null,true,1,null,0);
        var page=new ClassroomPage(null,null,null);
        for(Kind kind:Kind.values()) {
            var self=kind==Kind.DETAIL ? new Member(UUID.randomUUID(),id,UUID.randomUUID(),"Student",Role.STUDENT,"00123",true,1,null,0) : member;
            page.model.apply(new Screen(new Home(classroom,self,"Teacher",true),kind,new Page<>(List.of(),0,false),0),true);
            var c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),true);
            for(var command:c.getCommands()) if(command.selector!=null && !command.selector.startsWith("#Rows[")) {
                String base=command.selector.split("\\.",2)[0]; assertTrue(layout.contains(base+" {"),command.selector);
                if(command.type==com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType.Set) {
                    String property=command.selector.substring(command.selector.indexOf('.')+1);
                    assertTrue(Set.of("Text","TextSpans","Value","Visible","Disabled","Anchor").contains(property),property);
                }
            }
        }
        assertTrue(row.contains("#SelectionMark {")); assertTrue(row.contains("#RowStamp {"));
    }
    @Test void classroomClosureIsAvailableOnlyToLinkedTeacherAndConfirmationKeepsDraftFields() {
        UUID teacher=UUID.randomUUID(), student=UUID.randomUUID(), id=UUID.randomUUID();
        var classroom=new Classroom(id,teacher,"Class","Description",4,"Message");
        var teacherLink=new Member(UUID.randomUUID(),id,teacher,"Teacher",Role.TEACHER,null,true,1,null,0);
        var page=new ClassroomPage(null,null,null);
        page.model.apply(new Screen(new Home(classroom,teacherLink,"Teacher",true),Kind.SETTINGS,new Page<>(List.of(),0,false),0),true);
        var c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),true);
        assertTrue(value(c,"#SettingsActions.Visible").asBoolean().getValue()); assertFalse(value(c,"#Actions.Visible").asBoolean().getValue());
        assertTrue(value(c,"#Disband.Visible").asBoolean().getValue());
        assertTrue(value(c,"#Save.Visible").asBoolean().getValue()); assertFalse(value(c,"#CancelConfirm.Visible").asBoolean().getValue());
        page.model.confirming=true; c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),false);
        assertTrue(value(c,"#ConfirmDisband.Visible").asBoolean().getValue()); assertFalse(value(c,"#Disband.Visible").asBoolean().getValue());
        assertTrue(value(c,"#CancelDisband.Visible").asBoolean().getValue()); assertFalse(value(c,"#Save.Visible").asBoolean().getValue());
        assertEquals("server.logicgates.classroom.confirmDisbandHint",ClassroomScreenRenderer.status(page.model,false).getMessageId());
        assertTrue(Arrays.stream(c.getCommands()).noneMatch(command->command.selector!=null && command.selector.endsWith(".Value")));
        // A different classroom or loss of membership can never reuse a previous closure confirmation.
        page.model.apply(new Screen(new Home(null,null,"",true),Kind.SETTINGS,new Page<>(List.of(),0,false),0),false);
        assertFalse(page.model.confirming); assertEquals(0,page.model.editRevision);
        c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),true);
        assertFalse(value(c,"#Danger.Visible").asBoolean().getValue()); assertTrue(value(c,"#Save.Visible").asBoolean().getValue());
        var self=new Member(UUID.randomUUID(),id,student,"Student",Role.STUDENT,"00123",true,1,null,0);
        page.model.apply(new Screen(new Home(classroom,self,"Teacher",false),Kind.DETAIL,new Page<>(List.of(),0,false),1),true);
        c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),true);
        assertTrue(value(c,"#Danger.Text").toString().contains("server.logicgates.classroom.leave"));
    }
    @Test void closureInvalidatesAffectedAdmissionHistoryWithoutRefreshingUnrelatedPages() {
        UUID classroom=UUID.randomUUID(), student=UUID.randomUUID(), teacher=UUID.randomUUID();
        var intake=new Intake(UUID.randomUUID(),classroom,"Class",student,"Student",Origin.INVITE,State.INVITED,null,0,1);
        var model=new ClassroomPageModel(); model.apply(new Screen(new Home(null,null,"",false),Kind.INTAKES,new Page<>(List.of(intake),0,false),0),true);
        var interest=model.interest(student);
        var closure=new sisso.logicsim.classroom.ClassroomRuntime.Change(classroom,teacher,true);
        assertTrue(interest.test(closure)); assertTrue(model.interested(closure,student));
        assertFalse(interest.test(new sisso.logicsim.classroom.ClassroomRuntime.Change(UUID.randomUUID(),teacher,true)));
    }
    @Test void selectedAdmissionBecomesReadOnlyWhenClosureCancelsItDuringEditing() {
        UUID classroom=UUID.randomUUID(), student=UUID.randomUUID();
        var invited=new Intake(UUID.randomUUID(),classroom,"Class",student,"Student",Origin.INVITE,State.INVITED,null,0,1);
        var cancelled=new Intake(invited.id(),classroom,"Class",student,"Student",Origin.INVITE,State.CANCELLED,null,1,1);
        var page=new ClassroomPage(null,null,null); var home=new Home(null,null,"",false);
        page.model.apply(new Screen(home,Kind.INTAKES,new Page<>(List.of(invited),0,false),0),true);
        assertTrue(page.model.select(invited.id().toString()));
        assertFalse(page.model.apply(new Screen(home,Kind.INTAKES,new Page<>(List.of(cancelled),0,false),0),false));
        assertEquals(cancelled,page.model.selected);
        var c=new UICommandBuilder(); page.render(c,new UIEventBuilder(),false);
        assertFalse(value(c,"#Primary.Visible").asBoolean().getValue());
        assertFalse(value(c,"#Secondary.Visible").asBoolean().getValue());
        assertFalse(value(c,"#EnrollmentForm.Visible").asBoolean().getValue());
        assertTrue(value(c,"#MetaState.TextSpans").toString().contains("server.logicgates.classroom.state.cancelled"));
        assertTrue(Arrays.stream(c.getCommands()).noneMatch(command->command.selector!=null && command.selector.endsWith(".Value")));
    }
    private static String resource(String path) throws Exception {
        try(var stream=ClassroomPolishTest.class.getResourceAsStream("/Common/UI/Custom/"+path)) { assertNotNull(stream); return new String(stream.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8); }
    }
    private static int end(String content,int open) {
        int depth=0;
        for(int i=open;i<content.length();i++) {
            if(content.charAt(i)=='{') depth++;
            else if(content.charAt(i)=='}' && --depth==0) return i;
        }
        throw new AssertionError("Unbalanced UI group");
    }
}
