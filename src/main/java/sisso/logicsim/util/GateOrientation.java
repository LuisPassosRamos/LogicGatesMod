package sisso.logicsim.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import org.joml.Vector3i;
import sisso.logicsim.circuit.HorizontalDirection;

/** Shared gate-art orientation policy for live circuits and saved submissions. */
public final class GateOrientation {
    private GateOrientation() { }

    /** The artwork outputs along local +X. Returns null for tilted or overturned gates. */
    public static HorizontalDirection horizontalOutput(RotationTuple rotation) {
        var up = new Vector3i(0, 1, 0);
        rotation.applyRotationTo(up);
        if (up.x != 0 || up.y != 1 || up.z != 0) return null;

        var front = new Vector3i(1, 0, 0);
        rotation.applyRotationTo(front);
        for (var direction : HorizontalDirection.values()) {
            if (front.x == direction.dx() && front.y == 0 && front.z == direction.dz()) return direction;
        }
        return null;
    }
}
