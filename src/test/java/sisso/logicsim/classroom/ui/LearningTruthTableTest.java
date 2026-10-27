package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.ui.builder.*;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import sisso.logicsim.classroom.learning.LearningViews.Draft;
import sisso.logicsim.classroom.learning.VerificationRules;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class LearningTruthTableTest {
    @Test void incompleteDraftRoundTripsWithoutInventingAnswersAndPublicationStillRejectsIt() {
        var table=new LearningTruthTable();table.reset(3,2);table.cell("000:1","1");table.cell("101:0","0");
        var reopened=new LearningTruthTable();reopened.load(table.text());
        assertEquals(table.text(),reopened.text());assertEquals(0,reopened.completed());assertFalse(reopened.advanced);
        assertThrows(RuntimeException.class,()->new VerificationRules(Map.of(),reopened.text()).cases());
        reopened.andExample();assertEquals(4,reopened.completed());assertEquals(4,new VerificationRules(Map.of(),reopened.text()).cases().size());
    }
    @Test void malformedLegacyTextRemainsExactAndSparseTablesKeepUnknownCombinations() {
        var table=new LearningTruthTable();String raw="old invalid text\n01 -> ?";table.load(raw);
        assertTrue(table.advanced);assertEquals(raw,table.text());
        table.load("00 -> 1\n11 -> 0");assertFalse(table.advanced);assertEquals(2,table.completed());
        assertEquals("00 -> 1\n01 -> ?\n10 -> ?\n11 -> 0",table.text());
        table.load("0 -> 1\n0 -> 0");assertTrue(table.advanced);assertEquals("0 -> 1\n0 -> 0",table.text());
    }
    @Test void largestTableOnlyBuildsEightRowsAndThirtyTwoCellsAndRejectsOffPageEvents() {
        var table=new LearningTruthTable();table.reset(6,4);var commands=new UICommandBuilder();var events=new UIEventBuilder();
        LearningTableRenderer.render(table,commands,events,true);
        assertEquals(8,Arrays.stream(commands.getCommands()).filter(c->c.type==CustomUICommandType.Append && "#TruthRows".equals(c.selector)).count());
        assertEquals(32,events.getEvents().length);
        assertThrows(IllegalArgumentException.class,()->table.cell("111111:0","1"));
        table.page=7;table.structure=true;commands=new UICommandBuilder();events=new UIEventBuilder();LearningTableRenderer.render(table,commands,events,true);
        assertEquals(32,events.getEvents().length);assertTrue(events.getEvents()[31].data.contains("111111:3"));
        table.cell("111111:3","1");assertEquals('1',table.cell(63,3));
        assertThrows(IllegalArgumentException.class,()->table.cell("000000:0","1"));
        assertThrows(IllegalArgumentException.class,()->table.cell("111111:0","2"));
        commands=new UICommandBuilder();events=new UIEventBuilder();LearningTableRenderer.render(table,commands,events,true);
        assertEquals(0,events.getEvents().length); // Ordinary refresh preserves native controls and their open state.
    }
    @Test void nativeCellBindingsContainOnlyStableTargetAndValueAndHiddenSectionsDoNotBuildCells() {
        var table=new LearningTruthTable();table.andExample();var c=new UICommandBuilder();var e=new UIEventBuilder();
        LearningTableRenderer.render(table,c,e,false);assertEquals(0,e.getEvents().length);assertTrue(table.structure);
        LearningTableRenderer.render(table,c,e,true);
        for(var binding:e.getEvents())assertEquals(Set.of("Action","Target","@Cell"),BsonDocument.parse(binding.data).keySet());
    }
    @Test void leaveSnapshotPreservesExactUnsavedFieldsAndDoesNotCreateAnActivity() {
        var snapshot=LearningEditorSnapshot.from(Draft.empty(),"");var event=snapshot.event("Close");
        assertEquals(snapshot,LearningEditorSnapshot.from(event));event.title="  title  ";event.quantities[0]="0";event.truthTable="0 -> ?";
        assertNotEquals(snapshot,LearningEditorSnapshot.from(event));
        var save=LearningEditorSnapshot.from(event).event("SaveDraft");assertEquals("  title  ",save.title);assertEquals("0",save.quantities[0]);assertEquals("0 -> ?",save.truthTable);
        assertThrows(UnsupportedOperationException.class,()->snapshot.quantities().set(0,"1"));
    }
}
