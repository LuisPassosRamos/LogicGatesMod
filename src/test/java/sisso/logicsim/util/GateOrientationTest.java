package sisso.logicsim.util;

import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import org.junit.jupiter.api.Test;
import sisso.logicsim.circuit.HorizontalDirection;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

class GateOrientationTest {
    @Test void allFourUprightYawsExposeDistinctHorizontalOutputs() {
        var outputs = EnumSet.noneOf(HorizontalDirection.class);
        for (var yaw : Rotation.NORMAL) {
            var output = GateOrientation.horizontalOutput(RotationTuple.of(yaw, Rotation.None, Rotation.None));
            assertNotNull(output, "Upright yaw must remain supported: " + yaw);
            assertTrue(outputs.add(output), "Each yaw must expose a different output: " + yaw);
        }
        assertEquals(EnumSet.allOf(HorizontalDirection.class), outputs);
        assertEquals(HorizontalDirection.EAST,
                GateOrientation.horizontalOutput(RotationTuple.of(Rotation.None, Rotation.None, Rotation.None)),
                "The unrotated artwork outputs along local +X");
    }

    @Test void pitchedRolledAndOverturnedGatesAreUnsupported() {
        for (var yaw : Rotation.NORMAL) {
            for (var tilt : Rotation.NORMAL) {
                if (tilt == Rotation.None) continue;
                assertNull(GateOrientation.horizontalOutput(RotationTuple.of(yaw, tilt, Rotation.None)),
                        "A pitched gate must not acquire a horizontal fallback: " + yaw + ", " + tilt);
                assertNull(GateOrientation.horizontalOutput(RotationTuple.of(yaw, Rotation.None, tilt)),
                        "A rolled gate must not acquire a horizontal fallback: " + yaw + ", " + tilt);
            }
        }
    }
}
