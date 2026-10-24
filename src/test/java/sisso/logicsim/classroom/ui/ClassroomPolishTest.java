package sisso.logicsim.classroom.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommand;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import sisso.logicsim.classroom.ClassroomUiFixture;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomService;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.persistence.SqliteClassroomRepository;
import org.bson.BsonDocument;
import org.bson.BsonValue;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

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
