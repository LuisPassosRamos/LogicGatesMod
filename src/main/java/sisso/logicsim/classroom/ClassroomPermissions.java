package sisso.logicsim.classroom;

import com.hypixel.hytale.server.core.event.events.permissions.PlayerPermissionChangeEvent;
import com.hypixel.hytale.server.core.event.events.permissions.GroupPermissionChangeEvent;

/** Native events are dispatched after the permission provider has changed. No database writes or polling. */
public final class ClassroomPermissions {
    private final ClassroomRuntime runtime;
    public ClassroomPermissions(ClassroomRuntime runtime) { this.runtime = runtime; }
    public void playerChanged(PlayerPermissionChangeEvent event) { runtime.authorizationChanged(event.getPlayerUuid()); }
    public void groupChanged(GroupPermissionChangeEvent event) { runtime.authorizationChangedForOpenPages(); }
}
