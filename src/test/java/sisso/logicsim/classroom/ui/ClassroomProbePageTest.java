package sisso.logicsim.classroom.ui;

import com.google.gson.JsonParser;
import com.hypixel.hytale.codec.ExtraInfo;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUICommandType;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import org.bson.BsonDocument;
import org.junit.jupiter.api.Test;
import sisso.logicsim.util.BuildInfo;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static sisso.logicsim.util.LogicMessages.text;

/** Real Hytale builders/codecs; captured transport does not prove client rendering. */
class ClassroomProbePageTest {
    @Test void initialPageHasPackagedLayoutMatchingBindingsAndTranslatedMessages() throws Exception {
        var page = new CapturedPage();
        var commands = build(page);
        var events = new UIEventBuilder();
        page.build(null, new UICommandBuilder(), events, null);
        var append = commands.getCommands()[0];
        assertEquals(CustomUICommandType.Append, append.type);
        assertEquals(ClassroomProbePage.LAYOUT, append.text);
        try (var stream = getClass().getResourceAsStream("/Common/UI/Custom/" + append.text)) {
            assertNotNull(stream, "Page must ship with the plugin resources");
            String layout = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            for (var binding : events.getEvents()) {
                assertTrue(layout.contains(binding.selector + " {"), "Missing event target " + binding.selector);
                assertEquals(CustomUIEventBindingType.Activating, binding.type);
                assertTrue(binding.locksInterface, "Updates acknowledge the event before another click");
                var data = BsonDocument.parse(binding.data);
                assertEquals(1, data.size(), "The client sends only an action, never counter state or identity");
                assertEquals(binding.selector.substring(1), data.getString("Action").getValue());
            }
            assertEquals(List.of("#Increment", "#Close"),
                    Arrays.stream(events.getEvents()).map(binding -> binding.selector).toList());
        }
        assertEquals(CustomPageLifetime.CanDismiss, page.getLifetime());
        assertCounter(commands, 0);
        assertMessage(commands, "#Build.TextSpans", text("build")
                .param("build", BuildInfo.BUILD_ID).param("hytale", BuildInfo.HYTALE_VERSION));
    }

    @Test void decodedClicksUpdateOnlyTheServerCounterWithoutRebuildingOrRebinding() {
        var page = new CapturedPage();
        page.handleDataEvent(null, null, "{\"Action\":\"Increment\",\"Count\":9999}");
        page.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        assertEquals(2, page.updates.size());
        assertCounter(page.updates.get(0), 1);
        assertCounter(page.updates.get(1), 2);
        for (var update : page.updates) assertEquals(1, update.getCommands().length);
        assertFalse(page.closed);
    }

    @Test void missingAndUnknownActionsAreAcknowledgedWithoutChangingTheCounter() {
        var page = new CapturedPage();
        for (String json : List.of("{}", "{\"Action\":\"CreateClassroom\"}")) {
            page.handleDataEvent(null, null, json);
        }
        assertEquals(2, page.updates.size());
        page.updates.forEach(update -> assertEquals(0, update.getCommands().length));
        page.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        assertCounter(page.updates.getLast(), 1);
        assertFalse(page.closed);
    }

    @Test void closingStopsLaterEventsAndReopeningStartsANewCounter() {
        var page = new CapturedPage();
        page.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        page.handleDataEvent(null, null, "{\"Action\":\"Close\"}");
        assertTrue(page.closed);
        page.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        assertEquals(1, page.updates.size());
        assertCounter(build(new CapturedPage()), 0);
    }

    @Test void dismissalStopsLaterEventsAndPageInstancesHaveIndependentCounters() {
        var first = new CapturedPage();
        var second = new CapturedPage();
        first.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        assertCounter(build(second), 0);
        first.onDismiss(null, null);
        first.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        first.handleDataEvent(null, null, "{\"Action\":\"Close\"}");
        assertEquals(1, first.updates.size());
        assertFalse(first.closed, "Dismissal must not issue another close command");
        second.handleDataEvent(null, null, "{\"Action\":\"Increment\"}");
        assertCounter(second.updates.getLast(), 1);
    }

    private UICommandBuilder build(ClassroomProbePage page) {
        var commands = new UICommandBuilder();
        page.build(null, commands, new UIEventBuilder(), null);
        return commands;
    }

    private void assertCounter(UICommandBuilder commands, long expected) {
        assertMessage(commands, "#Counter.TextSpans", text("classroom.probe.counter").param("count", expected));
    }

    private void assertMessage(UICommandBuilder commands, String selector, Message expected) {
        var command = Arrays.stream(commands.getCommands()).filter(value -> selector.equals(value.selector))
                .findFirst().orElseThrow();
        assertEquals(CustomUICommandType.Set, command.type);
        var payload = BsonDocument.parse(command.data).get("0");
        // The wire format is JSON, which does not preserve BSON int32/int64 tags.
        assertEquals(JsonParser.parseString(Message.CODEC.encode(expected, new ExtraInfo()).asDocument().toJson()),
                JsonParser.parseString(payload.asDocument().toJson()));
        assertEquals(expected.getMessageId(), Message.CODEC.decode(payload, new ExtraInfo()).getMessageId());
    }

    private static final class CapturedPage extends ClassroomProbePage {
        private final List<UICommandBuilder> updates = new ArrayList<>();
        private boolean closed;
        CapturedPage() { super(null); }
        @Override protected void sendUpdate(UICommandBuilder commands, boolean clear) {
            assertFalse(clear, "An event updates the existing document");
            updates.add(commands);
        }
        @Override protected void close() { closed = true; }
    }
}
