package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.ui.builder.*;
import org.junit.jupiter.api.Test;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomViews.*;
import sisso.logicsim.classroom.ClassroomRuntime.Change;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ClassroomPageUpdatesTest {
    private static CustomPage packet(ClassroomPageUpdates updates,UICommandBuilder c,UIEventBuilder e,List<?> rows) {
        return updates.packet("Classroom",CustomPageLifetime.CanDismiss,c,e,rows);
    }
    private static UICommandBuilder rows() {
        return new UICommandBuilder().clear("#Rows").append("#Rows",ClassroomPage.ROW_LAYOUT)
                .set("#Rows[0] #RowTitle.Text","Student").set("#Rows[0] #SelectionMark.Visible",false);
    }
    private static UIEventBuilder bindings() {
        return new UIEventBuilder().addEventBinding(CustomUIEventBindingType.Activating,"#Rows[0]",EventData.of("Action","Select"));
    }
    @Test void unchangedRowsKeepNativeElementsAndBindingsAndStillProduceAnAcknowledgementPacket() {
        var updates=new ClassroomPageUpdates();
        var first=packet(updates,rows(),bindings(),List.of("one")); assertEquals(4,first.commands.length); assertEquals(1,first.eventBindings.length);
        var repeated=packet(updates,rows(),bindings(),List.of("one"));
        assertEquals(0,repeated.commands.length); assertEquals(0,repeated.eventBindings.length); assertFalse(repeated.clear);
        var selection=packet(updates,rows().set("#Rows[0] #SelectionMark.Visible",true),bindings(),List.of("one"));
        assertEquals(1,selection.commands.length); assertEquals("#Rows[0] #SelectionMark.Visible",selection.commands[0].selector);
        assertEquals(0,selection.eventBindings.length);
    }
    @Test void rebuiltRowsReceiveEveryPropertyEvenWhenTheirTextMatchesThePreviousRows() {
        var updates=new ClassroomPageUpdates(); packet(updates,rows(),bindings(),List.of("old UUID"));
        var rebuilt=packet(updates,rows(),bindings(),List.of("new UUID"));
        assertEquals(4,rebuilt.commands.length); assertEquals(1,rebuilt.eventBindings.length);
        packet(updates,new UICommandBuilder().clear("#Rows"),new UIEventBuilder(),null);
        assertEquals(4,packet(updates,rows(),bindings(),List.of("new UUID")).commands.length);
    }
    @Test void fieldValuesAlwaysReachTheClientForReloadAndRevocationEvenIfTheServerValueMatches() {
        var updates=new ClassroomPageUpdates();
        for(int i=0;i<3;i++) {
            var result=packet(updates,new UICommandBuilder().set("#Name.Value","").set("#NoAccess.Visible",true),new UIEventBuilder(),null);
            assertTrue(Arrays.stream(result.commands).anyMatch(c->"#Name.Value".equals(c.selector)));
            assertEquals(i==0?2:1,result.commands.length);
        }
    }
    @Test void intermediateVisibilityResetsAreCollapsedBeforeComparingTheFinalServerState() {
        var updates=new ClassroomPageUpdates();
        packet(updates,new UICommandBuilder().set("#Primary.Visible",true),new UIEventBuilder(),null);
        var result=packet(updates,new UICommandBuilder().set("#Primary.Visible",false).set("#Primary.Visible",true),new UIEventBuilder(),null);
        assertEquals(0,result.commands.length);
    }
    @Test void notificationInterestUsesAnImmutableContextWhileTheWorldThreadNavigates() {
        UUID actor=UUID.randomUUID(), id=UUID.randomUUID(); var classroom=new Classroom(id,actor,"Class","",0,"");
        var owner=new Member(UUID.randomUUID(),id,actor,"Teacher",Role.TEACHER,null,true,1,null,0);
        var model=new ClassroomPageModel(); model.apply(new Screen(new Home(classroom,owner,"Teacher",true),Kind.MEMBERS,new Page<>(List.of(),0,false),0),true);
        var interest=model.interest(actor); var unrelated=new Change(UUID.randomUUID(),UUID.randomUUID(),true);
        assertFalse(interest.test(unrelated)); model.navigate(Kind.BROWSE);
        assertTrue(model.interested(unrelated,actor)); assertFalse(interest.test(unrelated));
        assertTrue(interest.test(new Change(id,UUID.randomUUID(),false)));
        assertTrue(interest.test(new Change(null,actor,false)));
    }
    @Test void rebuildingSecondaryRowsRestoresTheirPropertiesWithoutResettingRosterOrPreview() {
        var updates=new ClassroomPageUpdates();packet(updates,rows().set("#Preview.Visible",true),bindings(),List.of("one"));
        for(int i=0;i<2;i++) {
            var commands=rows().set("#Preview.Visible",true).clear("#TruthRows").append("#TruthRows","Pages/LogicGates/TruthRow.ui")
                    .set("#TruthRows[0] #InputBits.Text","00");
            var result=packet(updates,commands,bindings(),List.of("one"));
            assertEquals(3,result.commands.length);assertEquals(0,result.eventBindings.length);
            assertTrue(Arrays.stream(result.commands).noneMatch(c->c.selector.startsWith("#Rows") || c.selector.equals("#Preview.Visible")));
        }
    }
}
