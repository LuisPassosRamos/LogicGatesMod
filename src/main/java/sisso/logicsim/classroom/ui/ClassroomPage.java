package sisso.logicsim.classroom.ui;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.protocol.packets.interface_.CustomPage;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import sisso.logicsim.classroom.ClassroomRuntime;
import sisso.logicsim.classroom.domain.Classroom;
import sisso.logicsim.classroom.domain.ClassroomException;
import sisso.logicsim.util.BuildInfo;
import java.util.concurrent.CompletionException;
import static sisso.logicsim.util.LogicMessages.text;

/** World-thread page state; database callbacks publish only to this exact active page. */
public class ClassroomPage extends InteractiveCustomUIPage<ClassroomPage.ActionEvent> {
    public static final String LAYOUT = "Pages/LogicGates/Classroom.ui";
    private final ClassroomRuntime runtime;
    private final World world;
    private Classroom current;
    private boolean busy = true;
    private boolean dismissed;

    public ClassroomPage(PlayerRef playerRef, ClassroomRuntime runtime, World world) {
        super(playerRef, CustomPageLifetime.CanDismiss, ActionEvent.CODEC);
        this.runtime = runtime;
        this.world = world;
    }

    @Override public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(LAYOUT);
        commands.set("#Build.TextSpans", text("build").param("build", BuildInfo.BUILD_ID).param("hytale", BuildInfo.HYTALE_VERSION));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Close", EventData.of("Action", "Close"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Reload", EventData.of("Action", "Reload"));
        events.addEventBinding(CustomUIEventBindingType.Activating, "#Save", EventData.of("Action", "Save")
                .append("@Name", "#Name.Value").append("@Description", "#Description.Value"));
    }

    public void load(Ref<EntityStore> ref, Store<EntityStore> store) {
        busy = true;
        runtime.own(playerRef.getUuid()).whenComplete((value, failure) -> world.execute(() -> {
            if (!active(ref, store)) return;
            busy = false;
            var commands = new UICommandBuilder();
            if (failure != null) showFailure(commands, failure);
            else {
                current = value.orElse(null);
                showClassroom(commands);
                commands.set("#Status.TextSpans", current == null ? text("classroom.createHint") : text("classroom.editHint"));
            }
            publish(ref, store, commands);
        }));
    }

    @Override public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, ActionEvent event) {
        if (!active(ref, store)) return;
        if ("Close".equals(event.action)) {
            dismissed = true;
            close();
            return;
        }
        var commands = new UICommandBuilder();
        if (busy) { publish(ref, store, commands); return; }
        if ("Reload".equals(event.action)) {
            commands.set("#Status.TextSpans", text("classroom.loading"));
            commands.set("#Save.Disabled", true);
            publish(ref, store, commands);
            load(ref, store);
        } else if ("Save".equals(event.action)) {
            busy = true;
            commands.set("#Save.Disabled", true);
            commands.set("#Status.TextSpans", text("classroom.saving"));
            publish(ref, store, commands);
            // Actor, classroom ID and revision come from this server session, never the UI payload.
            var operation = current == null ? runtime.create(playerRef.getUuid(), event.name, event.description)
                    : runtime.update(playerRef.getUuid(), current.id(), current.revision(), event.name, event.description);
            operation.whenComplete((value, failure) -> world.execute(() -> {
                if (!active(ref, store)) return;
                busy = false;
                var update = new UICommandBuilder();
                update.set("#Save.Disabled", false);
                if (failure != null) showFailure(update, failure);
                else {
                    current = value;
                    showClassroom(update);
                    update.set("#Status.TextSpans", text("classroom.saved"));
                }
                publish(ref, store, update);
            }));
        } else publish(ref, store, commands);
    }

    private void showClassroom(UICommandBuilder commands) {
        commands.set("#Form.Visible", true);
        commands.set("#NoAccess.Visible", false);
        commands.set("#Members.Visible", current != null);
        commands.set("#Save.Disabled", false);
        commands.set("#Teacher.TextSpans", Message.raw(playerRef.getUsername()));
        commands.set("#ClassName.TextSpans", current == null ? text("classroom.newClass") : Message.raw(current.name()));
        commands.set("#Name.Value", current == null ? "" : current.name());
        commands.set("#Description.Value", current == null ? "" : current.description());
        commands.set("#Save.Text", current == null ? text("classroom.create") : text("classroom.save"));
    }

    private void showFailure(UICommandBuilder commands, Throwable failure) {
        commands.set("#Status.TextSpans", error(failure));
        Throwable cause = unwrap(failure);
        if (cause instanceof ClassroomException error && error.reason() == ClassroomException.Reason.FORBIDDEN) {
            current = null;
            commands.set("#Form.Visible", false);
            commands.set("#Members.Visible", false);
            commands.set("#NoAccess.Visible", true);
            commands.set("#ClassName.TextSpans", text("classroom.title"));
            commands.set("#Teacher.TextSpans", Message.raw(""));
        }
    }

    static Message error(Throwable failure) {
        Throwable cause = unwrap(failure);
        if (!(cause instanceof ClassroomException error)) return text("classroom.error.unavailable");
        return switch (error.reason()) {
            case FORBIDDEN -> text("classroom.error.forbidden");
            case ALREADY_MEMBER -> text("classroom.error.alreadyMember");
            case INVALID_NAME -> text("classroom.error.name");
            case INVALID_DESCRIPTION -> text("classroom.error.description");
            case STALE -> text("classroom.error.stale");
            case UNAVAILABLE -> text("classroom.error.unavailable");
        };
    }

    private static Throwable unwrap(Throwable failure) {
        while (failure instanceof CompletionException && failure.getCause() != null) failure = failure.getCause();
        return failure;
    }

    private boolean active(Ref<EntityStore> ref, Store<EntityStore> store) {
        if (dismissed || !ref.isValid() || !ref.equals(playerRef.getReference()) || ref.getStore() != store
                || store.getExternalData().getWorld() != world) return false;
        var player = store.getComponent(ref, Player.getComponentType());
        return player != null && player.getPageManager().getCustomPage() == this;
    }

    private void publish(Ref<EntityStore> ref, Store<EntityStore> store, UICommandBuilder commands) {
        if (!active(ref, store)) return;
        // Already on the world thread: send directly after the instance check, without another queued hop.
        var player = store.getComponent(ref, Player.getComponentType());
        player.getPageManager().updateLegacyCustomPage(new CustomPage(getClass().getName(), false, false,
                getLifetime(), commands.getCommands(), UIEventBuilder.EMPTY_EVENT_BINDING_ARRAY));
    }

    @Override public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) { dismissed = true; }

    public static final class ActionEvent {
        public static final BuilderCodec<ActionEvent> CODEC = BuilderCodec.builder(ActionEvent.class, ActionEvent::new)
                .append(new KeyedCodec<>("Action", Codec.STRING), (event, value) -> event.action = value, event -> event.action).add()
                .append(new KeyedCodec<>("@Name", Codec.STRING), (event, value) -> event.name = value, event -> event.name).add()
                .append(new KeyedCodec<>("@Description", Codec.STRING), (event, value) -> event.description = value, event -> event.description).add()
                .build();
        String action;
        String name;
        String description;
    }
}
