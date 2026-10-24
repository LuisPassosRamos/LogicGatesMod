package sisso.logicsim.classroom;

import com.hypixel.hytale.server.core.event.events.player.PlayerReadyEvent;
import com.hypixel.hytale.server.core.event.events.player.PlayerDisconnectEvent;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.permissions.PermissionsModule;
import com.hypixel.hytale.server.core.Message;
import sisso.logicsim.classroom.domain.ClassroomViews.Role;
import static sisso.logicsim.util.LogicMessages.text;

/** Adapter for authenticated names, connection cleanup and a private message after the first ready event. */
public final class ClassroomPresence implements AutoCloseable {
    private final ClassroomRuntime runtime;
    private final ClassroomConnections connections=new ClassroomConnections();
    private final AutoCloseable subscription;
    public ClassroomPresence(ClassroomRuntime runtime) {
        this.runtime=runtime; subscription=runtime.listen(null,connections::changed);
    }
    public void ready(PlayerReadyEvent event) {
        var entity=event.getPlayerRef();
        if (entity==null || !entity.isValid()) return;
        PlayerRef player=entity.getStore().getComponent(entity,PlayerRef.getComponentType());
        if (player==null) return;
        var session=connections.ready(player.getUuid(),player.getPacketHandler());
        if (session==null) return;
        runtime.remember(player.getUuid(),player.getUsername()).thenCompose(ignored->runtime.home(player.getUuid()))
            .thenAccept(home->{
                if (home.classroom()==null || home.classroom().message().isEmpty()) return;
                session.classroom=home.classroom().id(); long revision=session.revision.get();
                var ref=player.getReference(); if (ref==null || !ref.isValid()) return;
                var store=ref.getStore(); var world=store.getExternalData().getWorld();
                try { world.execute(()->{
                    if (!connections.current(session,revision) || !ref.isValid() || !ref.equals(player.getReference())
                            || player.getPacketHandler()!=session.connection || ref.getStore()!=store) return;
                    if (home.member().role()==Role.TEACHER && !PermissionsModule.get().hasPermission(player.getUuid(),ClassroomRuntime.TEACH_PERMISSION,false)) return;
                    player.sendMessage(text("classroom.motdChat").param("classroom",Message.raw(home.classroom().name())).param("message",Message.raw(home.classroom().message())));
                }); } catch (RuntimeException stoppedWorld) { /* A later connection obtains a new session. */ }
            }).exceptionally(failure->null); // Storage already reports operational failures; no chat is sent on authorization failure.
    }
    public void disconnected(PlayerDisconnectEvent event) {
        PlayerRef player=event.getPlayerRef();
        if (connections.disconnect(player.getUuid(),player.getPacketHandler())) runtime.forget(player.getUuid());
    }
    @Override public void close() {
        connections.clear();
        try { subscription.close(); } catch (Exception ignored) { }
    }
}
