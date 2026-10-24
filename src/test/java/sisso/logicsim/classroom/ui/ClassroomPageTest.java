package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.codec.util.RawJsonReader;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import sisso.logicsim.classroom.domain.ClassroomException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletionException;
import static org.junit.jupiter.api.Assertions.*;

/** Checks wire bindings and localization, never claims to render a Hytale form. */
class ClassroomPageTest {
    @Test void packagedFormHasSaveFieldsButNoClientControlledClassroomIdentity() throws Exception {
        var page = new ClassroomPage(null, null, null);
        var commands = new UICommandBuilder();
        var events = new UIEventBuilder();
        page.build(null, commands, events, null);
        assertEquals(CustomUICommandType.Append, commands.getCommands()[0].type);
        assertEquals(ClassroomPage.LAYOUT, commands.getCommands()[0].text);
        try (var input = getClass().getResourceAsStream("/Common/UI/Custom/" + ClassroomPage.LAYOUT)) {
            assertNotNull(input);
            var layout = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            for (var binding : events.getEvents()) assertTrue(layout.contains(binding.selector + " {"));
        }
        var save = Arrays.stream(events.getEvents()).filter(event -> event.selector.equals("#Save")).findFirst().orElseThrow();
        var data = BsonDocument.parse(save.data);
        assertEquals(3, data.size());
        assertEquals("Save", data.getString("Action").getValue());
        assertEquals("#Name.Value", data.getString("@Name").getValue());
        assertEquals("#Description.Value", data.getString("@Description").getValue());
    }

    @Test void realJsonCodecAcceptsLiteralTextAndIgnoresForgedOwnerAndRevision() throws Exception {
        var json = "{\"Action\":\"Save\",\"@Name\":\"Turma 'A'\",\"@Description\":\"%server.fake\\n<b>literal</b>\",\"Owner\":\"forged\",\"Revision\":999}";
        var event = ClassroomPage.ActionEvent.CODEC.decodeJson(new RawJsonReader(json.toCharArray()), new ExtraInfo());
        assertEquals("Save", event.action);
        assertEquals("Turma 'A'", event.name);
        assertEquals("%server.fake\n<b>literal</b>", event.description);
        var empty = ClassroomPage.ActionEvent.CODEC.decodeJson(new RawJsonReader("{}".toCharArray()), new ExtraInfo());
        assertNull(empty.name);
        assertNull(empty.description);
    }

    @Test void applicationErrorsRemainLocalizedWithoutLeakingStorageDetails() {
        for (var reason : ClassroomException.Reason.values()) {
            var message = ClassroomPage.error(new CompletionException(new ClassroomException(reason)));
            assertTrue(message.getMessageId().startsWith("server.logicgates.classroom.error."));
            assertNull(message.getRawText());
        }
        var unexpected = ClassroomPage.error(new IllegalStateException("secret database path"));
        assertEquals("server.logicgates.classroom.error.unavailable", unexpected.getMessageId());
    }
}
