package sisso.logicsim.classroom.ui;

import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.server.core.ui.builder.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import sisso.logicsim.classroom.learning.*;
import static org.junit.jupiter.api.Assertions.*;

/** Transport/codec regression, not a substitute for testing the native client. */
class LearningPageTest {
    @Test void everyBoundControlExistsAndNoPartialAnchorOrClientAuthorityIsSent() throws Exception {
        var page=new LearningPage(null,null,null,null,null);var commands=new UICommandBuilder();var events=new UIEventBuilder();page.build(null,commands,events,null);
        try(var resource=getClass().getResourceAsStream("/Common/UI/Custom/"+LearningPage.LAYOUT)) {
            assertNotNull(resource);var layout=new String(resource.readAllBytes(),StandardCharsets.UTF_8);
            for(var binding:events.getEvents())assertTrue(layout.contains(binding.selector+" {"),binding.selector);
            assertTrue(layout.contains("PrefabPreviewComponent #PrefabPreview"));assertFalse(layout.contains("BuilderTool:"));
        }
        assertTrue(Arrays.stream(commands.getCommands()).noneMatch(c->c.selector!=null && c.selector.contains(".Anchor.")));
        var publish=Arrays.stream(events.getEvents()).filter(e->e.selector.equals("#Publish")).findFirst().orElseThrow();var fields=BsonDocument.parse(publish.data);
        assertEquals(Set.of("Action","@Title","@Description","@Deadline","@Value"),fields.keySet());
        var grade=Arrays.stream(events.getEvents()).filter(e->e.selector.equals("#SaveGrade")).findFirst().orElseThrow();assertEquals(Set.of("Action","@Score","@Feedback"),BsonDocument.parse(grade.data).keySet());
    }
    @Test void codecAcceptsLiteralFieldsButNoClaimedStudentPublicationOrGradeRevision() throws Exception {
        var event=LearningPage.Event.CODEC.decodeJson(new RawJsonReader("{\"Action\":\"Publish\",\"@Title\":\"%server.fake\",\"@Description\":\"literal\\ntext\",\"Publication\":999,\"Student\":\"forged\"}".toCharArray()),new ExtraInfo());
        assertEquals("%server.fake",event.title);assertEquals("literal\ntext",event.description);assertNull(event.target);
        for(var code:LearningException.Code.values())assertEquals("server.logicgates.learning.error."+code,LearningPage.error(new LearningException(code)).getMessageId());
    }
}
