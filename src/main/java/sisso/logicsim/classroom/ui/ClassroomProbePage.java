package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.util.BuildInfo;
import static sisso.logicsim.util.LogicMessages.text;

/** Increment 0 diagnostic: central flow confirmed in pt-BR; transient page state only. */
public class ClassroomProbePage extends InteractiveCustomUIPage<ClassroomProbePage.ActionEvent> {
    public static final String LAYOUT = "Pages/LogicGates/ClassroomProbe.ui";
    private long count;
    private boolean dismissed;

    public ClassroomProbePage(PlayerRef playerRef) {
        super(playerRef, CustomPageLifetime.CanDismiss, ActionEvent.CODEC);
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events,
                      Store<EntityStore> store) {
        commands.append(LAYOUT);
        ClassroomText.probe(commands);
        updateCounter(commands);
        commands.set("#Build.TextSpans", text("build").param("build", BuildInfo.BUILD_ID)
                .param("hytale", BuildInfo.HYTALE_VERSION));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Increment",
                EventData.of("Action", "Increment"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Close",
                EventData.of("Action", "Close"));
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ActionEvent event) {
        if (dismissed) return;
        if ("Close".equals(event.action)) {
            dismissed = true;
            close();
            return;
        }
        var commands = new UICommandBuilder();
        if ("Increment".equals(event.action)) {
            if (count < Long.MAX_VALUE) count++;
            updateCounter(commands);
        }
        // Even unknown/missing actions acknowledge the event and unlock the client UI.
        sendUpdate(commands, false);
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
        dismissed = true;
    }

    private void updateCounter(UICommandBuilder commands) {
        commands.set("#Counter.TextSpans", text("classroom.probe.counter").param("count", count));
    }

    public static final class ActionEvent {
        public static final BuilderCodec<ActionEvent> CODEC = BuilderCodec
                .builder(ActionEvent.class, ActionEvent::new)
                .append(new KeyedCodec<>("Action", Codec.STRING),
                        (event, value) -> event.action = value, event -> event.action).add().build();
        private String action;
    }
}
